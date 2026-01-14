package one.armelin.distale.listeners;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import one.armelin.distale.DisTale;
import one.armelin.distale.utils.DiscordCommandSender;
import one.armelin.distale.utils.MarkdownParser;
import one.armelin.distale.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;

/**
 * Discord event listener for DisTale
 * Handles messages from Discord and processes commands
 */
public class DiscordEventListener extends ListenerAdapter {

    DiscordCommandSender discordCommandSender = new DiscordCommandSender();

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // Ignore messages from wrong channel
        if (event.getChannel().getId().equals(DisTale.config.channelId) == false) {
            return;
        }

        // Ignore messages from self
        if (event.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
            return;
        }

        // Ignore webhook messages
        if (event.getAuthor().getId().equals(DisTale.webhookId)) {
            return;
        }

        // Ignore bot messages if configured
        if (event.getAuthor().isBot() && !DisTale.config.allowBotMessages) {
            return;
        }

        String message = event.getMessage().getContentRaw();

        // Handle commands
        if (message.startsWith("!console") && isAdmin(event.getAuthor().getId())) {
            handleConsoleCommand(event);
        } else if (message.startsWith("!online")) {
            handleOnlineCommand(event);
//        } else if (message.startsWith("!tps")) {
//            handleTpsCommand(event);
        } else if (message.startsWith("!help")) {
            handleHelpCommand(event);
        } else {
            // Regular chat message
            handleChatMessage(event);
        }
    }

    /**
     * Check if user is admin
     */
    private boolean isAdmin(String userId) {
        return Arrays.asList(DisTale.config.adminsIds).contains(userId);
    }

    /**
     * Handle !console command - Execute server command
     */
    private void handleConsoleCommand(MessageReceivedEvent event) {
        String command = event.getMessage().getContentRaw().replace("!console ", "");

        DisTale.commandManager.handleCommand(discordCommandSender, command);

        DisTale.LOGGER.atInfo().log("Admin " + event.getAuthor().getEffectiveName() + " executed command via Discord: " + command);
    }

    /**
     * Handle !online command - List online players
     */
    private void handleOnlineCommand(MessageReceivedEvent event) {
        java.util.List<PlayerRef> players = DisTale.universe.getPlayers();

        StringBuilder response = new StringBuilder("```\n");
        response.append("=============== Online Players (" + players.size() + ") ===============\n");
        DisTale.universe.getPlayers().forEach(player -> {
            response.append(player.getUsername()).append("\n");
        });
        response.append("```");

        event.getChannel().sendMessage(response.toString()).queue();
    }

    /**
     * Handle !tps command - Show server TPS
     */
    private void handleTpsCommand(MessageReceivedEvent event) {
        // TODO: Implement with actual Hytale server API
        String response = "Server TPS: 20.0";
        event.getChannel().sendMessage(response).queue();
    }

    /**
     * Handle !help command - Show available commands
     */
    private void handleHelpCommand(MessageReceivedEvent event) {
        String help = """
                ```
                =============== DisTale Commands ===============

                !online         - List online players
                !tps            - Show server TPS (not implemented)
                !console <cmd>  - Execute server command (admins only)
                !help           - Show this help message
                ```""";

        event.getChannel().sendMessage(help).queue();
    }

    /**
     * Handle regular chat message - Send to Hytale chat
     */
    private void handleChatMessage(MessageReceivedEvent event) {
        // This will need to broadcast a message to all players

        String discordName = event.getMember() != null ?
                event.getMember().getEffectiveName() :
                event.getAuthor().getName();

        Color color = Objects.requireNonNullElse(Objects.requireNonNull(event.getMember()).getColor(), Color.WHITE);

        String messageContent = event.getMessage().getContentDisplay();

        // Add attachment/embed indicators
        if (!event.getMessage().getAttachments().isEmpty()) {
            messageContent += " <att>";
        }
        if (!event.getMessage().getEmbeds().isEmpty()) {
            messageContent += " <embed>";
        }

        // Sanitize and parse markdown
        messageContent = Utils.sanitize(messageContent, false);
        messageContent = MarkdownParser.parseMarkdown(messageContent);

        // Apply text templates
        String coloredPart = DisTale.config.texts.coloredText
                .replace("%discordname%", Utils.sanitize(discordName, true))
                .replace("%message%", messageContent);
        Message coloredMessage = Message.raw(coloredPart).color(color);

        String colorlessPart = DisTale.config.texts.colorlessText
                .replace("%discordname%", Utils.sanitize(discordName, true) + (event.getAuthor().isBot() ? "[BOT]" : ""))
                .replace("%message%", messageContent);
        Message colorlessMessage = Message.raw(colorlessPart);

        // Handle replied messages
        String replyPart = "";
        if (event.getMessage().getReferencedMessage() != null) {
            String replyUser;
            if (event.getMessage().getReferencedMessage().getMember() != null) {
                replyUser = event.getMessage().getReferencedMessage().getMember().getEffectiveName();
            } else {
                replyUser = event.getMessage().getReferencedMessage().getAuthor().getName();
            }

            replyPart = DisTale.config.texts.replyText
                    .replace("%discordname%", Utils.sanitize(replyUser, true));
        }
        Message replyMessage = Message.raw(replyPart).color(Color.GRAY);

        Message finalMessage = Message.join(
                coloredMessage,
                replyMessage,
                colorlessMessage
        );

        DisTale.LOGGER.atInfo().log("[Discord -> Hytale] " + discordName + ": " + messageContent);

        DisTale.universe.sendMessage(finalMessage);
    }
}
