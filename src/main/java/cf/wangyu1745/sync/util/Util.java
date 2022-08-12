package cf.wangyu1745.sync.util;

import static cf.wangyu1745.sync.Sync.jedis;
import static cf.wangyu1745.sync.Sync.listLoad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.bukkit.entity.Player;

import cf.wangyu1745.sync.Sync;
import lombok.var;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagList;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;

public class Util {
    public static final String ONLINE = "/online/";
    public static final String INV = "/inv/";

    public static boolean online(Player p) {
        byte[] bytes = jedis.get((ONLINE + p.getName()).getBytes(StandardCharsets.UTF_8));
        return bytes != null && bytes.length != 0;
    }

    public static void online(Player p, boolean online) {
        if (online) {
            jedis.set((ONLINE + p.getName()).getBytes(StandardCharsets.UTF_8),
                    Sync.config.getString("id").getBytes(StandardCharsets.UTF_8));
        } else {
            jedis.del((ONLINE + p.getName()).getBytes(StandardCharsets.UTF_8));
        }
    }

    public static boolean thisServer(Player p) {
        byte[] bytes = jedis.get((ONLINE + p.getName()).getBytes(StandardCharsets.UTF_8));
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        return Arrays.equals(bytes, Sync.config.getString("id").getBytes(StandardCharsets.UTF_8));
    }

    public static void reload(Player player) {
        try {
            var start = System.nanoTime();
            var bytes = jedis.get((INV + player.getName()).getBytes(StandardCharsets.UTF_8));
            var l = new NBTTagList();
            listLoad.invoke(l, new DataInputStream(new ByteArrayInputStream(bytes)), 4,
                    new NBTReadLimiter(2097152));
            EntityPlayer p = ((CraftPlayer) player).getHandle();
            // readFromNBT
            p.inventory.b(l);
            System.out.println(player.getName() + " reload cost " + (System.nanoTime() - start) / 1000 + " us");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save(Player player) {
        try {
            var start = System.nanoTime();
            EntityPlayer p = ((CraftPlayer) player).getHandle();
            // writeToNBT
            var l = p.inventory.a(new NBTTagList());
            var bytes = new ByteArrayOutputStream();
            Sync.listWrite.invoke(l, new DataOutputStream(bytes));
            System.out.println(p.getName() + "'s backpack size: " + bytes.size());
            jedis.set((INV + player.getName()).getBytes(StandardCharsets.UTF_8),
                    bytes.toByteArray());
            System.out.println(p.getName() + " save cost " + (System.nanoTime() - start) / 1000 + " us");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
