package cf.wangyu1745.sync;

import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import cf.wangyu1745.sync.listener.MainListener;
import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

public final class Sync extends JavaPlugin {
    public static Economy eco;
    public static Jedis jedis;
    public static Logger log;
    public static FileConfiguration config;
    public static final String VAULT = "/vault/";
    public static final String INV = "/INV/";
    public static Method serializeNBT;
    public static Method deserializeNBT;
    public static Method listWrite;
    public static Method listLoad;

    @Override
    public void onEnable() {
        // Plugin startup logic
        if (!init()) {
            return;
        }
        getServer().getPluginManager().registerEvents(new MainListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gather") && sender instanceof Player) {
            Player p = (Player) sender;
            jedis.publish(VAULT.getBytes(StandardCharsets.UTF_8), p.getName().getBytes(StandardCharsets.UTF_8));
            sender.sendMessage("转账中");
            getServer().getScheduler().runTaskLater(this, () -> {
                double balance = 0;
                byte[] b;
                while ((b = jedis.lpop((VAULT + p.getName()).getBytes(StandardCharsets.UTF_8))) != null) {
                    balance += Double.parseDouble(new String(b, StandardCharsets.UTF_8));
                }
                eco.depositPlayer(p, balance);
                balance = eco.getBalance(p);
                sender.sendMessage("您当前的余额是" + balance);
            }, 20);
        }
        return true;
    }

    private boolean init() {
        try {
            log = getLogger();
            initConfig();
            initRedis();
            initVault();
            serializeNBT = ItemStack.class.getMethod("serializeNBT");
            deserializeNBT = ItemStack.class.getMethod("deserializeNBT", NBTTagCompound.class);
            listWrite = NBTTagList.class.getDeclaredMethod("write", DataOutput.class);
            listWrite.setAccessible(true);
            listLoad = NBTTagList.class.getDeclaredMethod("load", DataInput.class, int.class, NBTReadLimiter.class);
            listLoad.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void initConfig() {
        saveDefaultConfig();
        reloadConfig();
        getConfig().set("id", new Random().nextLong());
        saveConfig();
        config = getConfig();
    }

    private void initVault() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        eco = Objects.requireNonNull(rsp).getProvider();
        BukkitScheduler scheduler = getServer().getScheduler();
        Sync vaultSync = this;
        new Thread(() -> {
            try (Jedis jedis = new Jedis(getConfig().getString("redis.host"), getConfig().getInt("redis.port"))) {
                jedis.subscribe(new BinaryJedisPubSub() {
                    @Override
                    public void onMessage(byte[] channel, byte[] message) {
                        scheduler.runTask(vaultSync, () -> {
                            String name = new String(message, StandardCharsets.UTF_8);
                            if (vaultSync.getServer().getOnlinePlayers().stream()
                                    .noneMatch(p -> p.getName().equals(name))) {
                                double balance = eco.getBalance(name);
                                eco.withdrawPlayer(name, balance);
                                Sync.jedis.lpush((VAULT + name).getBytes(StandardCharsets.UTF_8),
                                        String.valueOf(balance).getBytes(StandardCharsets.UTF_8));
                            }
                        });
                    }
                }, VAULT.getBytes(StandardCharsets.UTF_8));
            }
        }).start();
    }

    private void initRedis() {
        jedis = new Jedis(getConfig().getString("redis.host"), getConfig().getInt("redis.port"));
        log.log(Level.INFO, "jedis.ping() = " + jedis.ping());
    }
}
