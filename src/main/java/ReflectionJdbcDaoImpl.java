import java.io.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.google.common.base.CaseFormat.*;

/**
 * Created by equi on 03.05.16.
 *
 * @author Kravchenko Dima
 */

/**
 * @param <T> specifies which type of objects will be stored
 */
public class ReflectionJdbcDaoImpl<T> implements ReflectionJdbcDao<T> {

    private static final int STRING_MAX_LENGTH = 255; // VARCHAR boundary
    private static final String OBJECT_ITSELF_COLUMN = "object_itself";

    private static final Set<Class<?>> SUPPORTED_INDEX_TYPES = new HashSet<>(Arrays.asList(new Class<?>[] {
            int.class,
            Integer.class,
            long.class,
            Long.class,
            String.class
    }));

    private Connection connection;
    private Class<T> clazz;

    private String tableName;

    private Field[] allFields;
    private List<Field> indexedFields;

    private String createQueryTemplate;
    private String insertQueryTemplate;
    private String updateQueryTemplate;
    private String deleteQueryTemplate;
    private String selectQueryTemplate;
    private String selectAllQueryTemplate;

    /**
     * User must specify which connection to use.
     *
     * @param connection the connection which user wants to use
     */
    public ReflectionJdbcDaoImpl(Connection connection) {
        this.connection = connection;
        indexedFields = new ArrayList<>();
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException
     */
    public void register(Class<T> classToRegister) throws IllegalArgumentException, SQLException {
        clazz = classToRegister;
        checkClass();
        setUpTableName();
        allFields = clazz.getDeclaredFields();
        getIndexedFields();
        formQueryTemplates();
        createTable();
    }

    /**
     * {@inheritDoc}
     */
    public void insert(T object) throws SQLException, IllegalAccessException {
        try (PreparedStatement pStatement = connection.prepareStatement(insertQueryTemplate)) {
            insertKeyValues(pStatement, object, 1);
            pStatement.setObject(1 + indexedFields.size(), serializeObject((Serializable) object));

            pStatement.execute();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void update(T object) throws SQLException, IllegalAccessException {
        try (PreparedStatement pStatement = connection.prepareStatement(updateQueryTemplate)) {
            pStatement.setObject(1, serializeObject((Serializable) object));
            insertKeyValues(pStatement, object, 2);

            pStatement.execute();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteByKey(T key) throws SQLException, IllegalAccessException {
        try (PreparedStatement pStatement = connection.prepareStatement(deleteQueryTemplate)) {
            insertKeyValues(pStatement, key, 1);

            pStatement.execute();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public T selectByKey(T key) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException {
        try (PreparedStatement pStatement = connection.prepareStatement(selectQueryTemplate)) {
            insertKeyValues(pStatement, key, 1);
            ResultSet resultSet = pStatement.executeQuery();

            if (resultSet.next()) {
                ByteArrayInputStream bais = new ByteArrayInputStream(resultSet.getBytes(1));
                ObjectInputStream ois = new ObjectInputStream(bais);
                return (T) ois.readObject();
            } else {
                return null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public List<T> selectAll() throws IOException, SQLException, ClassNotFoundException {
        try (PreparedStatement pStatement = connection.prepareStatement(selectAllQueryTemplate)) {
            ResultSet resultSet = pStatement.executeQuery();

            List<T> result = new ArrayList<>();

            while (resultSet.next()) {
                ByteArrayInputStream bais = new ByteArrayInputStream(resultSet.getBytes(1));
                ObjectInputStream ois = new ObjectInputStream(bais);
                result.add((T) ois.readObject());
            }

            return result;
        }
    }

    private void insertKeyValues(PreparedStatement pStatement, T key, int from) throws IllegalAccessException,
            SQLException{
        int i = from;
        for (Field field : indexedFields) {
            pStatement.setObject(i++, field.get(key));
        }
    }

    private void checkClass() throws IllegalArgumentException {
        Entity entity = clazz.getAnnotation(Entity.class);
        if (entity == null) {
            throw new IllegalArgumentException("class '" + clazz.getName() + "' does not " +
                    "have @Entity annotation");
        }

        if (!Serializable.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException("class '" + clazz.getName() + "' does not " +
                    "implement java.io.Serializable");
        }
    }

    private void setUpTableName() {
        Entity entity = clazz.getAnnotation(Entity.class);
        if (!entity.tableName().equals("")) {
            tableName = entity.tableName();
        } else {
            tableName = UPPER_CAMEL.to(LOWER_UNDERSCORE, clazz.getName());
        }
    }

    /**
     * searching for all fields annotated with @Index.
     * If none of the fields annotated IllegalArgumentException will be thrown.
     *
     * @throws IllegalArgumentException
     */
    private void getIndexedFields() throws IllegalArgumentException {
        for (Field field : allFields) {
            if (field.getAnnotation(Index.class) != null) {
                checkField(field);
                indexedFields.add(field);
            }
        }

        if (indexedFields.size() == 0) {
            throw new IllegalArgumentException("Given class " + clazz.getName() + " does not have any @Index");
        }
    }

    private void checkField(Field field) throws IllegalArgumentException {
        if (!SUPPORTED_INDEX_TYPES.contains(field.getType())) {
            throw new IllegalArgumentException("field " + field.getName() + " has has type " +
                    field.getType() + " which is not supported as @Index");
        }
    }

    private void createTable() throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, tableName.toUpperCase(),
                new String[] {"TABLE"})) {
            if (!tables.next()) {
                try (PreparedStatement pStatement = connection.prepareStatement(createQueryTemplate)) {
                    pStatement.execute();
                }
            }
        }
    }

    private void formQueryTemplates() {
        formCreateQueryTemplate();
        formInsertQueryTemplate();
        formUpdateQueryTemplate();
        formDeleteQueryTemplate();
        formSelectQueryTemplate();
        formSelectAllQueryTemplate();
    }

    /**
     * It will form {@link ReflectionJdbcDaoImpl#createQueryTemplate} like this:
     *
     * CREATE TABLE {@link ReflectionJdbcDaoImpl#tableName} (
     *     first_indexed_field TypeOfFirstField,
     *     second_indexed_field TypeOfSecondField,
     *     ...
     *     last_indexed_field TypeOfLastField,
     *     object_itself BLOB
     * )
     *
     * Where object_itself will be serialized object which user asked us to store.
     */
    private void formCreateQueryTemplate() {
        StringBuilder queryBuilder = new StringBuilder("CREATE TABLE " + tableName + " (\n");
        String tableColumn;

        for (Field field : indexedFields) {
            tableColumn = LOWER_CAMEL.to(LOWER_UNDERSCORE, field.getName()) + " " +
                    getFieldSQLType(field) + ",\n";
            queryBuilder.append(tableColumn);
        }

        createQueryTemplate = queryBuilder.toString() + OBJECT_ITSELF_COLUMN + " BLOB\n)";
    }

    /**
     * Converting java type to MySQL type;
     * java                   mysql
     *
     * int                    INTEGER
     * Integer                INTEGER
     * long                   BIGINT
     * Long                   BIGINT
     * String                 VARCHAR
     *
     * @param field field which type we need to convert
     * @return mysql type
     */
    private String getFieldSQLType(Field field) {
        if (field.getType() == int.class ||
                field.getType() == Integer.class) {
            return "INTEGER";
        }
        if (field.getType() == long.class ||
                field.getType() == Long.class) {
            return "BIGINT";
        }
        return "VARCHAR(" + STRING_MAX_LENGTH + ")";
    }

    /**
     * I will form {@link ReflectionJdbcDaoImpl#insertQueryTemplate} like this:
     *
     * INSERT INTO {@link ReflectionJdbcDaoImpl#tableName}
     * VALUES (?, ?, ..., ?)
     *
     * Where amount of '?' is equal to size of {@link ReflectionJdbcDaoImpl#indexedFields} plus one.
     * (this one additional is for `object_itself` column)
     */
    private void formInsertQueryTemplate() {
        StringBuilder queryBuilder = new StringBuilder("INSERT INTO " + tableName + "\nVALUES (");

        for (int i = 0; i < indexedFields.size(); i++) {
            queryBuilder.append("?, ");
        }

        insertQueryTemplate = queryBuilder.toString() + "?)";
    }

    /**
     * This method will form {@link ReflectionJdbcDaoImpl#updateQueryTemplate} like this:
     *
     * UPDATE {@link ReflectionJdbcDaoImpl#tableName}
     * SET {@link ReflectionJdbcDaoImpl#OBJECT_ITSELF_COLUMN}=?
     * WHERE first_indexed_field=? AND second_indexed_field=? AND ... AND last_indexed_field=?
     */
    private void formUpdateQueryTemplate() {
        updateQueryTemplate = "UPDATE " + tableName + "\n" +
                "SET " + OBJECT_ITSELF_COLUMN + "=?\n" +
                makeWhereStatement();
    }

    /**
     * This method will form {@link ReflectionJdbcDaoImpl#deleteQueryTemplate} like this:
     *
     * DELETE FROM {@link ReflectionJdbcDaoImpl#tableName}
     * WHERE first_indexed_field=? AND second_indexed_field=? AND ... AND last_indexed_field=?
     */
    private void formDeleteQueryTemplate() {
        deleteQueryTemplate = "DELETE FROM " + tableName + "\n" +
                makeWhereStatement();
    }

    /**
     * This method will form {@link ReflectionJdbcDaoImpl#selectQueryTemplate} like this:
     *
     * SELECT {@link ReflectionJdbcDaoImpl#OBJECT_ITSELF_COLUMN} FROM {@link ReflectionJdbcDaoImpl#tableName}
     * WHERE first_indexed_field=? AND ... AND last_indexed_field=?
     */
    private void formSelectQueryTemplate() {
        selectQueryTemplate = "SELECT " + OBJECT_ITSELF_COLUMN + " FROM " + tableName + "\n" +
                makeWhereStatement();
    }

    /**
     * This method will form {@link ReflectionJdbcDaoImpl#selectAllQueryTemplate} like this:
     *
     * SELECT {@link ReflectionJdbcDaoImpl#OBJECT_ITSELF_COLUMN} FROM {@link ReflectionJdbcDaoImpl#tableName}
     */
    private void formSelectAllQueryTemplate() {
        selectAllQueryTemplate = "SELECT " + OBJECT_ITSELF_COLUMN + " FROM " + tableName;
    }

    private String makeWhereStatement() {
        StringBuilder res = new StringBuilder("WHERE ");
        boolean needComma = false;

        for (Field field : indexedFields) {
            String condition = LOWER_CAMEL.to(LOWER_UNDERSCORE, field.getName()) + "=?";
            if (needComma) {
                res.append(" AND ");
            }
            res.append(condition);
            needComma = true;
        }

        return res.toString();
    }

    private byte[] serializeObject(Serializable object) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeObject(object);
            objOut.close();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            // I'm pretty sure that with our checks this can never happen
            return null;
        }
    }

}