package common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonElement {
    /*
    methods must have no parameters, and cannot throw an exception.
    Also, the return types are restricted to primitives, String, Class, enums, annotations, and arrays of these types,
    and the default value cannot be null
    * */
    public String key() default "";
}
