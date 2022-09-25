package net.ryanland.colossus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationFunction;
import net.dv8tion.jda.api.interactions.commands.localization.ResourceBundleLocalizationFunction;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.ryanland.colossus.command.Category;
import net.ryanland.colossus.command.Command;
import net.ryanland.colossus.command.CommandException;
import net.ryanland.colossus.command.ContextCommand;
import net.ryanland.colossus.command.arguments.parsing.exceptions.MalformedArgumentException;
import net.ryanland.colossus.command.executor.DisabledCommandHandler;
import net.ryanland.colossus.command.finalizers.CooldownFinalizer;
import net.ryanland.colossus.command.finalizers.Finalizer;
import net.ryanland.colossus.command.impl.DefaultCommand;
import net.ryanland.colossus.command.impl.DefaultDisableCommand;
import net.ryanland.colossus.command.impl.DefaultEnableCommand;
import net.ryanland.colossus.command.impl.DefaultHelpCommand;
import net.ryanland.colossus.command.inhibitors.Inhibitor;
import net.ryanland.colossus.command.inhibitors.impl.CooldownInhibitor;
import net.ryanland.colossus.command.inhibitors.impl.DisabledInhibitor;
import net.ryanland.colossus.command.inhibitors.impl.GuildOnlyInhibitor;
import net.ryanland.colossus.command.inhibitors.impl.PermissionInhibitor;
import net.ryanland.colossus.events.ButtonClickEvent;
import net.ryanland.colossus.events.InternalEventListener;
import net.ryanland.colossus.events.command.CommandEvent;
import net.ryanland.colossus.sys.file.Config;
import net.ryanland.colossus.sys.file.LocalFile;
import net.ryanland.colossus.sys.file.LocalFileBuilder;
import net.ryanland.colossus.sys.file.LocalFileType;
import net.ryanland.colossus.sys.file.database.DatabaseDriver;
import net.ryanland.colossus.sys.file.database.Provider;
import net.ryanland.colossus.sys.file.database.json.*;
import net.ryanland.colossus.sys.file.database.mongo.MongoDatabaseDriver;
import net.ryanland.colossus.sys.file.database.sql.*;
import net.ryanland.colossus.sys.message.DefaultPresetType;
import net.ryanland.colossus.sys.message.PresetBuilder;
import net.ryanland.colossus.sys.message.PresetType;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * A helper class for making a {@link Colossus} bot ready for startup
 */
public class ColossusBuilder {

    private static final Object[] CORE_EVENTS = new Object[]{
        new InternalEventListener()
    };

    private static final Inhibitor[] CORE_INHIBITORS = new Inhibitor[]{
        new DisabledInhibitor(),
        new PermissionInhibitor(),
        new CooldownInhibitor(),
        new GuildOnlyInhibitor()
    };

    private static final Finalizer[] CORE_FINALIZERS = new Finalizer[]{
        new CooldownFinalizer()
    };

    private static final String[] CORE_CONFIG_ENTRIES = new String[]{
        "token",
        "client_id",
        "prefix",

        "support_guild",
        "test_guild",
        "testing"
    };

    private JDABuilder jdaBuilder;
    private Config config;
    private String configDirectory;
    private final Set<Category> categories = new HashSet<>();
    private final List<Command> commands = new ArrayList<>();
    private final List<ContextCommand<?>> contextCommands = new ArrayList<>();
    private final List<LocalFile> localFiles = new ArrayList<>();
    private final List<String> configEntries = new ArrayList<>(List.of(CORE_CONFIG_ENTRIES));
    private final List<Inhibitor> inhibitors = new ArrayList<>();
    private final List<Finalizer> finalizers = new ArrayList<>();
    private final HashMap<String, Provider<?, ?>> providers = new HashMap<>();

    private boolean disableHelpCommand = false;
    private boolean disableCommandToggleCommands = false;
    private long buttonListenerExpirationTimeAmount = 2;
    private TimeUnit buttonListenerExpirationTimeUnit = TimeUnit.MINUTES;
    private DatabaseDriver databaseDriver = null;
    private PresetType defaultPresetType = DefaultPresetType.DEFAULT;
    private PresetType errorPresetType = DefaultPresetType.ERROR;
    private PresetType successPresetType = DefaultPresetType.SUCCESS;
    private LocalizationFunction localizationFunction = s -> Map.of();

