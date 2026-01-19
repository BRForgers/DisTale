package one.armelin.distale;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import one.armelin.distale.commands.ShrugCommand;
import one.armelin.distale.listeners.DeathEventListener;
import one.armelin.distale.listeners.DiscordEventListener;
import one.armelin.distale.listeners.HytaleEventListener;
import one.armelin.distale.utils.ExpiringMap;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DisTale - Discord bridge for Hytale servers
 * Ported from DisFabric/DisForge by armelin1
 */
public class DisTale extends JavaPlugin {

    public static final String NAME = "DisTale";
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static Configuration config;
    public static JDA jda;
    public static MessageChannel textChannel;
    public static String webhookId = "";
    public static boolean stop = false;
    public static Universe universe;
    public static CommandManager commandManager;

    public static Map<String, BufferedImage> playerSkins = new HashMap<>();
    public static ExpiringMap<String, String> skinsUrls = new ExpiringMap<>();

    public static OkHttpClient webhookClient = new OkHttpClient.Builder()
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .build();

    private static DisTale instance;

    public DisTale(JavaPluginInit init) {
        super(init);
        instance = this;

        LOGGER.atInfo().log("DisTale v%s initializing...", this.getManifest().getVersion());
        System.setProperty("net.dv8tion.jda.disableFallbackLogger", "true");

        // Load configuration (using plugins directory)
        if(!Files.exists(getDataDirectory())){
            try {
                Files.createDirectories(getDataDirectory());
            } catch (Exception e) {
                LOGGER.atSevere().withCause(e).log("Failed to create plugin data directory");
            }
        }
        config = Configuration.getConfig(getDataDirectory());

        LOGGER.atInfo().log("DisTale initialized successfully!");
    }

    @Override
    protected void setup() {
        super.setup();
        // Initialize Discord bot
        initializeDiscord();
        HytaleEventListener.register(this, getEventRegistry());
        getEntityStoreRegistry().registerSystem(new DeathEventListener());
        getCommandRegistry().registerCommand(new ShrugCommand());
    }

    @Override
    protected void start() {
        super.start();
        if (jda != null) {
            if (!config.botGameStatus.isEmpty())
                jda.getPresence().setActivity(Activity.playing(config.botGameStatus));
            textChannel.sendMessage(config.texts.serverStarted).queue();
        }
        universe = Universe.get();
        commandManager = CommandManager.get();
    }

    @Override
    protected void shutdown() {
        super.shutdown();
        onDisable();
        skinsUrls.shutdown();
    }


    private void initializeDiscord() {
        LOGGER.atInfo().log("Connecting to Discord...");

        try {
            JDABuilder jdaBuilder = JDABuilder
                    .createDefault(config.botToken)
                    .setHttpClient(new OkHttpClient.Builder()
                            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                            .build())
                    .addEventListeners(new DiscordEventListener())
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT);

            if (config.membersIntents) {
                jdaBuilder.enableIntents(GatewayIntent.GUILD_MEMBERS)
                         .setMemberCachePolicy(MemberCachePolicy.ALL);
            }

            DisTale.jda = jdaBuilder.build();
            DisTale.jda.awaitReady();
            DisTale.textChannel = DisTale.jda.getChannelById(MessageChannel.class, config.channelId);

            if (DisTale.textChannel == null) {
                LOGGER.atSevere().log("Could not find Discord channel with ID: %s", config.channelId);
                return;
            }

            LOGGER.atInfo().log("Successfully connected to Discord!");

        } catch (InvalidTokenException ex) {
            jda = null;
            LOGGER.atSevere().withCause(ex).log("Invalid Discord bot token!");
            return;
        } catch (InterruptedException ex) {
            jda = null;
            LOGGER.atSevere().withCause(ex).log("Failed to connect to Discord");
            return;
        } catch (Exception ex) {
            jda = null;
            LOGGER.atSevere().withCause(ex).log("An error occurred while connecting to Discord");
            return;
        }

        // Extract webhook ID if configured
        if (!config.webhookURL.isEmpty()) {
            Matcher webhookMatcher = Pattern
                    .compile("https://[a-z]*\\.?(?:discord|discordapp)\\.com/api/webhooks/(?<id>[0-9]+)/[a-zA-Z0-9_-]+")
                    .matcher(config.webhookURL);
            if (webhookMatcher.matches()) {
                webhookId = webhookMatcher.group("id");
            } else {
                LOGGER.atSevere().log("Invalid webhook URL format");
            }
        }
    }

    public void onDisable() {
        LOGGER.atInfo().log("DisTale shutting down - Disconnecting from Discord...");

        if (jda != null) {
            stop = true;

            // Send server stopped message
            try {
                textChannel.sendMessage(config.texts.serverStopped).queue();
                Thread.sleep(250);
            } catch (InterruptedException e) {
                LOGGER.atSevere().withCause(e).log("Error sending shutdown message");
            }

            // Cleanup webhook client
            webhookClient.dispatcher().executorService().shutdown();
            webhookClient.connectionPool().evictAll();

            // Shutdown JDA
            jda.shutdown();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                LOGGER.atSevere().withCause(e).log("Error during JDA shutdown");
            }
        }

        LOGGER.atInfo().log("DisTale shut down successfully!");
    }

    public static DisTale getInstance() {
        return instance;
    }
}
