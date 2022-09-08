package cf.wangyu1745.sync.listener;

import cf.wangyu1745.sync.util.InventoryUtil;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class MainListener implements Listener {
    // private static final int END = -1;
    private final BukkitScheduler scheduler;
    private final JavaPlugin plugin;
    private final InventoryUtil inventoryUtil;
    private static final List<String> l = new Vector<>();
    private final Logger logger;
    private final FileConfiguration config;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var p = event.getPlayer();
        if (inventoryUtil.online(p)) {
            // 玩家已经在线
            if (!inventoryUtil.thisServer(p)) {
                // 蹦极的代理先连要去的服务器再断开已经在的服务器，因此会有一段时间玩家同时在线两个服务器，此时需要等几秒原来的服务器下线
                // 已经在线但是不是在本服务器，等待，一端时间内没有从其他服务器下线，踢出
                p.getInventory().clear();
                l.add(p.getName());
                int retry = 3;
                var i = new AtomicInteger(retry);
                scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (inventoryUtil.online(p)) {
                            if (i.getAndDecrement() > 0) {
                                System.out.println("剩余尝试次数 = " + i.get());
                                scheduler.scheduleSyncDelayedTask(plugin, this, 20);
                            } else {
                                p.kickPlayer("已经在线");
                            }
                        } else {
                            //恢复背包
                            System.out.println("尝试" + (retry - i.get() + 1) + "次后成功登录");
                            inventoryUtil.online(p, true);
                            inventoryUtil.reload(p);
                            l.remove(p.getName());
                        }
                    }
                }, 20);
            } else {
                // 常发生于mc服务器崩溃后，下线事件没有被执行
                // 啥也不干
                logger.warning(p.getName() + "已经在本服在线");
                l.remove(p.getName());
            }
        } else {
            // 玩家不在线
            if (config.getString("id").equals(inventoryUtil.lastLogin(p))) {
                // 啥也不干
                logger.info(p.getName() + "同服登录");
                inventoryUtil.online(p, true);
                return;
            }
            inventoryUtil.online(p, true);
            inventoryUtil.reload(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            if (l.contains(event.getPlayer().getName())) {
                l.remove(event.getPlayer().getName());
            } else {
                inventoryUtil.save(event.getPlayer());
                inventoryUtil.online(event.getPlayer(), false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
