package com.github.rostmyr.jrpc.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Rostyslav Myroshnychenko
 * on 26.05.2018.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
public @interface ResourceId {

    /**
     * Resource id. It should be unique across all resources
     */
    int value();
}
