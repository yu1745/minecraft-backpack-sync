package cf.wangyu1745.sync.command;

import lombok.RequiredArgsConstructor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

import static cf.wangyu1745.sync.Sync.VAULT_CHANNEL;

@Component
@RequiredArgsConstructor
public class Gather implements CommandExecutor {
    private final BukkitScheduler scheduler;
    private final JavaPlugin plugin;
    private final Economy eco;
    private final RedisTemplate<String, Double> sd;
    private final RedisTemplate<String, byte[]> sb;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("gather") && sender instanceof Player) {
            Player p = (Player) sender;
            sb.convertAndSend(VAULT_CHANNEL, p.getName().getBytes(StandardCharsets.UTF_8));
            sender.sendMessage("转账中");
            scheduler.runTaskLater(plugin, () -> {
                double balance = 0;
                Double b;
                while ((b = sd.opsForList().leftPop(VAULT_CHANNEL + p.getName())) != null) {
                    balance += b;
                }
                eco.depositPlayer(p, balance);
                balance = eco.getBalance(p);
                sender.sendMessage("您当前的余额是" + balance);
            }, 20);
        }
        return true;
    }
}
