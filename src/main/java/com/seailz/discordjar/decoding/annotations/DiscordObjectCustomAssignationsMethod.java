package com.seailz.discordjar.decoding.annotations;

import com.seailz.discordjar.decoding.DiscordObjectParser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a method with this to define custom decompilation methods for certain values while using
 * {@link DiscordObjectParser DiscordObjectParser}.
 * <p>The method must be static and should take in a {@link org.json.JSONObject} and return a {@link java.util.HashMap HashMap<String, Object>} of JSON
 * keys to the values.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DiscordObjectCustomAssignationsMethod {
}
