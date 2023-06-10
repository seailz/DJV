package com.seailz.discordjar;

import com.seailz.discordjar.action.guild.GetCurrentUserGuildsAction;
import com.seailz.discordjar.command.Command;
import com.seailz.discordjar.command.CommandChoice;
import com.seailz.discordjar.command.CommandDispatcher;
import com.seailz.discordjar.command.CommandOption;
import com.seailz.discordjar.command.annotation.ContextCommandInfo;
import com.seailz.discordjar.command.annotation.Locale;
import com.seailz.discordjar.command.annotation.SlashCommandInfo;
import com.seailz.discordjar.command.listeners.CommandListener;
import com.seailz.discordjar.command.listeners.MessageContextCommandListener;
import com.seailz.discordjar.command.listeners.UserContextCommandListener;
import com.seailz.discordjar.command.listeners.slash.SlashCommandListener;
import com.seailz.discordjar.command.listeners.slash.SlashSubCommand;
import com.seailz.discordjar.command.listeners.slash.SubCommandListener;
import com.seailz.discordjar.events.DiscordListener;
import com.seailz.discordjar.events.EventDispatcher;
import com.seailz.discordjar.gateway.GatewayFactory;
import com.seailz.discordjar.http.HttpOnlyApplication;
import com.seailz.discordjar.model.api.APIRelease;
import com.seailz.discordjar.model.application.Application;
import com.seailz.discordjar.model.application.Intent;
import com.seailz.discordjar.model.channel.*;
import com.seailz.discordjar.model.channel.audio.VoiceRegion;
import com.seailz.discordjar.model.emoji.sticker.Sticker;
import com.seailz.discordjar.model.emoji.sticker.StickerPack;
import com.seailz.discordjar.model.guild.Guild;
import com.seailz.discordjar.model.invite.Invite;
import com.seailz.discordjar.model.invite.internal.InviteImpl;
import com.seailz.discordjar.model.status.Status;
import com.seailz.discordjar.model.user.User;
import com.seailz.discordjar.utils.Checker;
import com.seailz.discordjar.utils.HTTPOnlyInfo;
import com.seailz.discordjar.utils.URLS;
import com.seailz.discordjar.cache.Cache;
import com.seailz.discordjar.cache.JsonCache;
import com.seailz.discordjar.utils.memory.MemoryWatcher;
import com.seailz.discordjar.utils.rest.DiscordRequest;
import com.seailz.discordjar.utils.rest.DiscordResponse;
import com.seailz.discordjar.utils.rest.RequestQueueHandler;
import com.seailz.discordjar.utils.permission.Permission;
import com.seailz.discordjar.utils.rest.ratelimit.Bucket;
import com.seailz.discordjar.model.api.version.APIVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * The main class of the discord.jar wrapper for the Discord API. It is <b>HIGHLY</b> recommended that you use
 * {@link DiscordJarBuilder} for creating new instances of this class as the other constructors are deprecated
 * and will be set to protected/removed in the future.
 *
 * @author Seailz
 * @since 1.0
 */
public class DiscordJar {

    /**
     * The token of the bot
     */
    private final String token;
    /**
     * Used to manage the gateway connection
     */
    private GatewayFactory gatewayFactory;
    /**
     * Stores the logger
     */
    private final Logger logger;
    /**
     * Intents the bot will use when connecting to the gateway
     */
    private final EnumSet<Intent> intents;
    /**
     * Used for caching guilds in memory
     */
    private final Cache<Guild> guildCache;
    /**
     * Used for caching users in memory
     */
    private final Cache<User> userCache;
    /**
     * Used for caching channels in memory
     */
    private final Cache<Channel> channelCache;
    /**
     * Manages dispatching events to listeners
     */
    private final EventDispatcher eventDispatcher;
    /**
     * Queued messages to be sent to the Discord API incase the rate-limits are hit
     */
    private final List<DiscordRequest> queuedRequests;
    /**
     * The command dispatcher
     */
    protected final CommandDispatcher commandDispatcher;
    /**
     * A cache storing self user information
     */
    private JsonCache selfUserCache;
    private JsonCache getSelfUserCache;
    /**
     * Should the bot be in debug mode?
     */
    private boolean debug;
    /**
     * List of rate-limit buckets
     */
    private List<Bucket> buckets;
    private int shardId;
    private int numShards;
    /**
     * The current status of the bot, or null if not set.
     */
    private Status status;

    public int gatewayConnections = 0;
    public List<GatewayFactory> gatewayFactories = new ArrayList<>();

