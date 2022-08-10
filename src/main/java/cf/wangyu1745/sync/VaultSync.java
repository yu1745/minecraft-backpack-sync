package cf.wangyu1745.sync;

import cf.wangyu1745.sync.listener.MainListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class VaultSync extends JavaPlugin {
    public static Economy eco;
    public static Jedis jedis;
    public static Logger log;
    public static final String VAULT = "/vault/";


    @Override
    public void onEnable() {
        // Plugin startup logic
        processConfig();
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

    private void processConfig() {
        saveDefaultConfig();
//        System.out.println("getDataFolder().getAbsolutePath() = " + getDataFolder().getAbsolutePath());
        reloadConfig();
        getConfig().set("id", new Random().nextLong());
        saveConfig();
    }

    private boolean init() {
        try {
            log = getLogger();
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            eco = Objects.requireNonNull(rsp).getProvider();
            jedis = new Jedis(getConfig().getString("redis.host"), getConfig().getInt("redis.port"));
            log.log(Level.INFO, "jedis.ping() = " + jedis.ping());
            BukkitScheduler scheduler = getServer().getScheduler();
            VaultSync vaultSync = this;
            new Thread(() -> {
                try (Jedis jedis = new Jedis(getConfig().getString("redis.host"), getConfig().getInt("redis.port"))) {
                    jedis.subscribe(new BinaryJedisPubSub() {
                        @Override
                        public void onMessage(byte[] channel, byte[] message) {
                            scheduler.runTask(vaultSync, () -> {
                                String name = new String(message, StandardCharsets.UTF_8);
                                if (vaultSync.getServer().getOnlinePlayers().stream().noneMatch(p -> p.getName().equals(name))) {
                                    double balance = eco.getBalance(name);
                                    eco.withdrawPlayer(name, balance);
                                    VaultSync.jedis.lpush((VAULT + name).getBytes(StandardCharsets.UTF_8), String.valueOf(balance).getBytes(StandardCharsets.UTF_8));
                                }
                            });
                        }
                    }, VAULT.getBytes(StandardCharsets.UTF_8));
                }
            }).start();
        } catch (Exception e) {
            getServer().getPluginManager().registerEvents(new MainListener(), this);
            return false;
        }
        return true;
    }
}