    /**
     * Helper class to build a new instance of {@link Colossus}.<br>
     * @param configDirectory The directory the config.json file should be in, containing the bot token among other things.<br>
     *                        For example, a valid input could be: "src/config".<br>
     *                        This directory should be created manually before running your bot. When running, a config file<br>
     *                        will be automatically generated with empty fields for you to fill in.<br><br>
     *
     *                        <strong>WARNING:</strong> It is recommended to {@code .gitignore} your config.json to prevent
     *                        your bot token getting in hands of the wrong people.
     * @see Colossus
     */
    public ColossusBuilder(String configDirectory) {
        // Get directory and perform checks
        LocalFile.validateDirectoryPath(configDirectory);
        this.configDirectory = configDirectory;

        // Prepare the builder
        buildConfigFile();
        jdaBuilder = JDABuilder.createDefault(config.getToken())
            .addEventListeners(CORE_EVENTS);
    }

    /**
     * Helper class to build a new instance of {@link Colossus}.
     * <br>Note: using this constructor will let the bot ignore your {@code config.json} file.
     * <br>To use the config file instead, take advantage of the
     * {@link ColossusBuilder#ColossusBuilder(String)} constructor.
     * <br>This constructor is mainly intended for quick setup. It is recommended to use a proper config file instead.
     * <br><br>
     * @param token The token of the bot. If you use this constructor, please make sure your bot files are not public,
     *              as this can lead anyone to being able to steal your bot token.<br><br>
     *
     *              Your bot's token can be retrieved by:<br>
     *              - Go to <a href="https://discord.com/developers/applications">Discord Developer Applications</a><br>
     *              - Click the bot you want to use<br>
     *              - Click <strong>Bot</strong> on the left side menu<br>
     *              - Under <i>Token</i>, click {@code Copy}<br>
     *              - Paste it here<br><br>
     *
     * @param clientId The client ID of the bot.<br><br>
     *
     *                 Your bot's client ID can be retrieved by:<br>
     *                 - Go to <a href="https://discord.com/developers/applications">Discord Developer Applications</a><br>
     *                 - Click the bot you want to use<br>
     *                 - Under <i>Application ID</i>, click {@code Copy}<br>
     *                 - Paste it here<br><br>
     *
     * @param prefix The prefix of the bot, used for message commands.
     *               In addition to this prefix, the bot will also listen for mentions.
     * @param testGuild The ID of the Discord server you are testing your bot in.<br><br>
     *
     *                  Your server's ID can be retrieved by:<br>
     *                  - Open Discord<br>
     *                  - Enable Developer Mode (User settings > Advanced > Developer Mode)<br>
     *                  - Right-click your server and click {@code Copy ID}<br>
     *                  - Paste it here<br>
     * @see Colossus
     */
    public ColossusBuilder(String token, String clientId, String prefix, String testGuild) {
        config = new Config(token, clientId, prefix, "", testGuild, true);
        jdaBuilder = JDABuilder.createDefault(config.getToken())
            .addEventListeners(CORE_EVENTS);
    }