    /**
     * @deprecated Use {@link DiscordJarBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public DiscordJar(String token, EnumSet<Intent> intents, APIVersion version) throws ExecutionException, InterruptedException {
        this(token, intents, version, false, null, false, -1, -1, APIRelease.STABLE);
    }

    /**
     * @deprecated Use {@link DiscordJarBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public DiscordJar(String token, EnumSet<Intent> intents, APIVersion version, boolean debug) throws ExecutionException, InterruptedException {
        this(token, intents, version, false, null, debug, -1, -1, APIRelease.STABLE);
    }

    /**
     * @deprecated Use {@link DiscordJarBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public DiscordJar(String token, APIVersion version) throws ExecutionException, InterruptedException {
        this(token, EnumSet.of(Intent.ALL), version, false, null, false, -1, -1, APIRelease.STABLE);
    }

    /**
     * @deprecated Use {@link DiscordJarBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public DiscordJar(String token, APIVersion version, boolean httpOnly, HTTPOnlyInfo httpOnlyInfo) throws ExecutionException, InterruptedException {
        this(token, EnumSet.noneOf(Intent.class), version, httpOnly, httpOnlyInfo, false, -1, -1, APIRelease.STABLE);
    }

    /**
     * @deprecated Use {@link DiscordJarBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public DiscordJar(String token, boolean httpOnly, HTTPOnlyInfo httpOnlyInfo) throws ExecutionException, InterruptedException {
        this(token, EnumSet.noneOf(Intent.class), APIVersion.getLatest(), httpOnly, httpOnlyInfo, false, -1, -1, APIRelease.STABLE);
    }

        /**
         * Creates a new instance of the DiscordJar class
         * This will start the connection to the Discord gateway, set caches, set the event dispatcher, set the logger, set up eliminate handling, and initiates no shutdown
         *
         * @param token        The token of the bot
         * @param intents      The intents the bot will use
         * @param version      The version of the Discord API the bot will use
         * @param httpOnly     Makes your bot an <a href="https://discord.com/developers/docs/topics/gateway#privileged-intents">HTTP only bot</a>. This WILL
         *                     break some methods and is only recommended to be set to true if you know what you are doing. Otherwise, leave it to false or don't set it.
         *                     HTTP-only bots (or Interaction-only bots) are bots that do not connect to the gateway, and therefore cannot receive events. They receive
         *                     interactions through POST requests to a specified endpoint of your bot. This is useful if you want to make a bot that only uses slash commands.
         *                     Voice <b>will not work</b>, neither will {@link #setStatus(Status)} & most gateway events.
         *                     Interaction-based events will still be delivered as usual.
         *                     For a full tutorial, see the README.md file.
         * @param httpOnlyInfo The information needed to make your bot HTTP only. This is only needed if you set httpOnly to true, otherwise set to null.
         *                     See the above parameter for more information.
         * @param debug        Should the bot be in debug mode?
         * @throws ExecutionException   If an error occurs while connecting to the gateway
         * @throws InterruptedException If an error occurs while connecting to the gateway
         *
         * @deprecated Use {@link DiscordJarBuilder} instead. This constructor will be set to protected in the future.
         */
        @Deprecated
    public DiscordJar(String token, EnumSet<Intent> intents, APIVersion version, boolean httpOnly, HTTPOnlyInfo httpOnlyInfo, boolean debug, int shardId, int numShards, APIRelease release) throws ExecutionException, InterruptedException {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        new RequestQueueHandler(this);
        this.token = token;
        this.intents = intents;
        new URLS(release, version);
        logger = Logger.getLogger("DISCORD.JAR");
        this.commandDispatcher = new CommandDispatcher();
        this.queuedRequests = new ArrayList<>();
        this.buckets = new ArrayList<>();
        if (!httpOnly) {
            this.gatewayFactory = new GatewayFactory(this, debug, shardId, numShards);
        }
        this.debug = debug;
        this.guildCache = new Cache<>(this, Guild.class,
                new DiscordRequest(
                        new JSONObject(),
                        new HashMap<>(),
                        URLS.GET.GUILDS.GET_GUILD.replace("{guild.id}", "%s"),
                        this,
                        URLS.GET.GUILDS.GET_GUILD,
                        RequestMethod.GET
                ));

        this.userCache = new Cache<>(this, User.class, new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.GET.USER.GET_USER.replace("{user.id}", "%s"),
                this,
                URLS.GET.USER.GET_USER,
                RequestMethod.GET
        ));

