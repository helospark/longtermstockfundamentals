package com.helospark.financialdata.management.screener.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface ScreenerElement {
    String id() default "";

    String name();

    boolean separatorBelow() default false;

    AtGlanceFormat format() default AtGlanceFormat.SIMPLE_NUMBER;
}