    /**
     * Build the {@link Colossus} bot.
     * <br>To initialize the bot, use {@link Colossus#initialize()} after building
     * @return The built {@link Colossus} instance
     * @see Colossus
     */
    public Colossus build() {
        // register default commands
        if (!disableHelpCommand || !disableCommandToggleCommands) {
            List<DefaultCommand> defaultCommands = new ArrayList<>();
            if (!disableHelpCommand) defaultCommands.add(new DefaultHelpCommand());
            if (!disableCommandToggleCommands) defaultCommands.addAll(List.of(new DefaultDisableCommand(), new DefaultEnableCommand()));
            registerCategories(new Category("Default", "These are the default commands provided by Colossus. " +
                "You can optionally disable them in your *ColossusBuilder*. If you want to give them a new category, " +
                "run ```java\nCommandHandler.getCommand(\"COMMAND_NAME\").setCategory(YOUR_CATEGORY);``` " +
                "**after** initializing your bot.",
                "⚠", defaultCommands.toArray(Command[]::new)));
        }

        // add core inhibitors and finalizers
        inhibitors.addAll(0, List.of(CORE_INHIBITORS));
        finalizers.addAll(0, List.of(CORE_FINALIZERS));

        // register core providers
        if (databaseDriver instanceof JsonDatabaseDriver) {
            registerCoreProviders(new JsonGlobalProvider(), new JsonGuildsProvider(), new JsonMembersProvider(), new JsonUsersProvider());
        } else if (databaseDriver instanceof MongoDatabaseDriver) {
            //TODO
        } else if (databaseDriver instanceof SQLDatabaseDriver) {
            registerCoreProviders(new SQLGlobalProvider(), new SQLGlobalProvider.DisabledCommandsProvider(),
                new SQLGuildsProvider(), new SQLMembersProvider(), new SQLUsersProvider(), new SQLUsersProvider.CooldownsProvider());
        }

        buildConfigFile();

        return new Colossus(jdaBuilder, config, categories, commands, contextCommands, localFiles,
            buttonListenerExpirationTimeAmount, buttonListenerExpirationTimeUnit, databaseDriver, providers,
            defaultPresetType, errorPresetType, successPresetType, localizationFunction, inhibitors, finalizers);
    }

    private void registerCoreProviders(Provider<?, ?>... providers) {
        for (Provider<?, ?> provider : providers) {
            if (!this.providers.containsKey(provider.getStockName())) {
                registerProviders(provider);
            }
        }
    }

    /**
     * Register {@link LocalFile}s
     * @param localFiles The file(s) to register
     * @return The builder
     * @see LocalFile
     * @see Colossus#getLocalFile(String)
     */
    public ColossusBuilder registerLocalFiles(LocalFile... localFiles) {
        this.localFiles.addAll(List.of(localFiles));
        return this;
    }

    /**
     * Register categories with commands
     * @param categories The categories to register
     * @return The builder
     * @see Category
     * @see Command
     */
    public ColossusBuilder registerCategories(Category... categories) {
        for (Category category : categories) {
            this.commands.addAll(category.getAllCommands());
            this.categories.add(category);
        }
        return this;
    }

    /**
     * Register context commands
     * @param contextCommands The context commands to register
     * @return The builder
     * @see ContextCommand
     */
    public ColossusBuilder registerContextCommands(ContextCommand<?>... contextCommands) {
        this.contextCommands.addAll(List.of(contextCommands));
        return this;
    }

    /**
     * Disables the default help command, optionally allowing you to create your own. This command is enabled by default.
     * @return The builder
     * @see DefaultHelpCommand
     */
    public ColossusBuilder disableHelpCommand() {
        disableHelpCommand = true;
        return this;
    }

    /**
     * Disables the default disable and enable commands, optionally allowing you to create your own.<br>
     * These commands are enabled by default.
     * <p>Note: A {@link SelfUser} (global) type must be present in the defined {@link DatabaseDriver}.
     * @return The builder
     * @see DefaultDisableCommand
     * @see DefaultEnableCommand
     * @see DisabledCommandHandler
     */
    public ColossusBuilder disableCommandToggleCommands() {
        disableCommandToggleCommands = true;
        return this;
    }

    /**
     * Modify the {@link JDABuilder} implementation used for this bot
     * @param modifier The function to use, with the currently defined {@link JDABuilder} given
     * @return This {@link ColossusBuilder}
     * @see JDABuilder
     */
    public ColossusBuilder setJDABuilder(Function<JDABuilder, JDABuilder> modifier) {
        jdaBuilder = modifier.apply(jdaBuilder);
        return this;
    }

    /**
     * Modify the default amount of time to wait before a button listener should expire.<br>
     * Expiring means removing the buttons from the message and stopping listeners.<br>
     * This value is only used in the {@link ButtonClickEvent#addListener(Long, List, Runnable)} method.<br>
     * By default, this value is set to <strong>2 minutes</strong>.
     * @return This {@link ColossusBuilder}
     * @see ButtonClickEvent
     * @see ButtonClickEvent#addListener(Long, List, Runnable)
     */
    public ColossusBuilder setDefaultComponentListenerExpirationTime(long timeAmount, TimeUnit timeUnit) {
        buttonListenerExpirationTimeAmount = timeAmount;
        buttonListenerExpirationTimeUnit = timeUnit;
        return this;
    }

