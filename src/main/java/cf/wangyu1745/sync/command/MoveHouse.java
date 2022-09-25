package cf.wangyu1745.sync.command;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.springframework.stereotype.Component;

import static cf.wangyu1745.sync.listener.MoveHouseListener.MOVE_STICK;

@Component(MoveHouse.MOVE_HOUSE)
@RequiredArgsConstructor
public class MoveHouse implements CommandExecutor {
    private final FileConfiguration config;
    public static final String MOVE_HOUSE = "movehouse";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("请勿在控制台使用此命令");
            return true;
        }
        if (!cmd.getName().equalsIgnoreCase(MOVE_HOUSE)) {
            return true;
        }
        Player p = (Player) sender;
        ItemStack itemStack = new ItemStack(Material.valueOf(config.getString("material")));
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(MOVE_STICK);
        itemStack.setItemMeta(itemMeta);
        p.getInventory().addItem(itemStack);
        return true;
    }
}
