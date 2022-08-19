package cf.wangyu1745.sync.command;

import lombok.RequiredArgsConstructor;
import lombok.var;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServerName implements CommandExecutor {
    private final FileConfiguration config;
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("servername") && sender instanceof Player) {
            var p = (Player) sender;
            p.sendMessage(String.valueOf(config.getString("id")));
        }
        return true;
    }
}
