package com.Reffr_Backend.common.security.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    long ttlInSeconds() default 3600; // Default 1 hour
}
