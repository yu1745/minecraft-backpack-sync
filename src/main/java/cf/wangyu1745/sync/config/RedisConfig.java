package cf.wangyu1745.sync.config;

import cf.wangyu1745.sync.listener.GatherListener;
import lombok.var;
import org.bukkit.configuration.file.FileConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.RedisSerializer;

import static cf.wangyu1745.sync.Sync.VAULT_CHANNEL;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory(FileConfiguration config) {
        return new JedisConnectionFactory(new RedisStandaloneConfiguration(config.getString("redis.host"), config.getInt("redis.port")));
    }

    @Bean
    public RedisTemplate<String, String> redisTemplateSS(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.string());
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public RedisTemplate<String, byte[]> redisTemplateSB(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.byteArray());
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public RedisTemplate<String, Double> redisTemplateSD(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Double> template = new RedisTemplate<>();
        template.setKeySerializer(RedisSerializer.string());
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    /*@Bean
    public RedisTemplate<Object, byte[]> redisTemplateOB(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, byte[]> template = new RedisTemplate<>();
        template.setKeySerializer(RedisSerializer.java());
        template.setValueSerializer(RedisSerializer.byteArray());
        template.setConnectionFactory(connectionFactory);
        return template;
    }*/

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory, GatherListener gatherListener) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new MessageListenerAdapter(gatherListener,"onMessage"), new ChannelTopic(VAULT_CHANNEL));
        return container;
    }

}
