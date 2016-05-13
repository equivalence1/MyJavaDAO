/**
 * Created by equi on 03.05.16.
 *
 * @author Kravchenko Dima
 */

@Entity(tableName = "some_table")
public class AnotherTestObject {
    @Index public String name;
    @Index public String surname;

    public String country;
    public int age;

    public AnotherTestObject(String name, String surname, String country, int age) {
        this.name = name;
        this.surname = surname;
        this.country = country;
        this.age = age;
    }
}