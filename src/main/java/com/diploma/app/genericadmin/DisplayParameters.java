package com.diploma.app.genericadmin;

import java.lang.annotation.*;

@Target(value = ElementType.FIELD)
@Retention(value= RetentionPolicy.RUNTIME)
public @interface DisplayParameters {
    String name();
    int order() default 0;
}

