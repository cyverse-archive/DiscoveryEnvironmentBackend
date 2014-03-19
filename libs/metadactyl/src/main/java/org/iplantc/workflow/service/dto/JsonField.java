package org.iplantc.workflow.service.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A simple annotation that can be used to indicate the name of a JSON field in a DTO.
 * 
 * @author Dennis Roberts
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JsonField {
    String name();
    boolean optional() default false;
    String defaultValue() default NULL;
    public static final String NULL = "HACK TO ALLOW A \"NULL\" DEFAULT VALUE";
}
