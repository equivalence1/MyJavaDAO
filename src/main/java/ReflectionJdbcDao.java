import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by equi on 03.05.16.
 *
 * @author Kravchenko Dima
 */
public interface ReflectionJdbcDao<T> {

    /**
     * Before performing any queries to database you need to create table
     * for your class and make templates for queries.
     * To do this just use register this class through this method.
     *
     * @param clazz is the class you want to register in database
     */
    void register(Class<T> clazz) throws SQLException;

    /**
     * Insert object to database
     *
     * @param object is the object which fields will be inserted in database.
     */
    void insert(T object) throws SQLException, IllegalAccessException;

    /**
     * Update corresponding record in database
     *
     * @param object is the object which fields will be updated in database.
     */
    void update(T object) throws SQLException, IllegalAccessException;

    /**
     * Delete object from database.
     *
     * @param key identifies the object which will be deleted.
     */
    void deleteByKey(T key) throws SQLException, IllegalAccessException;

    /**
     * Retrieving object from database.
     *
     * @param key identifies the object to retrieve.
     * @return retrieved object.
     */
    T selectByKey(T key) throws SQLException, IllegalAccessException, IOException, ClassNotFoundException;

    /**
     * Retrieving all objects from database.
     *
     * @return all objects from database.
     */
    List<T> selectAll() throws SQLException, IOException, ClassNotFoundException;

}
