import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by equi on 03.05.16.
 *
 * @author Kravchenko Dima
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Entity {
    /**
     * This annotation before class declaration specifies that
     * this class will be used in DAO.
     *
     * You can specify your own table name or, if you did not,
     * just name of a class will be used.
     *
     * @return name of table or empty line if none specified.
     */
    String tableName() default ""; // TODO do I really need this? If no, delete corresponding code from `register`
}
