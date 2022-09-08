package cf.wangyu1745.sync.command;

import lombok.var;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class XYZ implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("xyz") && sender instanceof Player) {
            var p = (Player) sender;
            Location location = p.getTargetBlock(EnumSet.of(Material.AIR,Material.WATER), 3).getLocation();
            sender.sendMessage("x:" + location.getBlockX() + " y:" + location.getBlockY() + " z:" + location.getBlockZ());
        }
        return true;
    }
}
