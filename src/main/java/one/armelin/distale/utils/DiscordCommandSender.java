package one.armelin.distale.utils;

import com.hypixel.hytale.logger.backend.HytaleLoggerBackend;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleModule;
import com.hypixel.hytale.server.core.util.MessageUtil;
import one.armelin.distale.DisTale;
import org.jetbrains.annotations.NotNull;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

import java.util.UUID;

public class DiscordCommandSender  implements CommandSender {
    @Override
    public String getDisplayName() {
        return "Discord";
    }

    @Override
    public UUID getUuid() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    @Override
    public boolean hasPermission(@NotNull String s) {
        return true;
    }

    @Override
    public boolean hasPermission(@NotNull String s, boolean b) {
        return true;
    }

    @Override
    public void sendMessage(@NotNull Message message) {
        Terminal terminal = ConsoleModule.get().getTerminal();
        AttributedString attributedString = MessageUtil.toAnsiString(message);
        HytaleLoggerBackend.rawLog(attributedString.toAnsi(terminal));
        DisTale.textChannel.sendMessage("> " + attributedString).queue();
    }
}
