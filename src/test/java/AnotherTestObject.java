/**
 * Created by equi on 03.05.16.
 *
 * @author Kravchenko Dima
 */

@Entity(tableName = "some_table")
public class AnotherTestObject {
    @Index public String name;
    @Index public int intUnboxedField;
    @Index public Integer intBoxedField;
    @Index public long longUnboxedField;
    @Index public Long longBoxedField;

    public String innerInfo;

    public AnotherTestObject(String name, int field1, Integer field2, long field3, Long field4) {
        this.name = name;
        this.intUnboxedField = field1;
        this.intBoxedField = field2;
        this.longUnboxedField = field3;
        this.longBoxedField = field4;
    }
}