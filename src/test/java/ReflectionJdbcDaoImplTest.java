import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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

    @After
    public void trancateTables() throws Exception {
        //PreparedStatement
    }

    @Test
    public void testRegister() throws Exception {
        ReflectionJdbcDao<TestObject> dao = new ReflectionJdbcDaoImpl<>(connection);
        dao.register(TestObject.class);

        ResultSet tables = connection.getMetaData().getTables(null, null, "TEST_OBJECT",
                new String[] {"TABLE"});

        assertEquals(true, tables.next());
    }

    @Test
    public void testInsert() throws Exception {
        ReflectionJdbcDao<TestObject> dao = new ReflectionJdbcDaoImpl<>(connection);
        dao.register(TestObject.class);

        TestObject obj1 = new TestObject("Vasya", "Pupkin", "USSR", 12);
        TestObject obj2 = new TestObject("Ivan", "Ivanov", "Russia", 20);
        TestObject obj3 = new TestObject("John", "Smith", "UK", 436);

        dao.insert(obj1);
        dao.insert(obj2);
        dao.insert(obj3);
        dao.insert(obj1);

        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM test_object");
        ResultSet rs = preparedStatement.executeQuery();

        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
        }

        assertEquals(4, rowCount);
    }

    @Test
    public void testUpdate() throws Exception {

    }

    @Test
    public void testDeleteByKey() throws Exception {

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

}