package cf.wangyu1745.sync.util;

import cf.wangyu1745.sync.Sync;
import lombok.RequiredArgsConstructor;
import lombok.var;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static cf.wangyu1745.sync.Sync.listLoad;

@Component
@RequiredArgsConstructor
public class InventoryUtil {
    // online
    private static final String OL = "/online/";
    // inventory
    private static final String INV = "/inv/";
    // separator
    private final ThreadLocal<NumberFormat> decimalFormat = new ThreadLocal<>();

    private final RedisTemplate<String, String> ss;
    private final RedisTemplate<String, byte[]> sb;
    private final FileConfiguration config;

    @PostConstruct
    public void init() {
        decimalFormat.set(DecimalFormat.getInstance());
    }


    public boolean online(Player p) {
        return ss.opsForValue().get(OL.concat(p.getName())) != null;
    }

    public void online(Player p, boolean online) {
        if (online) {
            ss.opsForValue().set(OL.concat(p.getName()), config.getString("id"));
        } else {
            ss.delete(OL.concat(p.getName()));
        }
    }

    public boolean thisServer(Player p) {
        return Objects.equals(ss.opsForValue().get(OL.concat(p.getName())), config.getString("id"));
    }

    public void reload(Player player) {
        try {
            var start = System.nanoTime();
            var bytes = sb.opsForValue().get(INV.concat(player.getName()));
            var l = new NBTTagList();
            listLoad.invoke(l, new DataInputStream(new ByteArrayInputStream(Objects.requireNonNull(bytes))), 4, new NBTReadLimiter(2097152));
            EntityPlayer p = ((CraftPlayer) player).getHandle();
            // readFromNBT
            p.inventory.b(l);
            System.out.println(p.getName() + " reload cost " + decimalFormat.get().format((double) (System.nanoTime() - start) / 1000000) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(Player player) {
        try {
            var start = System.nanoTime();
            EntityPlayer p = ((CraftPlayer) player).getHandle();
            // writeToNBT
            var l = p.inventory.a(new NBTTagList());
            var bytes = new ByteArrayOutputStream();
            Sync.listWrite.invoke(l, new DataOutputStream(bytes));
            System.out.println(p.getName() + "'s backpack size: " + bytes.size());
            sb.opsForValue().set(INV.concat(player.getName()), bytes.toByteArray());
            System.out.println(p.getName() + " save cost " + decimalFormat.get().format((double) (System.nanoTime() - start) / 1000000) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] itemStacks2Bytes(ItemStack[] itemStacks) {
        var all = new ByteArrayOutputStream();
        var out = new DataOutputStream(all);
        Arrays.stream(itemStacks).filter(Objects::nonNull)
                // .peek(e->System.out.println(e.getType()))
                .map(CraftItemStack::asNMSCopy)
                // .peek(e->System.out.println(e.getItem().getName()))
                .forEach(e -> {
                    var nbt = new NBTTagCompound();
                    e.save(nbt);
                    var bytes = new ByteArrayOutputStream();
                    try {
                        Sync.write.invoke(nbt, new DataOutputStream(bytes));
                        if (bytes.size() != 0) {
                            out.writeInt(bytes.size());
                            out.write(bytes.toByteArray());
                        }
                    } catch (Exception e_) {
                        e_.printStackTrace();
                    }
                });
        return all.toByteArray();
    }

    public static ItemStack[] bytes2ItemStacks(byte[] bytes) {
        var l = new ArrayList<ItemStack>();
        var in = new DataInputStream(new ByteArrayInputStream(bytes));
        int len;
        try {
            while (in.available() > 0) {
                len = in.readInt();
                var b = new byte[len];
                //noinspection ResultOfMethodCallIgnored
                in.read(b);
                var nbt = new NBTTagCompound();
                Sync.load.invoke(nbt, new DataInputStream(new ByteArrayInputStream(b)), 4, new NBTReadLimiter(2097152));
                l.add(CraftItemStack.asBukkitCopy(new net.minecraft.server.v1_12_R1.ItemStack(nbt)));
            }
        } catch (Exception ignored) {
        }
        return l.toArray(new ItemStack[]{});
    }


    /*public static int ItemStackCount(byte[] bytes) {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        var num = 0;
        try {
            while (in.available() > 0) {
                int len = in.readInt();
                //noinspection ResultOfMethodCallIgnored
                in.skip(len);
                num++;
            }
        } catch (Exception ignored) {
        }
        return num;
    }*/

    /**
     * @return bytes所包含的物品中的前n个所占的字节数
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static int lenOf(byte[] bytes, int n) {
        if (n <= 0) return 0;
        if (bytes.length == 0) return 0;
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        var num = 0;
        var rt = 0;
        try {
            while (in.available() > 0 && num < n) {
                int len = in.readInt();
                in.skip(len);
                num++;
                rt += len + 4;
            }
        } catch (Exception ignored) {
        }
        return Math.min(rt, bytes.length);
    }
}
