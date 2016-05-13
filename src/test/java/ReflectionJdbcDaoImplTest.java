import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by equi on 03.05.16.
 *
 * @author Kravchenko Dima
 */
public class ReflectionJdbcDaoImplTest {

    private static final String DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";
    private static final String JDBC_URL = "jdbc:derby:testdb;create=true";

    private static Connection connection;

    /**
     * I delete previous version of testdb for a new test.
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        Class.forName(DRIVER); // no need since java 7. I do it just for sure
        cleanUp();
        connection = DriverManager.getConnection(JDBC_URL);
    }

    @Test
    public void testRegister() throws Exception {
        ReflectionJdbcDao<TestObject> dao = new ReflectionJdbcDaoImpl<>(connection);
        dao.register(TestObject.class);

        try (ResultSet tables = connection.getMetaData().getTables(null, null, "TEST_OBJECT",
                new String[] {"TABLE"})) {

            assertEquals(true, tables.next());
        }
    }

    @Test
    public void testInsert() throws Exception {
        ReflectionJdbcDao<TestObject> dao = new ReflectionJdbcDaoImpl<>(connection);
        dao.register(TestObject.class);

        truncateTable("test_object");

        TestObject obj1 = new TestObject("Vasya", "Pupkin", "USSR", 12);
        TestObject obj2 = new TestObject("Ivan", "Ivanov", "Russia", 20);
        TestObject obj3 = new TestObject("John", "Smith", "UK", 436);

        dao.insert(obj1);
        dao.insert(obj2);
        dao.insert(obj3);
        dao.insert(obj1);

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM test_object")) {
            ResultSet rs = preparedStatement.executeQuery();

            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
            }

            rs.close();

            assertEquals(4, rowCount);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        ReflectionJdbcDao<AnotherTestObject> dao = new ReflectionJdbcDaoImpl<>(connection);
        dao.register(AnotherTestObject.class);

        String infoAfterUpdate = "Info after update";

        AnotherTestObject obj1 = new AnotherTestObject("Name", 1, 2, 3L, 4L);
        AnotherTestObject key = new AnotherTestObject("Name", 1, 2, 3L, 4L);
        obj1.innerInfo = "Info before update";

        dao.insert(obj1);

        obj1.innerInfo = infoAfterUpdate;

        dao.update(obj1);

        assertEquals(infoAfterUpdate, dao.selectByKey(key).innerInfo);
    }

    @Test
    public void testDeleteByKey() throws Exception {
        ReflectionJdbcDao<TestObject> dao = new ReflectionJdbcDaoImpl<>(connection);
        dao.register(TestObject.class);

        TestObject obj = new TestObject("SomeName", "SomeSurname", "SomeCountry", 100500);
        TestObject key = new TestObject("SomeName", "SomeSurname", "", 0);

        dao.insert(obj);
        assertNotNull(dao.selectByKey(key));

        dao.deleteByKey(key);
        assertNull(dao.selectByKey(key));
    }

    @Test
    public void testSelectByKey() throws Exception {
        ReflectionJdbcDao<TestObject> dao = new ReflectionJdbcDaoImpl<>(connection);
        dao.register(TestObject.class);

        TestObject vasya = new TestObject("Vasya", "Pupkin", "USSR", 12);
        TestObject vanya = new TestObject("Vanya", "Pupkin", "USSR", 22);

        dao.insert(vanya);
        dao.insert(vasya);
        dao.insert(vanya);

        TestObject key = new TestObject();
        key.name = "Vasya";
        key.surname = "Pupkin";

        TestObject obj = dao.selectByKey(key);

        assertEquals(vasya.name,    obj.name);
        assertEquals(vasya.surname, obj.surname);
        assertEquals(vasya.country, obj.country);
        assertEquals(vasya.age,     obj.age);
    }

    @Test
    public void testSelectAll() throws Exception {
        ReflectionJdbcDao<TestObject> dao = new ReflectionJdbcDaoImpl<>(connection);
        dao.register(TestObject.class);

        truncateTable("test_object");

        final int n = 100;
        boolean was[] = new boolean[n];

        for (int i = 0; i < n; i++) {
            was[i] = false;
            dao.insert(new TestObject("" + i, "" + i, "" + i, i));
        }

        List<TestObject> fromDB = dao.selectAll();

        for (TestObject obj : fromDB) {
            assertTrue(obj.age < n);
            assertEquals("" + obj.age, obj.name);
            assertEquals("" + obj.age, obj.surname);
            assertEquals("" + obj.age, obj.country);
            was[obj.age] = true;
        }

        for (int i = 0; i < n; i++) {
            assertEquals(true, was[i]);
        }
    }

    /**
     * this method deletes testdb folder and derby.log file.
     */
    private static void cleanUp() throws IOException {
        delete(new File("testdb"));
        delete(new File("derby.log"));
    }

    private static void delete(File file) throws IOException {
        if(file.isDirectory()){
            if(file.list().length==0){
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            } else {
                String files[] = file.list();

                for (String temp : files) {
                    File fileDelete = new File(file, temp);
                    delete(fileDelete);
                }

                if(file.list().length==0){
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        } else {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    /**
     * Use this method if your test need clean table
     * (i.e. if you need to check sizes of this table)
     * to make sure that nothing left from previous tests.
     *
     * @param tableName which table to truncate
     * @throws SQLException
     */
    private static void truncateTable(String tableName) throws SQLException {
        try (PreparedStatement pStatement = connection.prepareStatement("TRUNCATE TABLE " + tableName)) {
            pStatement.execute();
        }
    }

}