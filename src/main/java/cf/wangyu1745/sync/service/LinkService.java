package cf.wangyu1745.sync.service;

import cf.wangyu1745.sync.LifeCycle;
import cf.wangyu1745.sync.entity.TunnelData;
import cf.wangyu1745.sync.entity.TunnelInfo;
import cf.wangyu1745.sync.mapper.TunnelInfoMapper;
import cf.wangyu1745.sync.mapper.TunnelDataMapper;
import cf.wangyu1745.sync.util.ItemStackUtil;
import cf.wangyu1745.sync.util.PlayerInventoryUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.var;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LinkService implements LifeCycle {
    private final TunnelInfoMapper tunnelInfoMapper;
    private final TunnelDataMapper tunnelDataMapper;
    private final FileConfiguration config;
    private final BukkitScheduler scheduler;
    private final JavaPlugin plugin;
    //    private final RedisTemplate<String, byte[]> sb;
    private final Logger logger;
    private volatile boolean close = false;
    private final CountDownLatch closeWait = new CountDownLatch(2);

    @SneakyThrows
    @Override
    public void onDisable() {
        close = true;
        closeWait.await();
    }

    @PostConstruct
    public void post() {
        CompletableFuture.runAsync(() -> {
            while (true) {
                if (!close) {
                    try {
                        send().get();
                        TimeUnit.SECONDS.sleep(1);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        closeWait.countDown();
                    }
                } else {
                    closeWait.countDown();
                    return;
                }
            }
        });
        CompletableFuture.runAsync(() -> {
            while (true) {
                if (!close) {
                    try {
                        receive().get();
                        TimeUnit.SECONDS.sleep(1);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        closeWait.countDown();
                    }
                } else {
                    closeWait.countDown();
                    return;
                }
            }
        });
    }

    public CompletableFuture<Void> send() {
        return CompletableFuture.supplyAsync(() -> tunnelInfoMapper.selectList(Wrappers.<TunnelInfo>lambdaQuery().eq(TunnelInfo::getActive, true).eq(TunnelInfo::getFromServer, config.getString("id")))).thenApplyAsync(l -> l.stream().map(e -> Pair.of(e, new Location(Bukkit.getWorld(e.getFromWorld()), e.getFromX(), e.getFromY(), e.getFromZ()))).collect(Collectors.toList())).thenAcceptAsync(l -> {
            var q = new ConcurrentLinkedQueue<>(l);
            var real = new AtomicInteger();
            while (!q.isEmpty()) {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                scheduler.runTask(plugin, () -> {
                    try {
                        var start = System.nanoTime();
                        for (int i = 0; ; i++) {
                            // 1ms
                            if (i % 50 == 0 && System.nanoTime() - start > 1000000) {
                                return;
                            }
                            var poll = q.poll();
                            if (poll != null) {
                                TunnelInfo tunnelInfo = poll.getLeft();
                                Location location = poll.getRight();
                                if (location.getChunk().isLoaded()) {
                                    // 区块加载
                                    var state = location.getBlock().getState();
                                    if (state instanceof Chest) {
                                        // 从现在开始已经确定是一个箱子
                                        Inventory inventory = ((Chest) state).getBlockInventory();
                                        ItemStack[] contents = inventory.getContents();
                                        // 有物品的格子数
                                        long count = Arrays.stream(contents).filter(Objects::nonNull).filter(e -> e.getType() != Material.AIR).count();
                                        if (count != 0) {
                                            logger.fine(String.format("[%d]发送物品数:%d", tunnelInfo.getId(), count));
                                            real.incrementAndGet();
                                            var bytesList = PlayerInventoryUtil.itemStacks2BytesList(contents);
                                            inventory.clear();
                                            CompletableFuture.runAsync(() -> {
                                                bytesList.parallelStream().forEach(bytes -> TunnelData.builder().tunnelId(tunnelInfo.getId()).data(bytes).build().insert());
//                                                        sb.opsForList().rightPush(String.valueOf(tunnelInfo.getId()), bytes);
                                            });
                                        }
                                    } else {
                                        logger.info(String.format("[%d]入口被破坏", tunnelInfo.getId()));
                                        closeTunnel(tunnelInfo);
                                    }
                                }
                            } else {
                                // 本轮结束
                                return;
                            }
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                });
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            logger.fine("本轮进行了" + real.get() + "次发送");
        });
    }

    public CompletableFuture<Void> receive() {
        return CompletableFuture.supplyAsync(() -> tunnelInfoMapper.selectList(Wrappers.<TunnelInfo>lambdaQuery().eq(TunnelInfo::getToServer, config.getString("id")).eq(TunnelInfo::getActive, true))).thenApplyAsync(l -> l.stream()
//                        .map(e->Pair.of(e,new Location(Bukkit.getWorld(e.getToWorld()), e.getToX(), e.getToY(), e.getToZ())))
                .map(e -> {
                    var temp = tunnelDataMapper.getTunnelById(e.getId()).stream().map(TunnelData::getData).collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
                    return Pair.of(e, temp);
                }).collect(Collectors.toList())).thenAcceptAsync(l -> {
            var q = new ConcurrentLinkedQueue<>(l);
            var real = new AtomicInteger();
            while (!q.isEmpty()) {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                scheduler.runTask(plugin, () -> {
                    try {
                        var start = System.nanoTime();
                        for (int i = 0; ; i++) {
                            // 1ms
                            if (i % 50 == 0 && (System.nanoTime() - start) > 1000000) {
                                return;
                            }
                            var poll = q.poll();
                            if (poll != null) {
                                real.incrementAndGet();
                                TunnelInfo tunnelInfo = poll.getLeft();
                                ConcurrentLinkedQueue<byte[]> queue = poll.getRight();
                                Location location = new Location(Bukkit.getWorld(tunnelInfo.getToWorld()), tunnelInfo.getToX(), tunnelInfo.getToY(), tunnelInfo.getToZ());
                                if (location.getChunk().isLoaded()) {
                                    // 区块已经加载
                                    var state = location.getBlock().getState();
                                    if (state instanceof Chest) {
                                        //从现在开始已经确定是一个箱子
                                        Inventory inventory = ((Chest) state).getBlockInventory();
                                        //容器已经占用的格子
                                        while (!queue.isEmpty()) {
//                                            byte[] bytes = queue.poll();
                                            var left = 27 - Arrays.stream(inventory.getContents()).filter(Objects::nonNull).filter(e -> e.getType() != Material.AIR).count();
                                            for (int j = 0; j < left; j++) {
                                                inventory.addItem(ItemStackUtil.bytes2itemStack(queue.poll()));
                                            }
                                            CompletableFuture.runAsync(() -> queue.forEach(bytes1 -> TunnelData.builder().data(bytes1).tunnelId(tunnelInfo.getId()).build().insert()));
                                        }
                                    } else {
                                        logger.info(String.format("[%d]出口被破坏", tunnelInfo.getId()));
                                        closeTunnel(tunnelInfo);
                                    }
                                }
                            } else {
                                // 本轮结束
                                return;
                            }
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                });
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            logger.fine("本轮进行了" + real.get() + "次接收");
        });
    }

    private void closeTunnel(TunnelInfo t) {
        CompletableFuture.runAsync(() -> {
            t.setActive(false);
            t.updateById();
        });
    }
}
