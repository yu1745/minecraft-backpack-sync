package cf.wangyu1745.sync.config;

import cf.wangyu1745.nettyRPC.Server;
import cf.wangyu1745.sync.service.IVaultService;
import cf.wangyu1745.sync.service.VaultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.RequiredArgsConstructor;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryForever;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Logger;

import static cf.wangyu1745.sync.Main.RPC_PATH;

@Configuration
@RequiredArgsConstructor
public class RpcConfig {
    private final Logger logger;
    private final FileConfiguration config;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    public Server server(AsyncCuratorFramework zk, VaultService vaultService) {
        Server server = new Server(0, config.getBoolean("debug"));
        ChannelFuture channelFuture = server.start();
        channelFuture.addListener((ChannelFutureListener) l -> {
            SocketAddress socketAddress = l.channel().localAddress();
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            logger.info("rpc server port:" + inetSocketAddress.getPort());
            boolean any = true;
            for (byte b : inetSocketAddress.getAddress().getAddress()) {
                if (b != 0) {
                    any = false;
                    break;
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();
            rootNode.put("id", config.getString("id"));
            rootNode.put("host", any ? "localhost" : inetSocketAddress.getHostString());
            rootNode.put("port", inetSocketAddress.getPort());
            byte[] bytes = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(rootNode);
            // 注册server
            zk.create().withMode(CreateMode.EPHEMERAL).forPath(RPC_PATH + "/" + config.getString("id"), bytes);
        });

        // 注册service
        server.register(IVaultService.class, vaultService);
        return server;
    }


    @Bean
    public AsyncCuratorFramework asyncCuratorFramework() {
        CuratorFramework client = CuratorFrameworkFactory.newClient(config.getString("zookeeper"), new RetryForever(1));
        client.start();
        return AsyncCuratorFramework.wrap(client);
    }


}
