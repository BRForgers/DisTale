package one.armelin.distale;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import blue.endless.jankson.api.SyntaxError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration class for DisTale plugin
 * Manages all configurable options and text templates
 */
public class Configuration {

    @Comment(value = "Sets if DisTale should modify in-game chat messages")
    public boolean modifyChatMessages = true;

    @Comment(value = "Bot Token; see https://discordpy.readthedocs.io/en/latest/discord.html")
    public String botToken = "";

    @Comment(value = "Bot Game Status; What will be displayed on the bot's game status (leave empty for nothing)")
    public String botGameStatus = "";

    @Comment(value = "Enable Webhook; If enabled, player messages will be sent using a webhook with the player's name and head, instead of a regular message.")
    public boolean isWebhookEnabled = true;

    @Comment(value = "Webhook URL; see https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks")
    public String webhookURL = "";

    @Comment(value = "Use UUID instead of nickname to request player head on webhook")
    public Boolean useUUIDInsteadNickname = true;

    @Comment(value = "Sets if DisTale should send Bot messages to Hytale")
    public boolean allowBotMessages = false;

    @Comment(value = """
            Admin IDs in Discord; see https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-
            If more than one, enclose each ID in quotation marks separated by commas, like this:
            "adminsIds": [\s
            \t\t"000",
            \t\t"111",
            \t\t"222"
            \t]""")
    public String[] adminsIds = {""};

    @Comment(value = "Channel ID in Discord")
    public String channelId = "";

    @Comment(value = "If you enabled \"Server Members Intent\" in the bot's config page, change it to true. (This is only necessary if you want to enable Discord mentions inside the game)")
    public boolean membersIntents = false;

    @Comment(value = "Should announce when players join/leave the server?")
    public boolean announcePlayers = true;

    @Comment(value = "Should announce when players get an advancement?")
    public boolean announceAdvancements = true;

    @Comment(value = "Should announce when a player dies?")
    public boolean announceDeaths = true;

    @Comment(value = "Should announce when server crashes?")
    public boolean announceCrashes = true;

    public Texts texts = new Texts();

    public static class Texts {

        @Comment(value = """
                Hytale -> Discord
                Player chat message (Only used when Webhook is disabled)
                Available placeholders:
                %playername% | Player name
                %playermessage% | Player message""")
        public String playerMessage = "**%playername%:** %playermessage%";

        @Comment(value = "Hytale -> Discord\nServer started message")
        public String serverStarted = "**Server started!**";

        @Comment(value = "Hytale -> Discord\nServer stopped message")
        public String serverStopped = "**Server stopped!**";

        @Comment(value = """
                Hytale -> Discord
                Join server
                Available placeholders:
                %playername% | Player name""")
        public String joinServer = "**%playername% joined the game**";

        @Comment(value = """
                Hytale -> Discord
                Left server
                Available placeholders:
                %playername% | Player name""")
        public String leftServer = "**%playername% left the game**";

        @Comment(value = """
                Hytale -> Discord
                Death message
                Available placeholders:
                %playername% | Player name
                %deathmessage% | Death message""")
        public String deathMessage = "**%playername%** was killed by **%deathmessage%**";

        @Comment(value = """
                Hytale -> Discord
                Advancement/Achievement message  (Not implemented yet)
                Available placeholders:
                %playername% | Player name
                %achievement% | Achievement name""")
        public String achievement = "%playername% has achieved **[%achievement%]**";

        @Comment(value = """
                Discord -> Hytale
                Colored part of the message, this part will receive the same color as the role in Discord
                Available placeholders:
                %discordname% | User nickname in the guild
                %message% | The message""")
        public String coloredText = "[Discord] ";

        @Comment(value = """
                Discord -> Hytale
                Colorless (white) part of the message
                Available placeholders:
                %discordname% | Nickname of the user in the guild
                %message% | The message""")
        public String colorlessText = "<%discordname%> %message%";

        @Comment(value = """
                Discord -> Hytale
                Replied message text, with gray color, goes before the colorless text, after colored text
                Available placeholders:
                %discordname% | Nickname of the replied user in the guild""")
        public String replyText = "-> %discordname%\n";

        @Comment(value = "Removes line breaks from any Discord message to avoid spam")
        public Boolean removeLineBreakFromDiscord = false;

        @Comment(value = """
                Hytale -> Discord
                Crash message (Not implemented yet)
                Available placeholders:
                %crashdescription% | Crash description""")
        public String crashMessage = "**Server crashed:** %crashdescription%";
    }

    /**
     * Load configuration from file or create default if it doesn't exist
     * @return Loaded or default configuration
     */
    public static Configuration getConfig(Path configPath) {
        var jankson = Jankson.builder().build();
        Configuration config;

        try {
            Path configFile = configPath.resolve("distale.json5");

            // Try to load existing config
            if (Files.exists(configFile)) {
                JsonObject configJson = jankson.load(configFile.toFile());
                config = jankson.fromJson(configJson, Configuration.class);
                DisTale.LOGGER.atInfo().log("Configuration loaded from: %s", configFile);
                DisTale.LOGGER.atInfo().log("modifyChatMessages: %s", config.modifyChatMessages);
                DisTale.LOGGER.atInfo().log("botToken: %s", (config.botToken == null || config.botToken.isEmpty()) ? "<empty>" : "<redacted>");
                DisTale.LOGGER.atInfo().log("isWebhookEnabled: %s", config.isWebhookEnabled);
                DisTale.LOGGER.atInfo().log("webhookURL: %s", (config.webhookURL == null || config.webhookURL.isEmpty()) ? "<empty>" : "<redacted>");
                DisTale.LOGGER.atInfo().log("allowBotMessages: %s", config.allowBotMessages);
                DisTale.LOGGER.atInfo().log("channelId: %s", (config.channelId == null || config.channelId.isEmpty()) ? "<empty>" : "<redacted>");
                DisTale.LOGGER.atInfo().log("membersIntents: %s", config.membersIntents);
                DisTale.LOGGER.atInfo().log("announcePlayers: %s", config.announcePlayers);
                DisTale.LOGGER.atInfo().log("announceDeaths: %s", config.announceDeaths);
            } else {
                config = new Configuration();
                DisTale.LOGGER.atInfo().log("Creating default configuration at: %s", configFile);
                DisTale.LOGGER.atWarning().log("Please configure the plugin before using it! Don't report issues without configuring first.");
            }

            // Save/update config file
            Files.writeString(configFile, jankson.toJson(config).toJson(true, true));

        } catch (IOException | SyntaxError e) {
            DisTale.LOGGER.atSevere().withCause(e).log("Failed to load configuration, using defaults");
            DisTale.LOGGER.atWarning().log("Please configure the plugin before using it! Don't report issues without configuring first.");
            config = new Configuration();
        }

        return config;
    }
}
