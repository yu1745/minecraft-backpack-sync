package cf.wangyu1745.sync.config;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.FileConfiguration;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MybatisConfig {

    @SneakyThrows
    @Bean
    public DataSource dataSource(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") FileConfiguration config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl(config.getString("mysql.url"));
        hikariConfig.setUsername(config.getString("mysql.username"));
        hikariConfig.setPassword(config.getString("mysql.password"));
        return new HikariDataSource(hikariConfig);
    }

    @SneakyThrows
    @Bean
    public MybatisSqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) {
        MybatisSqlSessionFactoryBean sqlSessionFactoryBean = new MybatisSqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(false);
        sqlSessionFactoryBean.setGlobalConfig(globalConfig);
        //打印sql语句
//        if(Main.debug) {
//            MybatisConfiguration mybatisConfiguration = new MybatisConfiguration();
//            mybatisConfiguration.setLogImpl(StdOutImpl.class);
//            sqlSessionFactoryBean.setConfiguration(mybatisConfiguration);
//        }
        return sqlSessionFactoryBean;
    }


    @Bean
    public static MapperScannerConfigurer classPathMapperScanner() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("cf.wangyu1745.sync.mapper");
        return configurer;
    }
}
