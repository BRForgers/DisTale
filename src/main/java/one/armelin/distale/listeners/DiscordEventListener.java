package one.armelin.distale.listeners;

import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.commands.world.perf.WorldPerfCommand;
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
        if (!event.getChannel().getId().equals(DisTale.config.channelId)) {
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
        } else if (message.startsWith("!tps")) {
            handleTpsCommand(event);
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

        DisTale.LOGGER.atInfo().log("Admin %s executed command via Discord: %s", event.getAuthor().getName(), command);
    }

    /**
     * Handle !online command - List online players
     */
    private void handleOnlineCommand(MessageReceivedEvent event) {
        java.util.List<PlayerRef> players = DisTale.universe.getPlayers();

        StringBuilder response = new StringBuilder("```\n");
        response.append("=============== Online Players (").append(players.size()).append(") ===============\n");
        DisTale.universe.getPlayers().forEach(player -> response.append(player.getUsername()).append("\n"));
        response.append("```");

        event.getChannel().sendMessage(response.toString()).queue();
    }

    /**
     * Handle !tps command - Show server TPS
     */
    private void handleTpsCommand(MessageReceivedEvent event) {
        StringBuilder response = new StringBuilder("```\n");
        response.append("=============== Server TPS ===============\n");
        DisTale.universe.getWorlds().forEach( (name, world) -> {
            int targetTps = world.getTps();
            int tickStepNanos = world.getTickStepNanos();
            HistoricMetric metrics = world.getBufferedTickLengthMetricSet();
            double tps = WorldPerfCommand.tpsFromDelta(metrics.getLastValue(), tickStepNanos);
            response.append(String.format("World '%s': %.2f TPS (Target: %s)\n", name, tps, targetTps));
        });
        response.append("```");
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
                event. getAuthor().getName();

        Color userColor = Objects.requireNonNullElse(
                event.getMember() != null ? event.getMember().getColor() : null,
                Color. WHITE
        );

        String messageContent = event.getMessage().getContentDisplay();

        // Add attachment/embed indicators
        if (!event. getMessage().getAttachments().isEmpty()) {
            messageContent += " <att>";
        }
        if (!event.getMessage().getEmbeds().isEmpty()) {
            messageContent += " <embed>";
        }

        // Sanitize message content
        messageContent = Utils.sanitize(messageContent, false);

        // Build the colored part (can contain full message)
        String coloredTemplate = DisTale. config.texts.coloredText
                .replace("%discordname%", discordName);

        Message coloredMessage;
        if (coloredTemplate.contains("%message%")) {
            String[] coloredParts = coloredTemplate.split("%message%", -1);
            coloredMessage = Message.raw("");

            if (!coloredParts[0]. isEmpty()) {
                coloredMessage.insert(Message.raw(coloredParts[0]).color(userColor));
            }

            // Parse markdown WITH user color
            Message parsedMarkdownColored = MarkdownParser.parseMarkdown(messageContent, userColor);
            coloredMessage.insert(parsedMarkdownColored);

            if (coloredParts.length > 1 && !coloredParts[1].isEmpty()) {
                coloredMessage.insert(Message.raw(coloredParts[1]).color(userColor));
            }
        } else {
            coloredMessage = Message. raw(coloredTemplate).color(userColor);
        }

        // Build the colorless part (can contain full message)
        String colorlessTemplate = DisTale.config.texts.colorlessText
                .replace("%discordname%", discordName + (event.getAuthor().isBot() ? "[BOT]" : ""));

        Message colorlessMessage;
        if (colorlessTemplate. contains("%message%")) {
            String[] colorlessParts = colorlessTemplate.split("%message%", -1);
            colorlessMessage = Message.raw("");

            if (!colorlessParts[0].isEmpty()) {
                colorlessMessage.insert(Message.raw(colorlessParts[0]));
            }

            // Parse markdown WITHOUT color (null = no default color)
            Message parsedMarkdownColorless = MarkdownParser.parseMarkdown(messageContent, null);
            colorlessMessage. insert(parsedMarkdownColorless);

            if (colorlessParts.length > 1 && !colorlessParts[1].isEmpty()) {
                colorlessMessage.insert(Message.raw(colorlessParts[1]));
            }
        } else {
            colorlessMessage = Message.raw(colorlessTemplate);
        }

        // Handle replied messages
        Message replyMessage = Message.empty();
        if (event.getMessage().getReferencedMessage() != null) {
            String replyUser;
            if (event.getMessage().getReferencedMessage().getMember() != null) {
                replyUser = event.getMessage().getReferencedMessage().getMember().getEffectiveName();
            } else {
                replyUser = event.getMessage().getReferencedMessage().getAuthor().getName();
            }

            String replyText = DisTale.config.texts.replyText
                    .replace("%discordname%", Utils.sanitize(replyUser, true));

            replyMessage = Message.raw(replyText).color(Color.GRAY);
        }

        // Combine all parts
        Message finalMessage = Message.empty()
                .insert(coloredMessage)
                .insert(replyMessage)
                .insert(colorlessMessage);

        DisTale.LOGGER.atInfo().log("[Discord -> Hytale] %s: %s", discordName ,messageContent);

        DisTale.universe.sendMessage(finalMessage);
    }
}
