package com.seailz.discordjar.decoding.data;

import com.seailz.discordjar.decoding.DiscordObject;
import com.seailz.discordjar.decoding.DiscordObjectParser;
import com.seailz.discordjar.decoding.annotations.DiscordObjectParameter;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public record DiscordObjectParameterInformation(String name, Field field, Class<?> type, boolean discordJar, DiscordObjectParameter parameter) {

    @Nullable
    public String determineKey() {
        if (discordJar) return null;

        return !parameter.overrideKey().isEmpty() ?
                parameter.overrideKey() : name.toLowerCase();
    }

    // TODO: HashMap implementation
    @Nullable
    @CheckReturnValue
    public Object determineValue(JSONObject obj, DiscordObjectParser parserInstance) {
        if (discordJar) return parserInstance.getDiscordJar();

        if (!obj.has(determineKey())) {
            if (!parameter().excludable()) {
                // A parameter marked as required has not been included. We'll warn, but still return null.
                // This attempt is to cause the least collateral damage.
                Logger.getLogger("DiscordJarObjectDecoder")
                        .warning("[discord.jar - object decoder] A parameter marked as not excludable was not" +
                                " included in the input data. Possibly a key error? Attempting to proceed - please inform the " +
                                "discord.jar developers.");
            }
            return null;
        }
        if (obj.get(determineKey()) == JSONObject.NULL || obj.get(determineKey()) == null) {
            if (!parameter().nullable()) {
                // A parameter marked as not nullable was null. We'll warn, but still return null.
                // This attempt is to cause the least collateral damage.
                Logger.getLogger("DiscordJarObjectDecoder")
                        .warning("[discord.jar - object decoder] A parameter marked as not nullable was null" +
                                " in the input data. Attempting to proceed - please inform " +
                                "discord.jar developers.");
            }

            return null;
        }

        Class<?> realType = getArrayElementType();
        boolean array = realType != null;
        if (!array) realType = type;

        if (DiscordObject.class.isAssignableFrom(realType)) {
            Object data = obj.get(determineKey());
            Class<? extends DiscordObject> clazz = (Class<? extends DiscordObject>) realType;

            if (array && data instanceof JSONArray dataArray) {
                System.out.println("array");
                if (type.isArray()) {
                    System.out.println("Type is array");
                    // The type of the list is an array so we need to treat it as such
                    Object[] resultArray = (Object[]) Array.newInstance(clazz, dataArray.length());

                    int i = 0;
                    for (Object dataJson : dataArray) {
                        resultArray[i] = parserInstance.decompileObject((JSONObject) dataJson, clazz);
                        i++;
                    }

                    return resultArray;
                }

                // The type of the list is a typical List class rather than an array
                List<Object> returnList = new ArrayList<>();

                for (Object dataJson : dataArray) {
                    returnList.add(parserInstance.decompileObject((JSONObject) dataJson, clazz));
                }

                System.out.println(returnList);

                return returnList;
            }

            if (!(data instanceof JSONObject dataJson)) {
                Logger.getLogger("DiscordJarObjectDecoder")
                        .severe("[discord.jar - object decoder] A parameter marked as an object was decoded as" +
                                " a non-object. Returning null and attempting to proceed - please inform discord.jar " +
                                "developers.");
                return null;
            }
            return parserInstance.decompileObject(dataJson, clazz);
        }

        if (array) {
            if (!(obj.get(determineKey()) instanceof JSONArray dataArray)) {
                Logger.getLogger("discord.jar").warning("[discord.jar - object decoder] Value for parameter " +
                        determineKey() + " was marked as an array while provided data was " + obj.get(determineKey()));
                return null;
            }

            if (type.isArray()) {
                Object[] returnArray = (Object[]) Array.newInstance(realType, dataArray.length());

                int i = 0;
                for (Object dataJson : dataArray) {
                    returnArray[i] = dataJson;
                    i++;
                }

                return returnArray;
            }

            return dataArray.toList();
        }

        // TODO: lists for non-objects

        return obj.get(determineKey());
    }

    /**
     * Checks if the type for this parameter is a list of some sort (ArrayList, Array, etc), and if so,
     * returns the type that the array takes. If not, returns null.
     */
    @Nullable
    private Class<?> getArrayElementType() {
        if (!type.isArray() && !(type.isAssignableFrom(List.class))) return null;
        return getElementType(field);
    }

    public static Class<?> getElementType(Field field) {
        if (field.getType().isArray()) {
            return field.getType().getComponentType();
        } else if (Collection.class.isAssignableFrom(field.getType())) {
            Type type = field.getGenericType();
            if (type instanceof ParameterizedType pt) {
                for (Type actualTypeArgument : pt.getActualTypeArguments()) {
                    return (Class<?>) actualTypeArgument;
                }
            }
            return null;
        } else {
            return null;
        }
    }


}
