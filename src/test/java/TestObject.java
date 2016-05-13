import java.io.Serializable;

/**
 * Created by equi on 03.05.16.
 *
 * @author Kravchenko Dima
 */

@Entity
public class TestObject implements Serializable {
    @Index public String name;
    @Index public String surname;

    public String country;
    public int age;

    public TestObject() {}

    public TestObject(String name, String surname, String country, int age) {
        this.name = name;
        this.surname = surname;
        this.country = country;
        this.age = age;
    }
}
