package com.seailz.discordjar.decoding.data;

import com.seailz.discordjar.decoding.DiscordObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Contains information about a {@link DiscordObject}. This class is generally used for caching purposes,
 * as reflection can take some time to complete and doing that every time you want to use a Discord object would
 * be too resource intensive.
 */
public record DiscordObjectInformation(List<DiscordObjectParameterInformation> parameterList, Method customAssignations, Constructor<DiscordObject> constructor) {

}
