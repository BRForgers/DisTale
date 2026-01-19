package one.armelin.distale.commands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import one.armelin.distale.DisTale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static one.armelin.distale.listeners.HytaleEventListener.sendWebhookMessage;

/**
 * Shrug command for DisTale
 * Sends "¯\_(ツ)_/¯" in chat when player uses /shrug
 */
public class ShrugCommand extends AbstractCommand {

    private static final String SHRUG = "¯\\_(ツ)_/¯";

    public ShrugCommand() {
        super("shrug", "send a shrug emoticon in chat");
        this.setAllowsExtraArguments(true);
        this.setPermissionGroup(GameMode.Adventure);
    }


    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@NotNull CommandContext context) {
        String rawArgs = context.getInputString().replace("shrug", "").trim();
        String message = SHRUG + (rawArgs.isEmpty() ? "" : " " + rawArgs);
        Message result = Message.translation("server.chat.broadcastMessage").param("username", context.sender().getDisplayName()).param("message", message);

        DisTale.universe.sendMessage(result);
        ConsoleSender.INSTANCE.sendMessage(result);

        String playerName = context.sender().getDisplayName();

        if (DisTale.config.isWebhookEnabled && !DisTale.webhookId.isEmpty()) {
            sendWebhookMessage(playerName, message);
        } else {
            String formattedMessage = DisTale.config.texts.playerMessage
                    .replace("%playername%", MarkdownSanitizer.escape(playerName))
                    .replace("%playermessage%", message);

            DisTale.textChannel.sendMessage(formattedMessage).queue();
        }
        return CompletableFuture.completedFuture(null);
    }
}
