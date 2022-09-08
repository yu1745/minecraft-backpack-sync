package cf.wangyu1745.sync.listener;

import lombok.RequiredArgsConstructor;
import lombok.var;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.data.redis.core.RedisTemplate;

import static cf.wangyu1745.sync.Main.VAULT_CHANNEL;

@SuppressWarnings({"unused", "deprecation"})
//@Component
@RequiredArgsConstructor
public class GatherListener {
    private final BukkitScheduler scheduler;
    private final JavaPlugin plugin;
    private final Economy eco;
    private final RedisTemplate<String, Double> sd;

    public void handleMessage(String name) {
        scheduler.scheduleSyncDelayedTask(plugin, () -> {
            System.out.println("name = " + name);
            var balance = eco.getBalance(name);
            eco.withdrawPlayer(name, balance);
            scheduler.runTaskAsynchronously(plugin, () -> sd.opsForList().rightPush(VAULT_CHANNEL + name, balance));
        });
    }
}
