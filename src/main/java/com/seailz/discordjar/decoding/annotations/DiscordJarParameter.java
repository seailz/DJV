package com.seailz.discordjar.decoding.annotations;

import com.seailz.discordjar.decoding.annotations.DiscordObjectParameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If used on a parameter, this won't be treated like a normal object parameter and instead will always
 * have its value set to the current DiscordJar instance.
 * <br><br>
 * <b>This must be used in conjunction with {@link DiscordObjectParameter} and cannot be used on it's own.</b>
 * {@link DiscordObjectParameter}
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DiscordJarParameter {
}
