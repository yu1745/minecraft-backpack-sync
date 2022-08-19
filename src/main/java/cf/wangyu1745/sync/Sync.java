package cf.wangyu1745.sync;

import cf.wangyu1745.sync.command.*;
import cf.wangyu1745.sync.listener.MainListener;
import cf.wangyu1745.sync.util.LifeCycle;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.DataInput;
import java.io.DataOutput;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

public final class Sync extends JavaPlugin {
    public static FileConfiguration config;
    public static final String VAULT_CHANNEL = "/vault/";

    public static Method listWrite;
    public static Method listLoad;
    public static Method write;
    public static Method load;
    public static JavaPlugin INSTANCE;
    public static ApplicationContext context;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().log(Level.INFO, "Sync enabled");
        if (!init()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        //初始化spring
        initContext();
        //注册事件监听器
        getServer().getPluginManager().registerEvents(context.getBean(MainListener.class), this);
        //注册命令
        Bukkit.getPluginCommand("servername").setExecutor(context.getBean(ServerName.class));
        Bukkit.getPluginCommand("worldname").setExecutor(context.getBean(WorldName.class));
        Bukkit.getPluginCommand("gather").setExecutor(context.getBean(Gather.class));
        Bukkit.getPluginCommand("link").setExecutor(context.getBean(Link.class));
        Bukkit.getPluginCommand("xyz").setExecutor(context.getBean(XYZ.class));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().log(Level.INFO, "Sync disabled");
        Map<String, LifeCycle> beansOfType = context.getBeansOfType(LifeCycle.class);
        beansOfType.values().forEach(LifeCycle::onDisable);
    }

    private boolean init() {
        try {
            INSTANCE = this;
            initConfig();
            write = NBTTagCompound.class.getDeclaredMethod("write", DataOutput.class);
            write.setAccessible(true);
            load = NBTTagCompound.class.getDeclaredMethod("load", DataInput.class, int.class, NBTReadLimiter.class);
            load.setAccessible(true);
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
        if (getConfig().getString("id") == null) {
            getConfig().set("id", new Random().nextLong());
            saveConfig();
        }
        config = getConfig();
    }


    private void initContext() {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.setClassLoader(getClassLoader());
        annotationConfigApplicationContext.scan("cf.wangyu1745.sync");
        annotationConfigApplicationContext.refresh();
        context = annotationConfigApplicationContext;
    }

}
