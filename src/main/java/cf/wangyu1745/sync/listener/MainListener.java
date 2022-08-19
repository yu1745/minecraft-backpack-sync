package cf.wangyu1745.sync.listener;

import cf.wangyu1745.sync.util.InventoryUtil;
import lombok.RequiredArgsConstructor;
import lombok.var;
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

@SuppressWarnings("StatementWithEmptyBody")
@Component
@RequiredArgsConstructor
public class MainListener implements Listener {
    // private static final int END = -1;
    private final BukkitScheduler scheduler;
    private final JavaPlugin plugin;
    private final InventoryUtil inventoryUtil;
    private static final List<String> l = new Vector<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var p = event.getPlayer();
        if (inventoryUtil.online(p)) {
            // 玩家已经在线
            if (!inventoryUtil.thisServer(p)) {
                // 蹦极的代理先连要去的服务器再断开已经在的服务器，因此会有一段时间玩家同时在线两个服务器，此时需要等几秒原来的服务器下线
                // 已经在线但是不是在本服务器，等待，一端时间内没有从其他服务器下线，踢出
                p.getInventory().clear();
                l.add(p.getDisplayName());
                var i = new AtomicInteger(3);
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
                            inventoryUtil.online(p, true);
                            inventoryUtil.reload(p);
                        }
                    }
                }, 20);
            } else {
                // 常发生于mc服务器崩溃后，下线事件没有被执行
                // 啥也不干
            }
        } else {
            inventoryUtil.online(p, true);
            inventoryUtil.reload(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            if (l.contains(event.getPlayer().getDisplayName())) {
                l.remove(event.getPlayer().getDisplayName());
            } else {
                inventoryUtil.save(event.getPlayer());
                inventoryUtil.online(event.getPlayer(), false);
            }
            /*
             * double balance = eco.getBalance(p);
             * System.out.println("save " + event.getPlayer().getName() + " " + balance);
             * jedis.set((PREFIX + p.getName()).getBytes(StandardCharsets.UTF_8),
             * String.valueOf(balance).getBytes(StandardCharsets.UTF_8));
             */
            // var atomicInteger = new AtomicInteger(0);
            // Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull).peek(ignored
            // -> System.out.print(atomicInteger.getAndIncrement()+":")).forEach(i ->
            // System.out.println(i.getClass().getName()));
            /*
             * ByteArrayOutputStream s = new ByteArrayOutputStream();
             * new BukkitObjectOutputStream(s).writeObject(p.getInventory().getContents());
             * //fei
             */
            // var s = new ByteArrayOutputStream();
            // var out = new DataOutputStream(s);
            // Arrays.stream(p.getInventory().getContents())
            // .forEach(e -> {
            // try {
            // if (e == null || e.getType() == Material.AIR) {
            // out.writeInt(0);
            // return;
            // }
            // var bytes = new ByteArrayOutputStream();
            // Object convertNBTCompoundtoNMSItem =
            // NBTReflectionUtil.convertNBTCompoundtoNMSItem(NBTItem.convertItemtoNBT(e));

            // NBTReflectionUtil.writeNBT(NBTReflectionUtil.convertNBTCompoundtoNMSItem(NBTItem.convertItemtoNBT(e)),
            // bytes);
            // System.out.println(bytes.size());
            // out.writeInt(bytes.size());
            // out.write(bytes.toByteArray());
            // } catch (IOException e1) {
            // e1.printStackTrace();
            // } catch (NoSuchMethodException e1) {
            // // TODO Auto-generated catch block
            // e1.printStackTrace();
            // } catch (SecurityException e1) {
            // // TODO Auto-generated catch block
            // e1.printStackTrace();
            // }
            // });
            // out.writeInt(END);
            // VaultSync.jedis.set(p.getName().getBytes(StandardCharsets.UTF_8),
            // s.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