    /**
     * Sets the {@link DatabaseDriver} used for this bot.<br>
     * This will affect the way database operations are made.
     * @param driver The driver to set to
     * @return The builder
     * @see DatabaseDriver
     */
    public ColossusBuilder setDatabaseDriver(DatabaseDriver driver) {
        databaseDriver = driver;
        return this;
    }

    /**
     * Register {@link Provider Providers}
     * @param providers The providers to register
     * @return The builder
     * @see Provider
     */
    public ColossusBuilder registerProviders(Provider<?, ?>... providers) {
        for (Provider<?, ?> provider : providers) {
            this.providers.put(provider.getStockName(), provider);
        }
        return this;
    }

    /**
     * Sets the default {@link PresetType} used in {@link PresetBuilder} when no type is specified.
     * <br>When this is not defined, {@link DefaultPresetType#DEFAULT} is used.
     * @param presetType The {@link PresetType} to set to
     * @return The builder
     * @see PresetType
     * @see PresetBuilder
     * @see DefaultPresetType
     */
    public ColossusBuilder setDefaultPresetType(PresetType presetType) {
        defaultPresetType = presetType;
        return this;
    }

    /**
     * Sets the default {@link PresetType} used when there is an internal error,
     * e.g. a command being on cooldown, {@link MalformedArgumentException}, {@link CommandException}, etc.
     * <br>When this is not defined, {@link DefaultPresetType#ERROR} is used.
     * @param presetType The {@link PresetType} to set to
     * @return The builder
     * @see PresetType
     * @see PresetBuilder
     * @see DefaultPresetType
     * @see CommandException
     */
    public ColossusBuilder setErrorPresetType(PresetType presetType) {
        errorPresetType = presetType;
        return this;
    }

    /**
     * Sets the default {@link PresetType} used when there was a successful action.
     * <br>When this is not defined, {@link DefaultPresetType#SUCCESS} is used.
     * @param presetType The {@link PresetType} to set to
     * @return The builder
     * @see PresetType
     * @see PresetBuilder
     * @see DefaultPresetType
     */
    public ColossusBuilder setSuccessPresetType(PresetType presetType) {
        successPresetType = presetType;
        return this;
    }

    /**
     * Sets the {@link LocalizationFunction} to use for slash and context commands, and optionally custom values.
     * <br>By default, no function is used.
     * @param localizationFunction The {@link LocalizationFunction} to set to
     * @return The builder
     * @see Colossus#getLocalization(DiscordLocale, String)
     * @see CommandEvent#getLocalization(String)
     * @see LocalizationFunction
     * @see ResourceBundleLocalizationFunction
     */
    public ColossusBuilder setLocalizationFunction(LocalizationFunction localizationFunction) {
        this.localizationFunction = localizationFunction;
        return this;
    }

    /**
     * Register {@link Inhibitor Inhibitors}
     * <br>Core inhibitors will be executed before custom ones. These are defined in {@code ColossusBuilder.CORE_INHIBITORS}
     * @param inhibitors The inhibitors to register
     * @return The builder
     * @see Inhibitor
     */
    public ColossusBuilder registerInhibitors(Inhibitor... inhibitors) {
        this.inhibitors.addAll(List.of(inhibitors));
        return this;
    }

    /**
     * Register {@link Finalizer Finalizers}
     * <br>Core finalizers will be executed before custom ones. These are defined in {@code ColossusBuilder.CORE_FINALIZERS}
     * @param finalizers The finalizers to register
     * @return The builder
     * @see Finalizer
     */
    public ColossusBuilder registerFinalizers(Finalizer... finalizers) {
        this.finalizers.addAll(List.of(finalizers));
        return this;
    }

