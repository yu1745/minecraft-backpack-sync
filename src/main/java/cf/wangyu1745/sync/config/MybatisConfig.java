package cf.wangyu1745.sync.config;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.mysql.cj.jdbc.Driver;
import lombok.SneakyThrows;
import org.bukkit.configuration.file.FileConfiguration;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class MybatisConfig {

    @SneakyThrows
    @Bean
    public DataSource dataSource(FileConfiguration config) {
        Connection connection = new SimpleDriverDataSource(new Driver(), config.getString("mysql.url"),
                config.getString("mysql.username"),
                config.getString("mysql.password")).getConnection();
        return new SingleConnectionDataSource(connection,true);
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
//        MybatisConfiguration mybatisConfiguration = new MybatisConfiguration();
//        mybatisConfiguration.setLogImpl(StdOutImpl.class);
//        sqlSessionFactoryBean.setConfiguration(mybatisConfiguration);
        return sqlSessionFactoryBean;
    }


    @Bean
    public static MapperScannerConfigurer classPathMapperScanner() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setBasePackage("cf.wangyu1745.sync.mapper");
        return configurer;
    }
}
