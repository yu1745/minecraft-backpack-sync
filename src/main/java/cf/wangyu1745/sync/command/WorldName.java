package cf.wangyu1745.sync.command;

import lombok.var;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.springframework.stereotype.Component;

@Component
public class WorldName implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("worldname") && sender instanceof Player) {
            var p = (Player) sender;
            p.sendMessage(String.valueOf(p.getLocation().getWorld().getName()));
        }
        return true;
    }
}