    /**
     * Register custom entries that will appear in the {@code config.json} file.<br>
     * These can be retrieved later using the {@link Config} class.
     * @param keys The keys to register
     * @return The builder
     * @see Config
     * @see Config#get(String)
     * @see Config#getString(String)
     * @see Config#getInt(String)
     * @see Config#getBoolean(String)
     */
    public ColossusBuilder registerConfigEntries(String... keys) {
        configEntries.addAll(List.of(keys));
        return this;
    }

    /**
     * Disable the specified {@link GatewayIntent GatewayIntents}.
     * <br>This will not enable any currently unset intents.
     *
     * <p>If you disable certain intents you also have to disable related {@link CacheFlag CacheFlags}.
     * This can be achieved using {@link #disableCache(CacheFlag...)}. The required intents for each
     * flag are documented in the {@link CacheFlag} enum.
     *
     * @param intents The intents to disable
     * @return The builder
     * @see #enableIntents(GatewayIntent...)
     */
    public ColossusBuilder disableIntents(GatewayIntent... intents) {
        return setJDABuilder(builder -> builder.disableIntents(List.of(intents)));
    }

    /**
     * Enable the specified {@link GatewayIntent GatewayIntents}.
     * <br>This will not disable any currently set intents.
     * @param intents The intents to enable
     * @return The builder
     * @see #disableIntents(GatewayIntent...)
     */
    public ColossusBuilder enableIntents(GatewayIntent... intents) {
        return setJDABuilder(builder -> builder.enableIntents(List.of(intents)));
    }

    /**
     * Disable specific cache flags.
     * <br>This will not enable any currently unset cache flagss.
     * @param flags The {@link CacheFlag CacheFlags} to disable
     * @return The builder
     * @see #enableCache(CacheFlag...)
     */
    public ColossusBuilder disableCache(CacheFlag... flags) {
        return setJDABuilder(builder -> builder.disableCache(List.of(flags)));
    }

    /**
     * Enable specific cache flags.
     * <br>This will not disable any currently set cache flags.
     * @param flags The {@link CacheFlag CacheFlags} to enable
     * @return The builder
     * @see #disableCache(CacheFlag...)
     */
    public ColossusBuilder enableCache(CacheFlag... flags) {
        return setJDABuilder(builder -> builder.enableCache(List.of(flags)));
    }

    /**
     * Adds all provided listeners to the list of listeners that will be used to populate the {@link JDA} object.
     * <br>This uses the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventListener} by default.
     * <br>To switch to the {@link net.dv8tion.jda.api.hooks.AnnotatedEventManager AnnotatedEventManager},
     * use {@link JDABuilder#setEventManager(net.dv8tion.jda.api.hooks.IEventManager) setEventManager(new AnnotatedEventManager())}.
     *
     * <p><b>Note:</b> When using the {@link net.dv8tion.jda.api.hooks.InterfacedEventManager InterfacedEventListener} (default),
     * given listener(s) <b>must</b> be instance of {@link net.dv8tion.jda.api.hooks.EventListener EventListener}!
     *
     * @param listeners The listener(s) to add to the list.
     * @return The builder
     */
    public ColossusBuilder addEventListeners(Object... listeners) {
        return setJDABuilder(builder -> builder.addEventListeners(listeners));
    }

    private void buildConfigFile() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Build the config file
        LocalFile configFile = new LocalFileBuilder()
            .setName(configDirectory + "/config")
            .setFileType(LocalFileType.JSON)
            .setDefaultContent(gson.toJson(LocalFile.jsonOfKeys(configEntries.toArray(new String[0]))))
            .buildFile();

        // Parse the config file's JSON
        JsonObject configJson = configFile.parseJson();

        // Check for missing config keys, and if they are found, add them and modify the file
        boolean changed = false;
        for (String key : configEntries) {
            if (!configJson.has(key)) {
                configJson.addProperty(key, "");
                changed = true;
            }
        }
        if (changed) configFile.write(gson.toJson(configJson));

        // Create a new Config and set it
        config = new Config(configJson);
    }

    /**
     * Returns the {@link Config} in its current state.<br>
     * This method should only be used before the bot is initialized.
     * @see Colossus#getConfig()
     */
    public Config getConfig() {
        return config;
    }

}
