package cf.wangyu1745.sync.listener;

import static cf.wangyu1745.sync.util.Util.online;
import static cf.wangyu1745.sync.util.Util.reload;
import static cf.wangyu1745.sync.util.Util.save;
import static cf.wangyu1745.sync.util.Util.thisServer;

import java.util.List;
import java.util.Vector;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import lombok.var;

public class MainListener implements Listener {
    // private static final int END = -1;
    private static final List<String> l = new Vector<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var p = event.getPlayer();
        if (online(p)) {
            // 玩家已经在线
            if (!thisServer(p)) {
                // 已经在线但是不是本服务器，踢出
                l.add(p.getDisplayName());
                p.kickPlayer("已经在线");
            } else {
                // 常发生于mc服务器崩溃后，下线事件没有被执行
            }
        } else {
            online(p, true);
            reload(p);
        }

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            if (l.contains(event.getPlayer().getDisplayName())) {
                l.remove(event.getPlayer().getDisplayName());
            } else {
                save(event.getPlayer());
                online(event.getPlayer(), false);
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
