package cf.wangyu1745.sync.util;

import cf.wangyu1745.sync.Main;
import cf.wangyu1745.sync.entity.Login;
import cf.wangyu1745.sync.entity.Player;
import cf.wangyu1745.sync.mapper.LoginMapper;
import lombok.RequiredArgsConstructor;
import lombok.var;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static cf.wangyu1745.sync.Main.load;
import static cf.wangyu1745.sync.Main.write;
import static cf.wangyu1745.sync.entity.Login.OFFLINE;

@SuppressWarnings("unused")
@Component
@RequiredArgsConstructor
public class PlayerInventoryUtil {
    // online
//    private static final String OL = "/online/";
//    private static final String LAST_LOGIN = "/last_login/";
    // inventory
//    private static final String INV = "/inv/";
    // separator
//    private final NumberFormat decimalFormat = DecimalFormat.getInstance();

    //    private final RedisTemplate<String, String> ss;
//    private final RedisTemplate<String, byte[]> sb;
    private final FileConfiguration config;
    private final LoginMapper loginMapper;
    private final String ID;
    private final Logger logger;
    private final BukkitScheduler scheduler;
    private final JavaPlugin plugin;

    public PlayerInventoryUtil(FileConfiguration config, LoginMapper loginMapper, Logger logger, BukkitScheduler bukkitScheduler, JavaPlugin plugin) {
        this.config = config;
        this.loginMapper = loginMapper;
        this.logger = logger;
        this.scheduler = bukkitScheduler;
        this.plugin = plugin;
        this.ID = config.getString("id");
    }


    public CompletableFuture<Boolean> isLogin(org.bukkit.entity.Player p) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return !Login.builder().name(p.getName()).build().selectById().getOnline().equals(OFFLINE);
            } catch (NullPointerException e) {
                Login.builder().name(p.getName()).online(ID).lastLogin(ID).lastDataId(-1L).build().insert();
                return false;
            }
        });
    }

    public void updateLogin(org.bukkit.entity.Player p, boolean online,long lastDataId) {
        Login login = Login.builder().name(p.getName()).build().selectById().setOnline(online ? ID : OFFLINE).setLastDataId(lastDataId);
        if (!online) {
            login.setLastLogin(ID);
        }
        login.updateById();
    }

    public void updateLogin(org.bukkit.entity.Player p, boolean online) {
        Login login = Login.builder().name(p.getName()).build().selectById().setOnline(online ? ID : OFFLINE);
        if (!online) {
            login.setLastLogin(ID);
        }
        login.updateById();
    }

    public CompletableFuture<Boolean> thisServer(org.bukkit.entity.Player p) {
        return CompletableFuture.supplyAsync(() -> Login.builder().name(p.getName()).build().selectById().getOnline().equals(ID));
//        return Objects.equals(ss.opsForValue().get(OL.concat(p.getName())), config.getString("id"));
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<Void> load(org.bukkit.entity.Player player, long id) {
        return CompletableFuture.supplyAsync(() -> {
            var start = System.nanoTime();
            Player pEntity = Player.builder().id(id).build().selectById();
            var bytes = pEntity.getData();
            var nbt = new NBTTagCompound();
            try {
                load.invoke(nbt, new DataInputStream(new ByteArrayInputStream(Objects.requireNonNull(bytes))), 4, new NBTReadLimiter(2097152));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return Pair.of(nbt, start);
        }).thenAcceptAsync(pair -> {
            NBTTagCompound nbt = pair.getLeft();
            Long start = pair.getRight();
            scheduler.runTask(plugin, () -> {
                EntityPlayer p = ((CraftPlayer) player).getHandle();
                // readFromNBT
                p.a(nbt);
                logger.info(p.getName() + " 重载花费 " + ((double) (System.nanoTime() - start) / 1000000) + " ms");
            });
        });
    }

    public CompletableFuture<Long> save(org.bukkit.entity.Player player) {
        var start = System.nanoTime();
        EntityPlayer p = ((CraftPlayer) player).getHandle();
        var nbt = new NBTTagCompound();
        // writeToNBT
        p.b(nbt);
        return CompletableFuture.supplyAsync(() -> {
            try {
                var bytes = new ByteArrayOutputStream();
                write.invoke(nbt, new DataOutputStream(bytes));
                logger.info(p.getName() + " 玩家数据大小: " + bytes.size());
                Player insert = Player.builder().data(bytes.toByteArray()).name(p.getName()).build();
                insert.insert();
                logger.info(p.getName() + " 保存耗费 " + ((double) (System.nanoTime() - start) / 1000000) + " ms");
                return insert.getId();
            } catch (Exception e) {
                e.printStackTrace();
                return -1L;
            }
        });
    }

    public static @Nullable byte[] toBytesOrdered(org.bukkit.entity.Player player) {
        try {
            var bytes = new ByteArrayOutputStream();
            var dataOutput = new DataOutputStream(bytes);
            EntityPlayer p = ((CraftPlayer) player).getHandle();
            ItemStackUtil.saveOrdered(p.inventory.getContents(), dataOutput);
            return bytes.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
                        Main.write.invoke(nbt, new DataOutputStream(bytes));
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

    public static List<byte[]> itemStacks2BytesList(ItemStack[] itemStacks) {
        var out = new ArrayList<byte[]>(itemStacks.length);
        Arrays.stream(itemStacks).filter(Objects::nonNull).map(CraftItemStack::asNMSCopy).forEach(e -> {
            var nbt = e.save(new NBTTagCompound());
            var bytes = new ByteArrayOutputStream();
            try {
                Main.write.invoke(nbt, new DataOutputStream(bytes));
                if (bytes.size() != 0) {
                    out.add(bytes.toByteArray());
                }
            } catch (Exception e_) {
                e_.printStackTrace();
            }
        });
        return out;
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
                load.invoke(nbt, new DataInputStream(new ByteArrayInputStream(b)), 4, new NBTReadLimiter(2097152));
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
     * @return bytes所包含的物品格子中的前n个格子所占的字节数
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

    public String lastLogin(org.bukkit.entity.Player p) {
        return Login.builder().name(p.getName()).build().selectById().getLastLogin();
    }
}
