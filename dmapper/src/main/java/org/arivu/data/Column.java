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
     * @return
     */
    String name() default "";

    /**
     * @return
     */
    boolean unique() default false;

    /**
     * @return
     */
    boolean nullable() default true;

    /**
     * @return
     */
    boolean insertable() default true;

    /**
     * @return
     */
    boolean updatable() default true;

    /**
     * @return
     */
    String columnDefinition() default "";

    /**
     * @return
     */
    String table() default "";

    /**
     * @return
     */
    int length() default 255;

    /**
     * @return
     */
    int precision() default 0;

    /**
     * @return
     */
    int scale() default 0;
}
