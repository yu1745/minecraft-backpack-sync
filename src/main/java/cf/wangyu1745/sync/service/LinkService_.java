/*
package cf.wangyu1745.sync.service;

import cf.wangyu1745.sync.entity.Tunnel;
import cf.wangyu1745.sync.mapper.TunnelMapper;
import cf.wangyu1745.sync.util.ArrayUtil;
import cf.wangyu1745.sync.util.InventoryUtil;
import cf.wangyu1745.sync.util.LifeCycle;
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
import org.springframework.data.redis.core.RedisTemplate;
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
public class LinkService_ implements LifeCycle {
    private final TunnelMapper tunnelMapper;
    private final FileConfiguration config;
    private final BukkitScheduler scheduler;
    private final JavaPlugin plugin;
    private final RedisTemplate<String, byte[]> sb;
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
                    } catch (InterruptedException | ExecutionException e) {
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
                    } catch (InterruptedException | ExecutionException e) {
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
        return CompletableFuture.supplyAsync(() -> tunnelMapper.selectList(Wrappers.<Tunnel>lambdaQuery().eq(Tunnel::getActive, true).eq(Tunnel::getFromServer, config.getString("id")))).thenApplyAsync(l -> l.stream().map(e -> Pair.of(e, new Location(Bukkit.getWorld(e.getFromWorld()), e.getFromX(), e.getFromY(), e.getFromZ()))).filter(e -> e.getRight().getChunk().isLoaded()).collect(Collectors.toList())).thenAcceptAsync(l -> {
            var q = new ConcurrentLinkedQueue<>(l);
            var real = new AtomicInteger();
            while (!q.isEmpty()) {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                scheduler.runTask(plugin, () -> {
                    try {
                        var start = System.nanoTime();
                        for (int i = 0; ; i++) {
                            if (i % 50 == 0 && System.nanoTime() - start > 1000000) {
                                return;
                            }
                            var poll = q.poll();
                            if (poll != null) {
                                if (poll.getRight().getChunk().isLoaded()) {
                                    var state = poll.getRight().getBlock().getState();
                                    if (state instanceof Chest) {
                                        // 从现在开始已经确定是一个箱子
                                        Inventory inventory = ((Chest) state).getBlockInventory();
                                        ItemStack[] contents = inventory.getContents();
                                        // 有物品的格子数
                                        long count = Arrays.stream(contents).filter(Objects::nonNull).filter(e -> e.getType() != Material.AIR).count();
                                        if (count != 0) {
                                            real.incrementAndGet();
                                            byte[] bytes = InventoryUtil.itemStacks2Bytes(contents);
                                            inventory.clear();
                                            CompletableFuture.runAsync(() -> sb.opsForList().rightPush(String.valueOf(poll.getLeft().getId()), bytes));
                                        }
                                    }
                                }
                            } else {
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
        return CompletableFuture.supplyAsync(() -> tunnelMapper.selectList(Wrappers.<Tunnel>lambdaQuery().eq(Tunnel::getToServer, config.getString("id")).eq(Tunnel::getActive, true))).thenApplyAsync(l -> l.stream()*/
/*.map(e->Pair.of(e,new Location(Bukkit.getWorld(e.getToWorld()), e.getToX(), e.getToY(), e.getToZ())))*//*
.map(e -> {
            var temp = new ConcurrentLinkedQueue<byte[]>();
            for (int i = 0; i < 3; i++) {
                byte[] bytes = sb.opsForList().leftPop(String.valueOf(e.getId()));
                if (bytes != null) {
                    temp.offer(bytes);
                }
            }
            return Pair.of(e, temp);
        }).collect(Collectors.toList())).thenAcceptAsync(l -> {
            var q = new ConcurrentLinkedQueue<>(l);
            while (!q.isEmpty()) {
                CountDownLatch countDownLatch = new CountDownLatch(1);
                scheduler.runTask(plugin, () -> {
                    try {
                        var start = System.nanoTime();
                        for (int i = 0; ; i++) {
                            if (i % 50 == 0 && (System.nanoTime() - start) > 1000000) {
                                return;
                            }
                            var poll = q.poll();
                            if (poll != null) {
                                Location location = new Location(Bukkit.getWorld(poll.getLeft().getToWorld()), poll.getLeft().getToX(), poll.getLeft().getToY(), poll.getLeft().getToZ());
                                if (location.getChunk().isLoaded()) {
                                    var state = location.getBlock().getState();
                                    if (state instanceof Chest) {
                                        //从现在开始已经确定是一个箱子
                                        Inventory inventory = ((Chest) state).getBlockInventory();
                                        //容器已经占用的格子
                                        var queue = poll.getRight();
                                        while (!queue.isEmpty()) {
                                            byte[] bytes = queue.poll();
                                            int count = (int) Arrays.stream(inventory.getContents()).filter(Objects::nonNull).filter(e -> e.getType() != Material.AIR).count();
                                            int len = InventoryUtil.lenOf(bytes, inventory.getSize() - count);
                                            assert bytes != null;
                                            if (len == bytes.length) {
                                                //箱子剩余空间够大
                                                ItemStack[] itemStacks = InventoryUtil.bytes2ItemStacks(bytes);
                                                logger.fine("[" + poll.getLeft().getId() + "] 接收物品数:" + itemStacks.length);
                                                inventory.addItem(itemStacks);
                                            } else {
                                                //箱子剩余空间不够大，分割bytes
                                                Pair<byte[], byte[]> split = ArrayUtil.split(bytes, len);
                                                ItemStack[] itemStacks = InventoryUtil.bytes2ItemStacks(split.getLeft());
                                                inventory.addItem(itemStacks);
                                                // 然后把读剩下的bytes写回redis
                                                CompletableFuture.runAsync(() -> {
                                                    if (split.getRight().length > 0) {
                                                        sb.opsForList().leftPush(String.valueOf(poll.getLeft().getId()), split.getRight());
                                                    }
                                                });
                                                break;
                                            }
                                        }
                                        CompletableFuture.runAsync(() -> queue.forEach(e -> sb.opsForList().leftPush(String.valueOf(poll.getLeft().getId()), e)));
                                    }
                                }
                            } else {
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
        });
    }
}
*/
