package one.armelin.distale.commands;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.MessageUtil;
import one.armelin.distale.DisTale;
import one.armelin.distale.utils.markdown.MarkdownConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

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
        String message = (rawArgs.isEmpty() ? "" : rawArgs + " ") + SHRUG;

        if(context.sender() instanceof Player) {
            PlayerRef playerRef = DisTale.universe.getPlayer(context.sender().getUuid());

            ArrayList<PlayerRef> targets = new java.util.ArrayList<>(DisTale.universe.getPlayers().stream().toList());
            targets.removeIf(targetPlayerRef -> targetPlayerRef.getHiddenPlayersManager().isPlayerHidden(context.sender().getUuid()));

            HytaleServer.get().getEventBus().dispatchForAsync(PlayerChatEvent.class).dispatch(
                    new PlayerChatEvent(playerRef, targets, message)
            );
        }else{
            Message result = Message.translation("server.chat.broadcastMessage")
                    .param("username", context.sender().getDisplayName())
                    .param("message", MarkdownConverter.toMessage(message));
            DisTale.universe.sendMessage(result);
            ConsoleSender.INSTANCE.sendMessage(Message.raw(MessageUtil.toAnsiString(result).toAnsi()));
        }
        return CompletableFuture.completedFuture(null);
    }
}
