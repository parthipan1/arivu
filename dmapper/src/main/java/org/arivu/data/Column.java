package org.arivu.data;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * @author P
 *
 */
@Target({METHOD, FIELD}) 
@Retention(RUNTIME)
public @interface Column {

    /**
     * @return name
     */
    String name() default "";

    /**
     * @return unique
     */
    boolean unique() default false;

    /**
     * @return nullable
     */
    boolean nullable() default true;

    /**
     * @return insertable
     */
    boolean insertable() default true;

    /**
     * @return updatable
     */
    boolean updatable() default true;

    /**
     * @return columnDefinition
     */
    String columnDefinition() default "";

    /**
     * @return table
     */
    String table() default "";

    /**
     * @return length
     */
    int length() default 255;

    /**
     * @return precision
     */
    int precision() default 0;

    /**
     * @return scale
     */
    int scale() default 0;
}
