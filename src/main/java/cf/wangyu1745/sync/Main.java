package cf.wangyu1745.sync;

import cf.wangyu1745.sync.aspect.Time;
import lombok.SneakyThrows;
import net.milkbowl.vault.economy.Economy;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main extends JavaPlugin {
    public static volatile boolean debug = false;
    public static final String RPC_PATH = "/sync";

    public static volatile ApplicationContext context;


    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().log(Level.INFO, "Sync enabled");
        if (!init()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        //注册事件监听器
        context.getBeansOfType(Listener.class).values().forEach(e -> getServer().getPluginManager().registerEvents(e, this));
        /*getServer().getPluginManager().registerEvents(context.getBean(LogIOListener.class), this);
        getServer().getPluginManager().registerEvents(context.getBean(KitListener.class), this);
        getServer().getPluginManager().registerEvents(context.getBean(CopyListener.class), this);*/
        //注册命令
        context.getBeansOfType(CommandExecutor.class).entrySet().stream().filter(e -> e.getValue().getClass() != Main.class)
                .forEach(e -> getCommand(e.getKey()).setExecutor(e.getValue()));
        /*getCommand(LINK).setExecutor(context.getBean(Link.class));
        getCommand(XYZ).setExecutor(context.getBean(XYZInfo.class));
        getCommand(COPY).setExecutor(context.getBean(Copy.class));
        getCommand(SYNC).setExecutor(context.getBean(Sync.class));
        getCommand(MOVE_HOUSE).setExecutor(context.getBean(MoveHouse.class));
        getCommand(KIT).setExecutor(context.getBean(Kit.class));*/
//        Kit.inventory = Bukkit.createInventory(new Kit.InvHolder(), 54);
//        getCommand()
    }

    /**
     * 测试用途
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ddd") && sender instanceof Player) {
            sender.getEffectivePermissions().forEach(e -> System.out.println(e.getPermission()));
        }
        return true;
    }

    @SneakyThrows
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().log(Level.INFO, "Sync disabled");
        Map<String, LifeCycle> beansOfType = context.getBeansOfType(LifeCycle.class);
        beansOfType.values().forEach(LifeCycle::onDisable);

        AsyncCuratorFramework zooKeeper = context.getBean(AsyncCuratorFramework.class);
        zooKeeper.unwrap().close();
    }

    private boolean init() {
        try {
            initConfig();
            // 初始化spring
            initContext();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void initConfig() {
        saveDefaultConfig();
        reloadConfig();
        if (getConfig().getString("id") == null) {
            getConfig().set("id", new Random().nextLong());
            saveConfig();
        }
        // 检测material
        Material.valueOf(getConfig().getString("material"));
        debug = getConfig().getBoolean("debug", false);
    }


    private void initContext() {
        AnnotationConfigApplicationContext context_ = new AnnotationConfigApplicationContext();
        context_.setClassLoader(getClassLoader());
        context_.registerBean(BukkitScheduler.class, () -> getServer().getScheduler());
        context_.registerBean(Logger.class, this::getLogger);
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        context_.registerBean(Economy.class, () -> Objects.requireNonNull(rsp).getProvider());
        context_.registerBean(JavaPlugin.class, () -> this);
        context_.registerBean(FileConfiguration.class, this::getConfig);
        context_.registerBean(Time.class, getLogger());
        long start = System.currentTimeMillis();
        context_.scan(Main.class.getPackage().getName());
        long scan = System.currentTimeMillis();
        getLogger().info("scan cost " + (scan - start) + "ms");
        context_.refresh();
        getLogger().info("refresh cost " + (System.currentTimeMillis() - scan) + "ms");
        context = context_;
    }

}
