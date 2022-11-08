package com.seailz.javadiscordwrapper;

import com.seailz.javadiscordwrapper.events.DiscordListener;
import com.seailz.javadiscordwrapper.events.EventDispatcher;
import com.seailz.javadiscordwrapper.events.annotation.EventMethod;
import com.seailz.javadiscordwrapper.events.model.command.CommandPermissionUpdateEvent;
import com.seailz.javadiscordwrapper.gateway.GatewayFactory;
import com.seailz.javadiscordwrapper.model.Application;
import com.seailz.javadiscordwrapper.model.channel.Channel;
import com.seailz.javadiscordwrapper.model.guild.Guild;
import com.seailz.javadiscordwrapper.model.Intent;
import com.seailz.javadiscordwrapper.model.User;
import com.seailz.javadiscordwrapper.model.status.Status;
import com.seailz.javadiscordwrapper.model.status.activity.Activity;
import com.seailz.javadiscordwrapper.model.status.activity.ActivityButton;
import com.seailz.javadiscordwrapper.model.status.activity.ActivityType;
import com.seailz.javadiscordwrapper.utils.discordapi.DiscordResponse;
import com.seailz.javadiscordwrapper.utils.discordapi.Requester;
import com.seailz.javadiscordwrapper.utils.URLS;
import com.seailz.javadiscordwrapper.utils.cache.Cache;
import com.seailz.javadiscordwrapper.model.status.StatusType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.socket.TextMessage;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * The main class of the Discord.jv wrapper
 * @author Seailz
 * @since 1.0
 */
public class DiscordJv {

    /**
     * The token of the bot
     */
    private String token;
    /**
     * Used to manage the gateway connection
     */
    private GatewayFactory gatewayFactory;
    /**
     * Stores the logger
     */
    private Logger logger;
    /**
     * Intents the bot will use when connecting to the gateway
     */
    private EnumSet<Intent> intents;
    /**
     * Used for caching guilds in memory
     */
    private Cache<Guild> guildCache;
    /**
     * Used for caching users in memory
     */
    private Cache<User> userCache;
    /**
     * Manages dispatching events to listeners
     */
    private EventDispatcher eventDispatcher;

    /**
     * Creates a new instance of the DiscordJv class
     * @param token   The token of the bot
     * @param intents The intents the bot will use
     *
     * @throws ExecutionException
     *         If an error occurs while connecting to the gateway
     * @throws InterruptedException
     *         If an error occurs while connecting to the gateway
     */
    public DiscordJv(String token, EnumSet<Intent> intents) throws ExecutionException, InterruptedException {
        this.token = token;
        this.intents = intents;
        logger = Logger.getLogger("DiscordJv");
        this.gatewayFactory = new GatewayFactory(this);
        this.guildCache = new Cache<>();
        this.userCache = new Cache<>();
        this.eventDispatcher = new EventDispatcher(this);

        initiateNoShutdown();
    }

    // This method is used for testing purposes only
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        String token = "";
        File file = new File("token.txt");
        // get first line of that file
        try (FileReader reader = new FileReader(file)) {
            token = new BufferedReader(reader).readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DiscordJv discordJv = new DiscordJv(token, EnumSet.of(Intent.GUILDS, Intent.GUILD_MESSAGES));
        discordJv.registerListeners(
                new DiscordListener() {
                    @Override
                    @EventMethod
                    public void onCommandPermissionUpdate(@NotNull CommandPermissionUpdateEvent event) {
                        System.out.println("Message received: " + event.getPermissions().permissions().length);
                    }
                }
        );

        ArrayList<Activity> activities = new ArrayList<>();
        activities.add(
                new Activity("Hello World2", ActivityType.WATCHING)
        );
        Status status = new Status(0, activities.toArray(new Activity[0]), StatusType.DO_NOT_DISTURB, false);
        discordJv. setStatus(status);
    }

    /**
     * Stops the bot from shutting down when processes are finished
     */
    public void initiateNoShutdown() {
        // stop program from shutting down
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Sets the bot's status
     *
     * @param status
     *        The status to set
     *
     * @throws IOException
     *         If an error occurs while setting the status
     */
    public void setStatus(Status status) throws IOException {
        JSONObject json = new JSONObject();
        json.put("d", status.compile());
        json.put("op", 3);
        gatewayFactory.queueMessage(json);
    }

    /**
     * Returns info about a user
     *
     * @param id
     *        The id of the user
     *
     * @return The user
     */
    public User getUserById(String id) {
        DiscordResponse response = Requester.get(URLS.GET.USER.GET_USER.replace("{user.id}", id), this);
        return User.decompile(response.body());
    }

    /**
     * Returns info about a user
     *
     * @param id
     *        The id of the user
     *
     * @return The user
     */
    public User getUserById(long id) {
        return getUserById(String.valueOf(id));
    }

    public Channel getChannelById(String id) {
        DiscordResponse response = Requester.get(URLS.GET.CHANNELS.GET_CHANNEL.replace("{channel.id}", id), this);
        return Channel.decompile(response.body());
    }

    /**
     * Registers a listener, or multiple, with the event dispatcher
     * @param listeners The listeners to register
     */
    public void registerListeners(DiscordListener... listeners) {
        eventDispatcher.addListener(listeners);
    }

    /**
     * Returns the bot's token inputted in the constructor
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns the bot's intents inputted in the constructor
     */
    public EnumSet<Intent> getIntents() {
        return intents;
    }

    /**
     * Returns the cache for storing guilds
     */
    public Cache<Guild> getGuildCache() {
        return guildCache;
    }

    /**
     * Returns a {@link Application} object containing information about the bot
     */
    public Application getSelfInfo() {
        return Application.decompile(Requester.get(URLS.GET.APPLICATION.APPLICATION_INFORMATION, this).body());
    }

    /**
     * Returns the cache for storing users
     */
    public Cache<User> getUserCache() {
        return userCache;
    }

    /**
     * Returns the event dispatcher
     */
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

}
