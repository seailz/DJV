package com.seailz.discordjar.decoding;

import com.seailz.discordjar.decoding.annotations.DiscordJarParameter;
import com.seailz.discordjar.decoding.annotations.DiscordObjectConstructor;
import com.seailz.discordjar.decoding.annotations.DiscordObjectParameter;

/**
 * Marks an object that can be decoded by {@link DiscordObjectParser}.
 * <p>Each one of your fields must be marked with {@link DiscordObjectParameter DiscordObjectParameter},
 * and can be marked with {@link DiscordJarParameter DiscordJarParameter} in order to have
 * its value set default to the existing DiscordJar instance.
 * <p>
 * You must have a constructor that matches your fields, <b>even down to the order</b>. That constructor
 * must be marked with {@link DiscordObjectConstructor DiscordObjectConstructor}.
 */
public interface DiscordObject {
}
