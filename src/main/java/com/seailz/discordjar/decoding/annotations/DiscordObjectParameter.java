package com.seailz.discordjar.decoding.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DiscordObjectParameter {

    boolean nullable() default true;

    /**
     * Is there a possibility for this parameter to be excluded from an object?
     */
    boolean excludable() default true;

    /**
     * Allows you to override the default JSON key of a parameter.
     * <br>If this is not set, a key is assumed by setting the parameter name to full lowercase.
     */
    String overrideKey() default "";

}
