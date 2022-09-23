package cf.wangyu1745.sync;

import cf.wangyu1745.sync.aspect.Time;
import cf.wangyu1745.sync.command.*;
import cf.wangyu1745.sync.listener.CopyListener;
import cf.wangyu1745.sync.listener.KitListener;
import cf.wangyu1745.sync.listener.LogIOListener;
import lombok.SneakyThrows;
import net.milkbowl.vault.economy.Economy;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import org.apache.zookeeper.ZooKeeper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cf.wangyu1745.sync.command.Copy.COPY;
import static cf.wangyu1745.sync.command.Kit.KIT;
import static cf.wangyu1745.sync.command.Link.LINK;
import static cf.wangyu1745.sync.command.Sync.SYNC;
import static cf.wangyu1745.sync.command.XYZInfo.XYZ;

public final class Main extends JavaPlugin {
    public static volatile boolean debug = false;
    public static final String RPC_PATH = "/sync";

    public static Method listWrite;
    public static Method listLoad;
    public static Method write;
    public static Method load;
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
        getServer().getPluginManager().registerEvents(context.getBean(LogIOListener.class), this);
        getServer().getPluginManager().registerEvents(context.getBean(KitListener.class), this);
        getServer().getPluginManager().registerEvents(context.getBean(CopyListener.class), this);
        //注册命令
        getCommand(LINK).setExecutor(context.getBean(Link.class));
        getCommand(XYZ).setExecutor(context.getBean(XYZInfo.class));
        getCommand(COPY).setExecutor(context.getBean(Copy.class));
        getCommand(SYNC).setExecutor(context.getBean(Sync.class));
        getCommand(KIT).setExecutor(context.getBean(Kit.class));


//        Kit.inventory = Bukkit.createInventory(new Kit.InvHolder(), 54);
//        getCommand()
    }

    /**
     * 测试用途
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ddd")) {
            /*Economy economy = context.getBean(Economy.class);
            EconomyResponse resp = economy.withdrawPlayer("wangyu", 1000);
            System.out.println("resp.transactionSuccess() = " + resp.transactionSuccess());
            System.out.println("resp.amount = " + resp.amount);
            System.out.println("resp.balance = " + resp.balance);
            System.out.println("resp.errorMessage = " + resp.errorMessage);*/
            sender.getEffectivePermissions().forEach(e-> System.out.println(e.getPermission()));
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

        ZooKeeper zooKeeper = context.getBean(ZooKeeper.class);
        zooKeeper.close();
    }

    private boolean init() {
        try {
            initConfig();
            // 反射nbt方法们
            write = NBTTagCompound.class.getDeclaredMethod("write", DataOutput.class);
            write.setAccessible(true);
            load = NBTTagCompound.class.getDeclaredMethod("load", DataInput.class, int.class, NBTReadLimiter.class);
            load.setAccessible(true);
            listWrite = NBTTagList.class.getDeclaredMethod("write", DataOutput.class);
            listWrite.setAccessible(true);
            listLoad = NBTTagList.class.getDeclaredMethod("load", DataInput.class, int.class, NBTReadLimiter.class);
            listLoad.setAccessible(true);
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