        this.channelCache = new Cache<>(this, Channel.class, new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.GET.CHANNELS.GET_CHANNEL.replace("{channel.id}", "%s"),
                this,
                URLS.GET.CHANNELS.GET_CHANNEL,
                RequestMethod.GET
        ));

        this.eventDispatcher = new EventDispatcher(this);

        if (httpOnly) {
            if (httpOnlyInfo == null)
                throw new IllegalArgumentException("httpOnlyInfo cannot be null if httpOnly is true!");
            HttpOnlyApplication.init(this, httpOnlyInfo.endpoint(), httpOnlyInfo.applicationPublicKey());
        }

        initiateNoShutdown();
        initiateShutdownHooks();

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (gatewayFactory == null || (gatewayFactory.getSession() != null && !gatewayFactory.getSession().isOpen())) {
                    restartGateway();
                }
            }
        }).start();
        this.shardId = shardId;
        this.numShards = numShards;

        new MemoryWatcher(this).start();
    }

    /**
     * @deprecated Use {@link DiscordJarBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public DiscordJar(String token) throws ExecutionException, InterruptedException {
        this(token, EnumSet.of(Intent.ALL), APIVersion.getLatest());
    }

    /**
     * @deprecated Use {@link DiscordJarBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public DiscordJar(String token, boolean debug) throws ExecutionException, InterruptedException {
        this(token, EnumSet.of(Intent.ALL), APIVersion.getLatest(), debug);
    }

    /**
     * @deprecated Use {@link DiscordJarBuilder} instead.
     */
    @Deprecated(forRemoval = true)
    public DiscordJar(String token, EnumSet<Intent> intents) throws ExecutionException, InterruptedException {
        this(token, intents, APIVersion.getLatest());
    }

    /**
     * Stops the bot from shutting down when processes are finished
     */
    protected void initiateNoShutdown() {
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

    public GatewayFactory getGateway() {
        return gatewayFactory;
    }

    /**
     * Kills the gateway connection and destroys the {@link GatewayFactory} instance.
     * This method will also initiate garbage collection to avoid memory leaks. This probably shouldn't be used unless in {@link #restartGateway()}.
     */
    public void killGateway() {
        try {
            if (gatewayFactory != null) gatewayFactory.killConnection();
        } catch (IOException ignored) {}
        gatewayFactory = null;
        // init garbage collection to avoid memory leaks
        System.gc();
    }

    /**
     * Restarts the gateway connection and creates a new {@link GatewayFactory} instance.
     * This will invalidate and destroy the old {@link GatewayFactory} instance.
     * This method will also initiate garbage collection to avoid memory leaks.
     *
     * @see GatewayFactory
     * @see #killGateway()
     */
    public void restartGateway() {
        killGateway();
        try {
            gatewayFactory = new GatewayFactory(this, debug, shardId, numShards);
            gatewayFactories.add(gatewayFactory);
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void initiateShutdownHooks() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (gatewayFactory != null) {
                try {
                    gatewayFactory.killConnectionNicely();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }));
    }

    public void setGatewayFactory(GatewayFactory gatewayFactory) {
        this.gatewayFactory = gatewayFactory;
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }

    public Bucket getBucket(String id) {
        for (Bucket bucket : buckets) {
            if (bucket.getId().equals(id)) return bucket;
        }
        return null;
    }

    public void updateBucket(String id, Bucket bucket) {
        for (int i = 0; i < buckets.size(); i++) {
            if (buckets.get(i).getId().equals(id)) {
                buckets.set(i, bucket);
                return;
            }
        }
        buckets.add(bucket);
    }

    public void removeBucket(Bucket bucket) {
        buckets.remove(bucket);
    }

    public Bucket getBucketForUrl(String url) {
        for (Bucket bucket : buckets) {
            if (bucket.getAffectedRoutes().contains(url)) return bucket;
        }
        return null;
    }

    /**
     * Sets the bot's status
     *
     * @param status The status to set
     */
    public void setStatus(@NotNull Status status) {
        if (gatewayFactory == null)
            throw new IllegalStateException("Cannot set status on an HTTP-only bot. See the constructor for more information.");
        JSONObject json = new JSONObject();
        json.put("d", status.compile());
        json.put("op", 3);
        gatewayFactory.queueMessage(json);
        gatewayFactory.setStatus(status);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Returns info about a user
     *
     * @param id The id of the user
     * @return The user
     */
    @Nullable
    public User getUserById(String id) {
        Checker.isSnowflake(id, "Given id is not a snowflake");
        try {
            return userCache.getById(id);
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }


    /**
     * Returns info about the bot user
     * This shouldn't return null, but it might if the Discord API didn't respond correctly.
     *
     * @return a {@link User} object
     */
    @Nullable
    public User getSelfUser() {
        if (this.getSelfUserCache != null && getSelfUserCache.get() != null)
            return User.decompile(getSelfUserCache.get(), this);

        DiscordRequest req = new DiscordRequest(
                new JSONObject(), new HashMap<>(),
                URLS.GET.USER.GET_USER.replace("{user.id}", "@me"),
                this, URLS.GET.USER.GET_USER, RequestMethod.GET
        );
        DiscordResponse response = null;
        try {
            response = req.invoke();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }

        if (getSelfUserCache == null) {
            getSelfUserCache = JsonCache.newc(response.body(), req);
            getSelfUserCache.reset(60000);
        }
        this.getSelfUserCache.update(response.body());
        return User.decompile(response.body(), this);
    }

    /**
     * Returns info about a user
     *
     * @param id The id of the user
     * @return The user
     */
    @Nullable
    public User getUserById(long id) {
        Checker.isSnowflake(String.valueOf(id), "Given id is not a snowflake");
        return getUserById(String.valueOf(id));
    }

    /**
     * Returns info about a {@link Channel}
     *
     * @param id The id of the channel
     * @return A {@link Channel} object
     */
    @Nullable
    public Channel getChannelById(String id) {
        Checker.isSnowflake(id, "Given id is not a snowflake");
        Cache<Channel> cc = getChannelCache();
        Channel res;
        try {
            res = cc.getById(id);
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return res;
    }

    /**
     * Returns info about a {@link Channel}
     *
     * @param id The id of the channel
     * @return A {@link Channel} object
     */
    @Nullable
    public MessagingChannel getTextChannelById(String id) {
        Checker.isSnowflake(id, "Given id is not a snowflake");
        JSONObject raw = null;
        try {
            raw = getChannelCache().getById(id).raw();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return MessagingChannel.decompile(raw, this);
    }

    /**
     * Returns info about a {@link com.seailz.discordjar.model.channel.thread.Thread Thread}
     *
     * @param id The id of the channel
     * @return A {@link com.seailz.discordjar.model.channel.thread.Thread} object
     */
    @Nullable
    public com.seailz.discordjar.model.channel.thread.Thread getThreadById(String id) {
        Checker.isSnowflake(id, "Given id is not a snowflake");
        JSONObject raw = null;
        try {
            raw = getChannelCache().getById(id).raw();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return com.seailz.discordjar.model.channel.thread.Thread.decompile(raw, this);
    }

    /**
     * Returns info about a {@link DMChannel}
     *
     * @param id The id of the channel
     * @return A {@link DMChannel} object
     */
    @Nullable
    public DMChannel getDmChannelById(String id) {
        Checker.isSnowflake(id, "Given id is not a snowflake");
        JSONObject raw = null;
        try {
            raw = getChannelCache().getById(id).raw();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return DMChannel.decompile(raw, this);
    }

    /**
     * Returns info about a {@link ForumChannel}
     *
     * @param id The id of the channel
     * @return A {@link ForumChannel} object
     */
    @Nullable
    public ForumChannel getForumChannelById(String id) {
        Checker.isSnowflake(id, "Given id is not a snowflake");
        JSONObject raw = null;
        try {
            raw = getChannelCache().getById(id).raw();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return ForumChannel.decompile(raw, this);
    }

    /**
     * Returns info about a {@link Category}
     *
     * @param id The id of the category
     * @return A {@link Category} object
     */
    @Nullable
    public Category getCategoryById(String id) {
        Checker.isSnowflake(id, "Given id is not a snowflake");
        JSONObject raw = null;
        try {
            raw = getChannelCache().getById(id).raw();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return Category.decompile(raw, this);
    }

    /**
     * Returns info about a {@link Guild}
     *
     * @param id The id of the guild
     * @return A {@link Guild} object
     */
    @Nullable
    public Guild getGuildById(long id) {
        Checker.isSnowflake(String.valueOf(id), "Given id is not a snowflake");
        return getGuildById(String.valueOf(id));
    }

    /**
     * Returns info about a {@link Guild}
     *
     * @param id The id of the guild
     * @return A {@link Guild} object
     */
    @Nullable
    public Guild getGuildById(String id) {
        Checker.isSnowflake(id, "Given id is not a snowflake");
        try {
            return getGuildCache().getById(id);
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }

    /**
     * Returns info about a {@link Sticker}
     *
     * @param id The id of the sticker
     * @return A {@link Sticker} object
     */
    @Nullable
    public Sticker getStickerById(String id) {
        Checker.isSnowflake(id, "Given id is not a snowflake");
        try {
            return Sticker.decompile(new DiscordRequest(
                    new JSONObject(), new HashMap<>(),
                    URLS.GET.STICKER.GET_STICKER.replace("{sticker.id}", id),
                    this, URLS.GET.STICKER.GET_STICKER, RequestMethod.GET
            ).invoke().body(), this);
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }

    /**
     * Returns a list of {@link StickerPack StickerPacks} that nitro subscribers can use
     *
     * @return List of {@link StickerPack StickerPacks}
     */
    public List<StickerPack> getNitroStickerPacks() {
        try {
            return StickerPack.decompileList(new DiscordRequest(
                    new JSONObject(), new HashMap<>(),
                    URLS.GET.STICKER.GET_NITRO_STICKER_PACKS,
                    this, URLS.GET.STICKER.GET_NITRO_STICKER_PACKS, RequestMethod.GET
            ).invoke().body(), this);
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }


    /**
     * Registers a listener, or multiple, with the event dispatcher
     *
     * @param listeners The listeners to register
     */
    public void registerListeners(@NotNull DiscordListener... listeners) {
        eventDispatcher.addListener(listeners);
    }

    /**
     * Returns the bot's token inputted in the constructor
     */
    @NotNull
    public String getToken() {
        return token;
    }

    /**
     * Returns the bot's intents inputted in the constructor
     */
    @NotNull
    public EnumSet<Intent> getIntents() {
        return intents;
    }

    /**
     * Returns the cache for storing guilds
     */
    @NotNull
    public Cache<Guild> getGuildCache() {
        return guildCache;
    }

    /**
     * Returns a {@link Application} object containing information about the bot
     */
    @Nullable
    public Application getSelfInfo() {
        if (this.selfUserCache != null && !selfUserCache.isEmpty())
            return Application.decompile(selfUserCache.get(), this);

        DiscordRequest request = new DiscordRequest(
                new JSONObject(), new HashMap<>(),
                URLS.GET.APPLICATION.APPLICATION_INFORMATION,
                this, URLS.GET.APPLICATION.APPLICATION_INFORMATION, RequestMethod.GET
        );
        DiscordResponse response = null;
        try {
            response = request.invoke();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            if (e.getHttpCode() == 404) return null;
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }

        if (this.selfUserCache == null)
            this.selfUserCache = JsonCache.newc(response.body(), request);
        this.selfUserCache.update(response.body());

        return Application.decompile(response.body(), this);
    }

    /**
     * Returns the cache for storing users
     */
    @NotNull
    public Cache<User> getUserCache() {
        return userCache;
    }

    /**
     * Returns the event dispatcher
     */
    @NotNull
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Returns the command dispatcher
     */
    @NotNull
    public CommandDispatcher getCommandDispatcher() {
        return commandDispatcher;
    }

    /**
     * Gets queued requests
     */
    @NotNull
    public List<DiscordRequest> getQueuedRequests() {
        return queuedRequests;
    }

    /**
     * Get channel cache
     */
    @NotNull
    public Cache<Channel> getChannelCache() {
        return channelCache;
    }

    /**
     * Adds an intent
     */
    public void addIntent(@NotNull Intent intent) {
        intents.add(intent);
    }

    /**
     * Registers command(s) with Discord.
     *
     * @param listeners The listeners/commands to register
     * @throws IllegalArgumentException <ul>
     *                                  <li>If the command name is less than 1 character or more than 32 characters</li>
     *
     *                                  <li>If the command description is less than 1 character or more than 100 characters</li>
     *
     *                                  <li>If the command options are more than 25</li>
     *
     *                                  <li>If a command option name is less than 1 character or more than 32 characters</li>
     *
     *                                  <li>If a command option description is less than 1 character or more than 100 characters</li>
     *
     *                                  <li>If a command option choices are more than 25</li>
     *
     *                                  <li>If a command option choice name is less than 1 character or more than 100 characters</li>
     *
     *                                  <li>If a command option choice value is less than 1 character or more than 100 characters</li></ul>
     */
    public void registerCommands(CommandListener... listeners) {
        for (CommandListener listener : listeners) {
            Checker.check((listener instanceof SlashCommandListener) && !listener.getClass().isAnnotationPresent(SlashCommandInfo.class), "SlashCommandListener must have @SlashCommandInfo annotation");
            Checker.check((listener instanceof MessageContextCommandListener || listener instanceof UserContextCommandListener)
                    && !listener.getClass().isAnnotationPresent(ContextCommandInfo.class), "Context commands must have @ContextCommandInfo annotation");

            Annotation ann = listener.getClass().isAnnotationPresent(SlashCommandInfo.class) ? listener.getClass().getAnnotation(SlashCommandInfo.class) : listener.getClass().getAnnotation(ContextCommandInfo.class);
            String name = (ann instanceof SlashCommandInfo) ? ((SlashCommandInfo) ann).name() : ((ContextCommandInfo) ann).value();
            String description = (ann instanceof SlashCommandInfo) ? ((SlashCommandInfo) ann).description() : "";

            Locale[] nameLocales = (ann instanceof SlashCommandInfo) ? ((SlashCommandInfo) ann).nameLocalizations() : ((ContextCommandInfo) ann).nameLocalizations();
            Locale[] descriptionLocales = (ann instanceof SlashCommandInfo) ? ((SlashCommandInfo) ann).descriptionLocalizations() : ((ContextCommandInfo) ann).descriptionLocalizations();
            Permission[] defaultMemberPermissions = (ann instanceof SlashCommandInfo) ? ((SlashCommandInfo) ann).defaultMemberPermissions() : ((ContextCommandInfo) ann).defaultMemberPermissions();
            boolean canUseInDms = (ann instanceof SlashCommandInfo) ? ((SlashCommandInfo) ann).canUseInDms() : ((ContextCommandInfo) ann).canUseInDms();
            boolean nsfw = (ann instanceof SlashCommandInfo) ? ((SlashCommandInfo) ann).nsfw() : ((ContextCommandInfo) ann).nsfw();
            new Thread(() -> {
                registerCommand(
                        new Command(
                                name,
                                listener.getType(),
                                description,
                                (listener instanceof SlashCommandListener) ? ((SlashCommandListener) listener).getOptions() : new ArrayList<>(),
                                nameLocales,
                                descriptionLocales,
                                defaultMemberPermissions,
                                canUseInDms,
                                nsfw
                        )
                );
            }).start();
            commandDispatcher.registerCommand(name, listener);

            if (!(listener instanceof SlashCommandListener slashCommandListener)) continue  ;
            if (slashCommandListener.getSubCommands().isEmpty()) continue;

            for (SlashSubCommand subCommand : slashCommandListener.getSubCommands().keySet()) {
                SubCommandListener subListener =
                        slashCommandListener.getSubCommands().values().stream().toList().get(
                                slashCommandListener.getSubCommands().keySet().stream().toList()
                                        .indexOf(subCommand)
                        );
                commandDispatcher.registerSubCommand(slashCommandListener, subCommand, subListener);
            }
        }
    }

    protected void registerCommand(Command command) {
        Checker.check(!(command.name().length() > 1 && command.name().length() < 32), "Command name must be within 1 and 32 characters!");
        Checker.check(!Objects.equals(command.description(), "") && !(command.description().length() > 1 && command.description().length() < 100), "Command description must be within 1 and 100 characters!");
        Checker.check(command.options().size() > 25, "Application commands can only have up to 25 options!");

        for (CommandOption o : command.options()) {
            Checker.check(!(o.name().length() > 1 && o.name().length() < 32), "Option name must be within 1 and 32 characters!");
            if (o.description() != null) Checker.check(!(o.description().length() > 1 && o.description().length() < 100), "Option description must be within 1 and 100 characters!");
            if (o.choices() != null) Checker.check(o.choices().size() > 25, "Command options can only have up to 25 choices!");

            if (o.choices() != null) {
                for (CommandChoice c : o.choices()) {
                    Checker.check(!(c.name().length() > 1 && c.name().length() < 100), "Choice name must be within 1 and 100 characters!");
                    Checker.check(!(c.value().length() > 1 && c.value().length() < 100), "Choice value must be within 1 and 100 characters!");
                }
            }
        }

        DiscordRequest commandReq = new DiscordRequest(
                command.compile(),
                new HashMap<>(),
                URLS.POST.COMMANDS.GLOBAL_COMMANDS.replace("{application.id}", getSelfInfo().id()),
                this,
                URLS.BASE_URL,
                RequestMethod.POST);
        try {
            commandReq.invoke();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }

    /**
     * Clears all the global commands for this app. Cannot be undone.
     */
    public void clearCommands() {
        DiscordRequest cmdDelReq = new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.POST.COMMANDS.GLOBAL_COMMANDS.replace("{application.id}", getSelfInfo().id()),
                this,
                URLS.BASE_URL,
                RequestMethod.PUT
        );
        try {
            cmdDelReq.invoke(new JSONArray());
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }

    /**
     * Returns all the global commands for this app.
     * @param withLocalizations Whether to include localizations for the commands.
     * @return List of {@link Command} objects.
     */
    @Nullable
    public List<Command> getGlobalCommands(boolean withLocalizations) {
        DiscordRequest req = new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.GET.APPLICATION.COMMANDS.GET_GLOBAL_APPLICATION_COMMANDS.replace("{application.id}", getSelfInfo().id()) + (withLocalizations ? "?with_localizations=true" : ""),
                this,
                URLS.GET.APPLICATION.COMMANDS.GET_GLOBAL_APPLICATION_COMMANDS,
                RequestMethod.GET
        );
        JSONArray res = null;
        try {
            res = req.invoke().arr();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        List<Command> commands = new ArrayList<>();
        res.forEach(o -> commands.add(Command.decompile((JSONObject) o)));
        return commands;
    }

    @Nullable
    public List<Command> getGlobalCommands() {
        return getGlobalCommands(true);
    }

    /**
     * Returns a global command for this app.
     * @param commandId The id of the command.
     * @return The {@link Command} object.
     */
    @Nullable
    public Command getGlobalCommand(String commandId) {
        DiscordRequest req = new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.GET.APPLICATION.COMMANDS.GET_GLOBAL_APPLICATION_COMMAND.replace("{application.id}", getSelfInfo().id()).replace("{command.id}", commandId),
                this,
                URLS.GET.APPLICATION.COMMANDS.GET_GLOBAL_APPLICATION_COMMAND,
                RequestMethod.GET
        );
        JSONObject res = null;
        try {
            res = req.invoke().body();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return Command.decompile(res);
    }

    /**
     * Edits a global command for this app.
     * @param newCommand The new command.
     * @param commandId The id of the command.
     * @return The {@link Command} object.
     */
    @Nullable
    public Command editGlobalCommand(@NotNull Command newCommand, String commandId) {
        DiscordRequest req = new DiscordRequest(
                newCommand.compile(),
                new HashMap<>(),
                URLS.PATCH.APPLICATIONS.COMMANDS.EDIT_GLOBAL_COMMAND.replace("{application.id}", getSelfInfo().id()).replace("{command.id}", commandId),
                this,
                URLS.PATCH.APPLICATIONS.COMMANDS.EDIT_GLOBAL_COMMAND,
                RequestMethod.PATCH
        );
        JSONObject res = null;
        try {
            res = req.invoke().body();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return Command.decompile(res);
    }

    /**
     * Deletes a global command for this app.
     * @param commandId The id of the command.
     */
    public void deleteGlobalCommand(String commandId) {
        DiscordRequest req = new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.DELETE.APPLICATION.COMMANDS.DELETE_GLOBAL_COMMAND.replace("{application.id}", getSelfInfo().id()).replace("{command.id}", commandId),
                this,
                URLS.DELETE.APPLICATION.COMMANDS.DELETE_GLOBAL_COMMAND,
                RequestMethod.DELETE
        );
        try {
            req.invoke();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }

    /**
     * Bulk overwrites all the global commands for this app.
     * @param commands The new commands to overwrite with.
     */
    public void bulkOverwriteCommands(@NotNull List<Command> commands) {
        JSONArray arr = new JSONArray();
        commands.forEach(c -> arr.put(c.compile()));
        DiscordRequest req = new DiscordRequest(
                arr,
                new HashMap<>(),
                URLS.POST.COMMANDS.GLOBAL_COMMANDS.replace("{application.id}", getSelfInfo().id()),
                this,
                URLS.BASE_URL,
                RequestMethod.PUT
        );
        try {
            req.invoke();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }

    /**
     * Returns all the guild commands for this app.
     * @param guildId The id of the guild.
     * @param withLocalizations Whether to include localizations for the commands.
     * @return List of {@link Command} objects.
     */
    @Nullable
    public List<Command> getGuildCommands(String guildId, boolean withLocalizations) {
        DiscordRequest req = new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.GET.APPLICATION.COMMANDS.GET_GUILD_APPLICATION_COMMANDS.replace("{application.id}", getSelfInfo().id()).replace("{guild.id}", guildId) + (withLocalizations ? "?with_localizations=true" : ""),
                this,
                URLS.GET.APPLICATION.COMMANDS.GET_GUILD_APPLICATION_COMMANDS,
                RequestMethod.GET
        );
        JSONArray res = null;
        try {
            res = req.invoke().arr();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        List<Command> commands = new ArrayList<>();
        res.forEach(o -> commands.add(Command.decompile((JSONObject) o)));
        return commands;
    }

    @Nullable
    public List<Command> getGuildCommands(String guildId) {
        return getGuildCommands(guildId, true);
    }

    /**
     * Returns a guild command for this app.
     * @param guildId The id of the guild.
     * @param commandId The id of the command.
     * @return The {@link Command} object.
     */
    @Nullable
    public Command getGuildCommand(String guildId, String commandId) {
        DiscordRequest req = new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.GET.APPLICATION.COMMANDS.GET_GUILD_APPLICATION_COMMAND.replace("{application.id}", getSelfInfo().id()).replace("{guild.id}", guildId).replace("{command.id}", commandId),
                this,
                URLS.GET.APPLICATION.COMMANDS.GET_GUILD_APPLICATION_COMMAND,
                RequestMethod.GET
        );
        JSONObject res = null;
        try {
            res = req.invoke().body();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return Command.decompile(res);
    }

    /**
     * Edits a guild command for this app.
     * @param newCommand The new command.
     * @param guildId The id of the guild.
     * @param commandId The id of the command.
     * @return The edited {@link Command} object.
     */
    @Nullable
    public Command editGuildCommand(@NotNull Command newCommand, String guildId, String commandId) {
        DiscordRequest req = new DiscordRequest(
                newCommand.compile(),
                new HashMap<>(),
                URLS.PATCH.APPLICATIONS.COMMANDS.EDIT_GUILD_COMMAND.replace("{application.id}", getSelfInfo().id()).replace("{guild.id}", guildId).replace("{command.id}", commandId),
                this,
                URLS.PATCH.APPLICATIONS.COMMANDS.EDIT_GUILD_COMMAND,
                RequestMethod.PATCH
        );
        JSONObject res = null;
        try {
            res = req.invoke().body();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        return Command.decompile(res);
    }

    /**
     * Deletes a guild command for this app.
     * @param guildId The id of the guild.
     * @param commandId The id of the command.
     */
    public void deleteGuildCommand(String guildId, String commandId) {
        DiscordRequest req = new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.DELETE.APPLICATION.COMMANDS.DELETE_GUILD_COMMAND.replace("{application.id}", getSelfInfo().id()).replace("{guild.id}", guildId).replace("{command.id}", commandId),
                this,
                URLS.DELETE.APPLICATION.COMMANDS.DELETE_GUILD_COMMAND,
                RequestMethod.DELETE
        );
        try {
            req.invoke();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }

    /**
     * Bulk overwrites all the guild commands for this app.
     * @param commands The new commands to overwrite with.
     * @param guildId The id of the guild.
     */
    public void bulkOverwriteGuildCommands(@NotNull List<Command> commands, String guildId) {
        JSONArray arr = new JSONArray();
        commands.forEach(c -> arr.put(c.compile()));
        DiscordRequest req = new DiscordRequest(
                arr,
                new HashMap<>(),
                URLS.POST.COMMANDS.GUILD_COMMANDS.replace("{application.id}", getSelfInfo().id()).replace("{guild.id}", guildId),
                this,
                URLS.BASE_URL,
                RequestMethod.PUT
        );
        try {
            req.invoke();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }



    /**
     * Retrieves up to 200 guilds the bot is in.
     * <br>If you want to retrieve more guilds than that, you need to specify the last guild id in the <b>after</b> parameter.
     *<p>
     * Please be aware of the fact that this method is rate limited quite heavily.
     * <br>It is recommended that only smaller bots use this method.
     *<p>
     * If you need to retrieve a (possibly inaccurate) list of guilds as a larger bot, use {@link #getGuildCache()} instead.
     * <br>All guilds retrieved from this method will be cached.
     */
    public GetCurrentUserGuildsAction getGuilds() {
        return new GetCurrentUserGuildsAction(this);
    }

    /**
     * Retrieves all voice regions.
     * <br>They can be used to specify the rtc_region of a voice or stage channel.
     *
     * <p>You can find the RTC region of an {@link com.seailz.discordjar.model.channel.AudioChannel AudioChannel} by using {@link com.seailz.discordjar.model.channel.AudioChannel#region() AudioChannel#region()}.
     * <br>Avoid switching to deprecated regions.
     *
     * @return A list of all voice regions.
     */
    public List<VoiceRegion> getVoiceRegions() {
        DiscordRequest request = new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.GET.VOICE.REGIONS.GET_VOICE_REGIONS,
                this,
                URLS.GET.VOICE.REGIONS.GET_VOICE_REGIONS,
                RequestMethod.GET
        );
        JSONArray response = null;
        try {
            response = request.invoke().arr();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        List<VoiceRegion> regions = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            regions.add(VoiceRegion.decompile(response.getJSONObject(i)));
        }
        return regions;
    }

    /**
     * Retrieves an {@link com.seailz.discordjar.model.invite.Invite Invite} by its code.
     */
    @Nullable
    public Invite getInvite(String code) {
        DiscordRequest req = new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.GET.INVITES.GET_INVITE.replace("{invite.code}", code),
                this,
                URLS.BASE_URL,
                RequestMethod.GET
        );
        DiscordResponse res = null;
        try {
            res = req.invoke();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
        if (res.code() == 404) return null;
        return InviteImpl.decompile(res.body(), this);
    }

    /**
     * Deletes an {@link com.seailz.discordjar.model.invite.Invite Invite} by its code.
     */
    public void deleteInvite(String code) {
        DiscordRequest req = new DiscordRequest(
                new JSONObject(),
                new HashMap<>(),
                URLS.DELETE.INVITE.DELETE_INVITE.replace("{invite.code}", code),
                this,
                URLS.BASE_URL,
                RequestMethod.DELETE
        );
        try {
            req.invoke();
        } catch (DiscordRequest.UnhandledDiscordAPIErrorException e) {
            throw new DiscordRequest.DiscordAPIErrorException(e);
        }
    }

    public boolean isDebug() {
        return debug;
    }
}
