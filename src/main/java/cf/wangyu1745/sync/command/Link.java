package cf.wangyu1745.sync.command;

import cf.wangyu1745.sync.entity.Tunnel;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class Link implements CommandExecutor {
    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;
    private final FileConfiguration config;
    private final Random random = new Random();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("请勿在控制台使用此命令");
            return true;
        }
        if (!cmd.getName().equalsIgnoreCase("link")) {
            return true;
        }
        if (args.length != 5) {
            return false;
        }
        var p = (Player) sender;
        if (p.getInventory().getItemInMainHand().getType() != Material.AIR) {
            p.sendMessage("使用链接命令时主手不能持有物品");
            return true;
        }
        var block = /*p.getWorld().getBlockAt(Integer.parseInt(args[0]), Integer.parseInt(args[1]),
                Integer.parseInt(args[2])).getState();*/
                p.getTargetBlock(EnumSet.of(Material.AIR,Material.WATER), 3).getState();
        if (!(block instanceof Chest)) {
            sender.sendMessage("使用链接命令需要准星对准箱子");
            return true;
        }
        if (config.getString("id").equals(args[0]) && p.getWorld().getName().equals(args[1]) && block.getX() == Integer.parseInt(args[2]) && block.getY() == Integer.parseInt(args[3]) && block.getZ() == Integer.parseInt(args[4])) {
            sender.sendMessage("禁止隧道入口和出口重叠");
            return true;
        }
        long count = Arrays.stream(((Chest) block).getBlockInventory().getContents()).filter(Objects::nonNull).filter(e -> e.getType() != Material.AIR).count();
        if (count > 0) {
            sender.sendMessage("使用链接命令时需要箱子为空");
            return true;
        }
        p.sendMessage("请在三秒内将系统发放的木棍放入箱子内");
        int nextInt = random.nextInt(64);
        p.getInventory().addItem(new ItemStack(Material.STICK, nextInt));
        scheduler.runTaskLater(plugin, () -> {
            int count_ = Arrays.stream(((Chest) block).getBlockInventory().getContents()).filter(Objects::nonNull).filter(e -> e.getType() == Material.STICK).map(ItemStack::getAmount).reduce(0, Integer::sum);
            if (count_ != nextInt) {
                p.sendMessage("木棍数量不符,隧道创建失败");
            } else {
                Tunnel tunnel = Tunnel.builder().fromServer(config.getString("id")).fromWorld(p.getWorld().getName()).fromX(block.getX()).fromY(block.getY()).fromZ(block.getZ()).toServer(args[0]).toWorld(args[1]).toX(Integer.parseInt(args[2])).toY(Integer.parseInt(args[3])).toZ(Integer.parseInt(args[4])).active(true).build();
                tunnel.insert();
                p.sendMessage("隧道创建成功");
            }
        }, 60);
        return true;
    }

}
