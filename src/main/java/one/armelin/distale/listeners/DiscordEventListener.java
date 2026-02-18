package one.armelin.distale.listeners;

import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.commands.world.perf.WorldPerfCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import one.armelin.distale.DisTale;
import one.armelin.distale.utils.DiscordCommandSender;
import one.armelin.distale.utils.TinyMsg;
import one.armelin.distale.utils.markdown.MarkdownConverter;
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
        if(DisTale.universe == null){
            event.getChannel().sendMessage("Universe is not ready.").queue();
        }
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
        if(DisTale.universe == null){
            event.getChannel().sendMessage("Universe is not ready.").queue();
        }
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
                !tps            - Show server TPS
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

        Color userColor = Objects.requireNonNullElse(
                event.getMember() != null ? event.getMember().getColor() : null,
                Color.WHITE
        );

        String messageContent = event.getMessage().getContentDisplay();

        // Add attachment/embed indicators
        if (!event.getMessage().getAttachments().isEmpty()) {
            messageContent += " <att>";
        }
        if (!event.getMessage().getEmbeds().isEmpty()) {
            messageContent += " <embed>";
        }

        // Sanitize message content
        messageContent = Utils.sanitize(messageContent, false);
        String markDownContent = MarkdownConverter.convert(messageContent).trim();

        String coloredMessage = "<color:" + String.format("#%06x", userColor.getRGB() & 0x00FFFFFF) + ">" + DisTale.config.texts.coloredText
                .replace("%discordname%", discordName).replace("%message%", markDownContent) + "</color>";

        String colorlessMessage = DisTale.config.texts.colorlessText
                .replace("%discordname%", discordName + (event.getAuthor().isBot() ? "[BOT]" : ""))
                .replace("%message%", markDownContent);

        // Handle replied messages
        String replyMessage = "";
        if (event.getMessage().getReferencedMessage() != null) {
            String replyUser;
            if (event.getMessage().getReferencedMessage().getMember() != null) {
                replyUser = event.getMessage().getReferencedMessage().getMember().getEffectiveName();
            } else {
                replyUser = event.getMessage().getReferencedMessage().getAuthor().getName();
            }

            String replyText = DisTale.config.texts.replyText
                    .replace("%discordname%", Utils.sanitize(replyUser, true));

            replyMessage += "<color:gray>" + replyText + "</color>";
        }

        String finalMessage =  coloredMessage + replyMessage + colorlessMessage;

        if(DisTale.universe == null){
            return;
        }

        DisTale.universe.sendMessage(TinyMsg.parse(finalMessage));
        DisTale.LOGGER.getSubLogger("Discord -> Hytale").atInfo().log("%s: %s", discordName, messageContent);
    }
}
