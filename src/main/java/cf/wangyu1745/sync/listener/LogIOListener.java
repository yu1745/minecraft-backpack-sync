package cf.wangyu1745.sync.listener;

import cf.wangyu1745.sync.entity.Login;
import cf.wangyu1745.sync.util.PlayerInventoryUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.var;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class LogIOListener implements Listener {
    // private static final int END = -1;
//    private final BukkitScheduler scheduler;
//    private final JavaPlugin plugin;
    private final PlayerInventoryUtil playerInventoryUtil;
    private static final List<String> l = new Vector<>();
    private final Logger logger;
    private final FileConfiguration config;
    private String id;

    @PostConstruct
    void post() {
        id = config.getString("id");
    }

    /*@EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var p = event.getPlayer();
        if (playerInventoryUtil.isLogin(p)) {
            // 玩家已经在线
            if (!playerInventoryUtil.thisServer(p)) {
                // 蹦极的代理先连要去的服务器再断开已经在的服务器，因此会有一段时间玩家同时在线两个服务器，此时需要等几秒原来的服务器下线
                // 已经在线但是不是在本服务器，等待，一端时间内没有从其他服务器下线，踢出
                p.getInventory().clear();
                l.add(p.getName());
                int retry = 3;
                var i = new AtomicInteger(retry);
                scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (playerInventoryUtil.isLogin(p)) {
                            if (i.getAndDecrement() > 0) {
                                System.out.println("剩余尝试次数 = " + i.get());
                                scheduler.scheduleSyncDelayedTask(plugin, this, 20);
                            } else {
                                p.kickPlayer("已经在线");
                            }
                        } else {
                            //恢复背包
                            System.out.println("尝试" + (retry - i.get() + 1) + "次后成功登录");
                            playerInventoryUtil.updateLogin(p, true);
                            playerInventoryUtil.load(p);
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
            if (config.getString("id").equals(playerInventoryUtil.lastLogin(p))) {
                // 啥也不干
                logger.info(p.getName() + "同服登录");
                playerInventoryUtil.updateLogin(p, true);
                return;
            }
            playerInventoryUtil.updateLogin(p, true);
            playerInventoryUtil.load(p);
        }
    }*/

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerInventoryUtil.isLogin(player).thenAcceptAsync(isLogin -> {
            if (isLogin) {
                playerInventoryUtil.thisServer(player).thenAcceptAsync(isThisServer -> {
                    //noinspection StatementWithEmptyBody
                    if (isThisServer) {
                        // 常发生于服务器之前崩了,玩家没下线
                        // 啥也不用干
                    } else {
                        // 蹦极的代理先连要去的服务器再断开已经在的服务器，因此会有一段时间玩家同时在线两个服务器，此时需要等几秒原来的服务器下线
                        // 已经在线但是不是在本服务器，等待，一端时间内没有从其他服务器下线，踢出
                        player.getInventory().clear();
                        l.add(player.getName());
                        int retry = 3;
                        var i = new AtomicInteger(retry);
                        CompletableFuture.runAsync(new Runnable() {
                            @SneakyThrows
                            @Override
                            public void run() {
                                TimeUnit.SECONDS.sleep(1);
                                playerInventoryUtil.isLogin(player).thenAcceptAsync(isLogin -> {
                                    if (isLogin) {
                                        if (i.getAndDecrement() > 0) {
                                            logger.info("剩余尝试次数 = " + i.get());
                                            CompletableFuture.runAsync(this);
                                        } else {
                                            player.kickPlayer("已在其他服务器在线");
                                        }
                                    } else {
                                        //恢复背包
                                        logger.info("尝试" + (retry - i.get() + 1) + "次后成功登录");
                                        playerInventoryUtil.updateLogin(player, true);
                                        CompletableFuture.supplyAsync(() -> Login.builder().name(player.getName()).build().selectById().getLastDataId())
                                                .thenAcceptAsync(dataId -> playerInventoryUtil.load(player, dataId))
                                                .thenRunAsync(() -> l.remove(player.getName()));
                                    }
                                });
                            }
                        });
                    }
                });
            } else {
                if (id.equals(playerInventoryUtil.lastLogin(player))) {
                    // 同服登录,啥也不干
                    logger.info(player.getName()+"同服登录");
                    playerInventoryUtil.updateLogin(player, true);
                } else {
                    // 重载
                    playerInventoryUtil.updateLogin(player, true);
                    CompletableFuture.supplyAsync(() -> Login.builder().name(player.getName()).build().selectById().getLastDataId())
                            .thenAcceptAsync(dataId -> playerInventoryUtil.load(player, dataId))
                            .thenRunAsync(() -> l.remove(player.getName()));
                }
            }
        });
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            if (!l.remove(event.getPlayer().getName())) {
                playerInventoryUtil.save(event.getPlayer())
                        .thenAcceptAsync(id -> playerInventoryUtil.updateLogin(event.getPlayer(), false, id));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
