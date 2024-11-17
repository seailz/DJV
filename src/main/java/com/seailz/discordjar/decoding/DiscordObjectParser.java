package com.seailz.discordjar.decoding;

import com.seailz.discordjar.DiscordJar;
import com.seailz.discordjar.decoding.annotations.DiscordJarParameter;
import com.seailz.discordjar.decoding.annotations.DiscordObjectConstructor;
import com.seailz.discordjar.decoding.annotations.DiscordObjectCustomAssignationsMethod;
import com.seailz.discordjar.decoding.annotations.DiscordObjectParameter;
import com.seailz.discordjar.decoding.data.DiscordObjectInformation;
import com.seailz.discordjar.decoding.data.DiscordObjectParameterInformation;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses objects from JSON into Java Objects ({@link DiscordObject}). Designed using reflection
 * to improve the developer experience of working on discord.jar and decrease
 * the amount of repeated code.
 * <p>
 * Each DiscordJar instance uses its own parser as if a parameter is marked as {@link DiscordObjectParameter},
 * the parser needs to provide a DiscordJar instance.
 */
public class DiscordObjectParser {

    @Getter
    private DiscordJar discordJar;

    public DiscordObjectParser(DiscordJar discordJar) {
        this.discordJar = discordJar;
    }

    /**
     * Since reflection isn't instant, there's a cache to prevent un-needed delays.
     */
    private HashMap<Class<? extends DiscordObject>, DiscordObjectInformation> objectInformationCache = new HashMap<>();

    public <T extends DiscordObject> T decompileObject(JSONObject data, Class<T> type) {
        long start = System.currentTimeMillis();
        DiscordObjectInformation objInfo = objectInformationCache.get(type);

        if (objInfo == null) {
            objInfo = discoverObject(type);
        }
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to discover object info");

        List<Object> parameterValues = new ArrayList<>();
        start = System.currentTimeMillis();
        HashMap<String, Object> customAssignations = invokeCustomAssignationsMethod(objInfo, data);
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to discover custom assignations");

        start = System.currentTimeMillis();
        for (DiscordObjectParameterInformation param : objInfo.parameterList()) {
            if (customAssignations != null && customAssignations.containsKey(param.determineKey())) {
                parameterValues.add(customAssignations.get(param.determineKey()));
                continue;
            }
            parameterValues.add(param.determineValue(data, this));
        }
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to discover parameter values");

        try {
            start = System.currentTimeMillis();
            if (objInfo.constructor() == null) {
                Logger.getLogger("discord.jar")
                        .severe("[discord.jar - object decoding] Unable to find valid constructor for " +
                                type.getName() + " - returning null, please contact discord.jar's developers.");
                return null;
            }

            System.out.println(Arrays.toString(parameterValues.toArray()));
            System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to check constructor");

            start = System.currentTimeMillis();
            T obj = (T) objInfo.constructor()
                    .newInstance(parameterValues.toArray());
            System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to create new instance of object");
            return obj;
        } catch (Exception e) {
            Logger.getLogger("discord.jar")
                    .severe("[discord.jar - object decoding] Unable to decode object for " + type.getName() + ", printing stacktrace," +
                            " returning null. Please contact discord.jar's developers.");
            e.printStackTrace();
        }

        return null;
    }

    private DiscordObjectInformation discoverObject(Class<? extends DiscordObject> clazz) {
        long start = System.currentTimeMillis();
        List<DiscordObjectParameterInformation> parameterList = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            // If an annotation isn't present, we don't really care.
            if (!field.isAnnotationPresent(DiscordObjectParameter.class)) continue;
            DiscordObjectParameter param = field.getAnnotation(DiscordObjectParameter.class);

            DiscordObjectParameterInformation info = new DiscordObjectParameterInformation(
                    field.getName(),
                    field,
                    field.getType(),
                    field.isAnnotationPresent(DiscordJarParameter.class),
                    param
            );

            parameterList.add(info);
        }

        System.out.println("Time to discover object: " + (System.currentTimeMillis() - start) + "ms");

        DiscordObjectInformation info = new DiscordObjectInformation(parameterList, discoverCustomAssignationsMethod(clazz), discoverConstructor(clazz));
        objectInformationCache.remove(clazz);
        objectInformationCache.put(clazz, info);

        return info;
    }

    /**
     * Finds a constructor marked with {@link DiscordObjectConstructor} within a class.
     * Ideally this shouldn't return null, but don't rule it out.
     */
    @Nullable
    private Constructor<DiscordObject> discoverConstructor(Class<? extends DiscordObject> clazz) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (!constructor.isAnnotationPresent(DiscordObjectConstructor.class)) continue;
            return (Constructor<DiscordObject>) constructor;
        }
        return null;
    }

    /**
     * discord.jar allows objects to come up with their own method for decompiling certain parameters.
     * <br>To do this, an object would define a static method that takes a {@link JSONObject}, returns a
     * {@link HashMap HashMap<String, Object>} of JSON keys to values and is annotated with
     * {@link DiscordObjectCustomAssignationsMethod}.
     */
    private Method discoverCustomAssignationsMethod(Class<? extends DiscordObject> clazz) {
        for (Method method : clazz.getMethods()) {
            if (!method.isAnnotationPresent(DiscordObjectCustomAssignationsMethod.class)) continue;

            if (!Modifier.isStatic(method.getModifiers())) {
                Logger.getLogger("discord.jar").warning("[discord.jar - object decoding] Defined custom assignations method" +
                        " for " + clazz.getName() + " was not static. Ignoring custom assignations - please contact the discord.jar" +
                        " developers.");
                return null;
            }
            if (!method.getReturnType().equals(HashMap.class)) {
                Logger.getLogger("discord.jar").warning("[discord.jar - object decoding] Defined custom assignations method" +
                        " for " + clazz.getName() + " doesn't return a HashMap. Ignoring custom assignations - please contact the discord.jar" +
                        " developers.");
                return null;
            }
            if (!Arrays.equals(method.getParameterTypes(), new Class[]{JSONObject.class})) {
                Logger.getLogger("discord.jar").warning("[discord.jar - object decoding] Defined custom assignations method" +
                        " for " + clazz.getName() + " doesn't take a JSONObject as input. Ignoring custom assignations - please contact the discord.jar" +
                        " developers.");
                return null;
            }

            return method;
        }
        return null;
    }

    private HashMap<String, Object> invokeCustomAssignationsMethod(DiscordObjectInformation info, JSONObject data) {
        if (info.customAssignations() == null) return null;
        Object obj = null;
        try {
            obj = info.customAssignations().invoke(null, data);
        } catch (Exception e) {
            Logger.getLogger("discord.jar").warning("[discord.jar - object decoding] Error while finding custom " +
                    "assignations in the process of decoding an object - please contact the discord.jar developers with " +
                    "the stacktrace below.");
            e.printStackTrace();
            return null;
        }

        try {
            return (HashMap<String, Object>) obj;
        } catch (Exception e) {
            Logger.getLogger("discord.jar").warning("[discord.jar - object decoding] Error while finding custom " +
                    "assignations in the process of decoding an object - couldn't cast to HashMap<String, Object> - please " +
                    "contact the discord.jar developers with the stacktrace below.");
            e.printStackTrace();
            return null;
        }
    }
}