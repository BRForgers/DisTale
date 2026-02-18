package one.armelin.distale.utils;

import com.hypixel.hytale.logger.backend.HytaleLoggerBackend;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleModule;
import com.hypixel.hytale.server.core.util.MessageUtil;
import net.dv8tion.jda.api.utils.FileUpload;
import one.armelin.distale.DisTale;
import org.jetbrains.annotations.NotNull;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

import java.nio.charset.StandardCharsets;
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
        if(attributedString.length() > 1900){
            DisTale.textChannel.sendMessage("> Result too long").setFiles(FileUpload.fromData(attributedString.toString().getBytes(StandardCharsets.UTF_8), "result.txt")).queue();
        }else {
            DisTale.textChannel.sendMessage("> ```\n" + attributedString.toString().stripTrailing().replaceAll("(?m)^", "> ") + "\n> ```").queue();
        }
    }
}
