package cf.wangyu1745.sync.config;

import cf.wangyu1745.sync.Sync;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;
import java.util.logging.Logger;

@Configuration
public class BukkitConfig {
    @Bean
    public BukkitScheduler scheduler(JavaPlugin plugin) {
        return plugin.getServer().getScheduler();
    }

    @Bean
    public FileConfiguration pluginConfig(JavaPlugin plugin) {
        return plugin.getConfig();
    }

    @Bean
    public JavaPlugin plugin() {
        return Sync.INSTANCE;
    }

    @Bean(name = "bukkitLogger")
    public Logger bukkitLogger(JavaPlugin plugin) {
        return plugin.getLogger();
    }

    @Bean
    public Economy economy(JavaPlugin plugin) {
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        return Objects.requireNonNull(rsp).getProvider();
    }
}
