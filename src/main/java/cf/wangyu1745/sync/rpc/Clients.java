package cf.wangyu1745.sync.rpc;

import cf.wangyu1745.nettyRPC.Client;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.x.async.AsyncCuratorFramework;
import org.apache.curator.x.async.WatchMode;
import org.apache.zookeeper.AddWatchMode;
import org.apache.zookeeper.WatchedEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import static cf.wangyu1745.sync.Main.RPC_PATH;

@RequiredArgsConstructor
@Component
public class Clients {
    private final AsyncCuratorFramework zk;
    private final Map<ClientWrapper, Set<Object>> m = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final FileConfiguration config;
    private final Logger logger;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @PostConstruct
    public void post() {
        //noinspection Convert2Lambda
        zk.with(WatchMode.successOnly).addWatch().withMode(AddWatchMode.PERSISTENT_RECURSIVE).usingWatcher(new CuratorWatcher() {
            @Override
            public void process(WatchedEvent event) {
                logger.info(event.toString());
                switch (event.getType()) {
                    case NodeCreated: {
                        String[] split = event.getPath().split("/");
                        if (!split[split.length - 1].equals(config.getString("id"))) {
                            // 排除自己
                            zk.getData().forPath(event.getPath()).thenAcceptAsync(bytes -> {
                                try {
                                    ClientWrapper clientWrapper = mapper.readValue(bytes, ClientWrapper.class);
                                    clientWrapper.client = new Client(clientWrapper.host, clientWrapper.port, true);
                                    try {
                                        lock.writeLock().lock();
                                        m.keySet().stream().filter(clientWrapper::equals).findAny().ifPresent(o -> o.client.stop());
                                        m.put(clientWrapper, new HashSet<>());
                                        logger.info("除开自己之外的存活服务器:" + m.keySet());
                                    } finally {
                                        lock.writeLock().unlock();
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                        }
                        break;
                    }
                    case NodeDeleted: {
                        String[] split = event.getPath().split("/");
                        ClientWrapper toRemove = new ClientWrapper();
                        toRemove.id = split[split.length - 1];
                        try {
                            lock.writeLock().lock();
                            m.keySet().stream().filter(toRemove::equals).findAny().ifPresent(e -> e.client.stop());
                            m.remove(toRemove);
                        } finally {
                            lock.writeLock().unlock();
                        }
                        logger.info("除开自己之外的存活服务器:" + m.keySet());
                        break;
                    }
                }
            }

        }).forPath(RPC_PATH);
        //noinspection CodeBlock2Expr
        zk.getChildren().forPath(RPC_PATH).thenAcceptAsync(l -> {
            l.forEach(e -> zk.getData().forPath(RPC_PATH + "/" + e).thenAcceptAsync(bytes -> {
                try {
                    var clientWrapper = mapper.readValue(bytes, ClientWrapper.class);
                    clientWrapper.client = new Client(clientWrapper.host, clientWrapper.port, true);
                    try {
                        lock.writeLock().lock();
                        m.put(clientWrapper, new HashSet<>());
                    } finally {
                        lock.writeLock().unlock();
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }));
        });

    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    private static class ClientWrapper {
        String id;
        String host;
        int port;
        @JsonIgnore
        Client client;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientWrapper that = (ClientWrapper) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("{id='%s', host='%s', port=%d}", id, host, port);
        }
    }


    @AllArgsConstructor
    @Getter
    public static class ServiceWrapper<T> {
        final String serverId;
        final T service;
    }

    public synchronized <T> List<ServiceWrapper<T>> getServices(Class<T> c) {
        List<ServiceWrapper<T>> list = new ArrayList<>();
        try {
            lock.readLock().lock();
            m.forEach((k, v) -> {
                @SuppressWarnings("unchecked") Optional<T> any = (Optional<T>) v.stream().filter(e -> c.isAssignableFrom(e.getClass())).findAny();
                if (any.isPresent()) {
                    list.add(new ServiceWrapper<>(k.id, any.get()));
                } else {
                    T service = k.client.getService(c);
                    v.add(service);
                    list.add(new ServiceWrapper<>(k.id, service));
                }
            });
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

}
