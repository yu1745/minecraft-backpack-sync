package cf.wangyu1745.sync.command;

import lombok.RequiredArgsConstructor;
import lombok.var;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component(XYZInfo.XYZ)
@RequiredArgsConstructor
public class XYZInfo implements CommandExecutor {
    public static final String XYZ = "xyz";
    private final FileConfiguration config;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("xyz") && sender instanceof Player) {
            var p = (Player) sender;
            Location location = p.getTargetBlock(EnumSet.of(Material.AIR, Material.WATER), 3).getLocation();
            p.sendMessage("服务器:" + config.getString("id"));
            p.sendMessage("世界:" + p.getLocation().getWorld().getName());
            sender.sendMessage("x:" + location.getBlockX() + " y:" + location.getBlockY() + " z:" + location.getBlockZ());
        }
        return true;
    }
}
