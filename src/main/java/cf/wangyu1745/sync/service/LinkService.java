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
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class LinkService implements LifeCycle {
    private final TunnelMapper tunnelMapper;
    private final FileConfiguration config;
    private final BukkitScheduler scheduler;
    private final JavaPlugin plugin;
    private final RedisTemplate<String, byte[]> sb;
    private final Logger logger;
    private volatile boolean close = false;
    private final ExecutorService exe = Executors.newCachedThreadPool();
    private final CopyOnWriteArrayList<Tunnel> sendList = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Tunnel> receiveList = new CopyOnWriteArrayList<>();
    private final Queue<Pair<Tunnel, byte[]>> workQueue = new ArrayDeque<>();
    private final AtomicInteger i = new AtomicInteger(0);

    @SneakyThrows
    @Override
    public void onDisable() {
        close = true;
        // 等一秒
        CountDownLatch countDownLatch = new CountDownLatch(1);
        scheduler.runTaskLater(plugin, countDownLatch::countDown, 20);
        countDownLatch.await();
    }

    /**
     * 0---5(主线程执行发送逻辑)---10(异步刷新活跃的出和入隧道)---15(主线程执行接收逻辑)---20
     */
    @PostConstruct
    public void post() {
        // 异步的刷新隧道列表
        scheduler.runTaskAsynchronously(plugin, this::updateSendList);
        scheduler.runTaskAsynchronously(plugin, this::updateReceiveList);
    }

    private void updateSendList() {
        try {
            List<Tunnel> list = tunnelMapper.selectList(Wrappers.<Tunnel>lambdaQuery().eq(Tunnel::getFromServer, config.getString("id")).eq(Tunnel::getActive, true));
            sendList.clear();
            sendList.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (!close) {
                scheduler.runTaskLater(plugin, this::send, 20);
            }
            if (i.getAndIncrement() % 60 == 0) {
                logger.info("出方向激活隧道数:" + sendList.size());
            } else {
                logger.fine("出方向激活隧道数:" + sendList.size());
            }
        }
    }

    private void updateReceiveList() {
        try {
            List<Tunnel> list = tunnelMapper.selectList(Wrappers.<Tunnel>lambdaQuery().eq(Tunnel::getToServer, config.getString("id")).eq(Tunnel::getActive, true));
            receiveList.clear();
            receiveList.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (!close) {
                scheduler.runTaskAsynchronously(plugin, this::retrieve);
            }
            if (i.getAndIncrement() % 60 == 1) {
                logger.info("入方向激活隧道数:" + receiveList.size());
            } else {
                logger.fine("入方向激活隧道数:" + receiveList.size());
            }
        }
    }


    private void send() {
        var start = System.currentTimeMillis();
        final int[] real = {0};
        try {
            sendList.forEach(next -> {
                Location location = new Location(Bukkit.getWorld(next.getFromWorld()), next.getFromX(), next.getFromY(), next.getFromZ());
                Chunk chunk = location.getChunk();
                if (location.getWorld().isChunkLoaded(chunk)) {
                    //区块已经加载
                    BlockState state;
                    if ((state = location.getBlock().getState()) instanceof Chest) {
                        //从现在开始已经确定是一个箱子
                        Inventory inventory = ((Chest) state).getBlockInventory();
                        ItemStack[] contents = inventory.getContents();
                        long count = Arrays.stream(contents).filter(Objects::nonNull).filter(e -> e.getType() != Material.AIR).count();
                        logger.fine("[" + next.getId() + "] 发送物品数:" + count);
                        if (count != 0) {
                            real[0]++;
                            byte[] bytes = InventoryUtil.itemStacks2Bytes(contents);
                            inventory.clear();
                            exe.submit(() -> {
                                sb.opsForList().rightPush(String.valueOf(next.getId()), bytes);
                            });
                        }
                    } else {
                        logger.info("[" + next.getId() + "]号隧道源端箱子被破坏，关闭隧道");
                        exe.submit(() -> next.setActive(false).updateById());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            logger.fine("本轮进行了" + real[0] + "次传送,耗时" + (System.currentTimeMillis() - start) + "ms");
            if (!close) {
                scheduler.runTaskAsynchronously(plugin, this::updateSendList);
            }
        }
    }

    public void receive() {
        var start = System.currentTimeMillis();
        final int[] real = {0};
        try {
            for (int i = 0; i < workQueue.size(); i++) {
                if (i % 50 == 0 && System.currentTimeMillis() - start > 1) {
                    logger.fine("本轮进行了" + real[0] + "次传送,耗时" + (System.currentTimeMillis() - start) + "ms");
                    break;
                }
                final var poll = Objects.requireNonNull(workQueue.poll());
                final Location location = new Location(Bukkit.getWorld(poll.getLeft().getToWorld()), poll.getLeft().getToX(), poll.getLeft().getToY(), poll.getLeft().getToZ());
                final Chunk chunk = location.getChunk();
                if (location.getWorld().isChunkLoaded(chunk)) {
                    //区块已经加载
                    BlockState state;
                    if ((state = location.getBlock().getState()) instanceof Chest) {
                        //从现在开始已经确定是一个箱子
                        Inventory inventory = ((Chest) state).getBlockInventory();
                        //容器空余格子
                        int count = (int) Arrays.stream(inventory.getContents()).filter(Objects::nonNull).filter(e -> e.getType() != Material.AIR).count();
                        int len = InventoryUtil.lenOf(poll.getRight(), inventory.getSize() - count);
                        if (len == poll.getRight().length) {
                            //箱子剩余空间够大
                            ItemStack[] itemStacks = InventoryUtil.bytes2ItemStacks(poll.getRight());
                            logger.fine("[" + poll.getLeft().getId() + "] 接收物品数:" + itemStacks.length);
                            inventory.addItem(itemStacks);
                        } else {
                            //箱子剩余空间不够大，分割bytes
                            Pair<byte[], byte[]> split = ArrayUtil.split(poll.getRight(), len);
                            ItemStack[] itemStacks = InventoryUtil.bytes2ItemStacks(split.getLeft());
                            inventory.addItem(itemStacks);
                            // 然后把读剩下的bytes写回redis
                            exe.submit(() -> {
                                if (split.getRight().length > 0) {
                                    sb.opsForList().leftPush(String.valueOf(poll.getLeft().getId()), split.getRight());
                                }
                            });
                        }
                    } else {
                        // 不是箱子
                        logger.info("[" + poll.getLeft().getId() + "]号隧道落地箱子被破坏，关闭隧道");
                        exe.submit(() -> poll.getLeft().setActive(false).updateById());
                    }
                }
            }
            workQueue.forEach(e -> exe.submit(() -> sb.opsForList().leftPush(String.valueOf(e.getLeft().getId()), e.getRight())));
            workQueue.clear();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (!close) {
                scheduler.runTaskAsynchronously(plugin, this::updateReceiveList);
            }
        }
    }

    private void retrieve() {
        try {
            receiveList.parallelStream().forEach(e -> {
                for (int j = 0; j < 5; j++) {
                    byte[] bytes = sb.opsForList().leftPop(String.valueOf(e.getId()));
                    if (bytes != null && bytes.length > 0) {
                        workQueue.offer(Pair.of(e, bytes));
                    } else {
                        break;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (!close) {
                scheduler.runTaskLater(plugin, this::receive, 20);
            }
        }
    }

}
