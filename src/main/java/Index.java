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
@Target({ElementType.FIELD})
public @interface Index {
    /**
     * Tells that this field will be a key field in database.
     * The name of database column is the name of field converted
     * to lower case (e.g. userId -> user_id)
     *
     * Field type should be one of {int, long, String}.
     * String should have length <= {@link ReflectionJdbcDaoImpl#STRING_MAX_LENGTH}
     */
}
