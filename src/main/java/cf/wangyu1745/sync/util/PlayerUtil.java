package cf.wangyu1745.sync.util;

import cf.wangyu1745.sync.entity.Login;
import cf.wangyu1745.sync.entity.Player;
import cf.wangyu1745.sync.mapper.LoginMapper;
import lombok.var;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static cf.wangyu1745.sync.entity.Login.OFFLINE;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
@Component
public class PlayerUtil {
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
    private final boolean debug;

    public PlayerUtil(FileConfiguration config, LoginMapper loginMapper, Logger logger, BukkitScheduler bukkitScheduler, JavaPlugin plugin) {
        this.config = config;
        this.loginMapper = loginMapper;
        this.logger = logger;
        this.scheduler = bukkitScheduler;
        this.plugin = plugin;
        this.ID = config.getString("id");
        this.debug = config.getBoolean("debug");
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

    public void updateLogin(org.bukkit.entity.Player p, boolean online, long lastDataId) {
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
        return CompletableFuture.runAsync(() -> {
            var start = System.nanoTime();
            Player pEntity = Player.builder().id(id).build().selectById();
            var bytes = pEntity.getData();
            NBTTagCompound nbt = NBTUtil.toNbt(bytes);
            logger.info(nbt.toString());
            scheduler.runTask(plugin, () -> {
                EntityPlayer p = ((CraftPlayer) player).getHandle();
                // readFromNBT
                p.a(nbt);
                logger.info(p.getName() + " 玩家数据大小: " + bytes.length);
                logger.info(p.getName() + " 恢复花费 " + ((double) (System.nanoTime() - start) / 1000000) + " ms");
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
                logger.info(nbt.toString());
                byte[] bytes = NBTUtil.toBytes(nbt);
                Player insert = Player.builder().data(bytes).name(p.getName()).build();
                insert.insert();
                logger.info(p.getName() + " 玩家数据大小: " + bytes.length);
                logger.info(p.getName() + " 保存耗费 " + ((double) (System.nanoTime() - start) / 1000000) + " ms");
                return insert.getId();
            } catch (Exception e) {
                e.printStackTrace();
                return -1L;
            }
        });
    }

    public static byte[] toBytes(org.bukkit.entity.Player player) {
        NBTTagCompound nbt = new NBTTagCompound();
        ((CraftPlayer) player).getHandle().b(nbt);
        return NBTUtil.toBytes(nbt);
    }

    public static void reloadFromBytes(org.bukkit.entity.Player player,byte[] bytes) {
        NBTTagCompound nbt = NBTUtil.toNbt(bytes);
        ((CraftPlayer) player).getHandle().a(nbt);
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
    @Deprecated
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
