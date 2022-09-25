package cf.wangyu1745.sync.command;

import cf.wangyu1745.sync.entity.KitLog;
import cf.wangyu1745.sync.mapper.KitLogMapper;
import cf.wangyu1745.sync.mapper.KitMapper;
import cf.wangyu1745.sync.util.ItemStackUtil;
import cf.wangyu1745.sync.util.PlayerUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.var;
import net.minecraft.server.v1_12_R1.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

//@Component(Kit.KIT)
@RequiredArgsConstructor
public class Kit implements CommandExecutor {
    public static final String KIT = "kit";
    public static final String USAGE = "/kit create 礼包名字 冷却    创建礼包,冷却为0即为一次性礼包\n" +
            "/kit remove 礼包名字    删除礼包\n" +
            "/kit view 礼包名字     查看礼包内容\n" +
            "/kit reload    重载(实际没啥用,不会真有人不通过指令,直接操作数据添加礼包吧)\n" +
            "/kit list    列出所有礼包\n" +
            "/kit give 礼包名字 玩家名字    给x玩家y礼包,如果在冷却玩家不会获得礼包\n" +
            "/kit force_give 礼包名字 玩家名字    强制给x玩家y礼包,即使在冷却也会获得礼包,冷却会从重新计算\n" +
            "/kit help    打印用法";
    public static final Map<String, Inventory> map = new HashMap<>();
    private final KitMapper kitMapper;
    private final KitLogMapper kitLogMapper;
    private final BukkitScheduler scheduler;
    private final JavaPlugin plugin;
    private List<cf.wangyu1745.sync.entity.Kit> kitList;
    private CompletableFuture<Void> wait = CompletableFuture.completedFuture(null);

    private enum SubCommand {
        CREATE, FORCE_GIVE, GIVE, HELP, LIST, RELOAD, REMOVE, VIEW
    }

    @PostConstruct
    private void reload() {
        map.clear();
        kitList = kitMapper.selectList(null);
        kitList.forEach(e -> {
            var dataInput = new DataInputStream(new ByteArrayInputStream(e.getData()));
            ItemStack[] itemStacks = ItemStackUtil.loadOrdered(dataInput);
            Inventory inventory = Bukkit.createInventory(HOLDER, 54, e.getName());
            org.bukkit.inventory.ItemStack[] bukkitStacks = new org.bukkit.inventory.ItemStack[Math.min(54, itemStacks.length)];
            for (int i = 0; i < bukkitStacks.length; i++) {
                if (itemStacks[i] != null) {
                    bukkitStacks[i] = CraftItemStack.asBukkitCopy(itemStacks[i]);
                } else {
                    bukkitStacks[i] = new org.bukkit.inventory.ItemStack(Material.AIR);
                }
            }
            inventory.setContents(bukkitStacks);
            map.put(e.getName(), inventory);
        });
    }


    public static class InvHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final InvHolder HOLDER = new InvHolder();


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player && cmd.getName().equalsIgnoreCase(KIT)) {
            var p = (Player) sender;
            if (args.length == 0) {
                return false;
            }
            try {
                switch (SubCommand.valueOf(args[0].toUpperCase())) {
                    case VIEW: {
                        if (args.length != 2) {
                            p.sendMessage("/kit view 礼包名称");
                            return true;
                        }
                        if (map.get(args[1]) != null) {
                            p.openInventory(map.get(args[1]));
                        } else {
                            p.sendMessage("礼包不存在");
                        }
                        break;
                    }
                    case CREATE: {
                        if (args.length == 2) {
                            cf.wangyu1745.sync.entity.Kit.builder().data(PlayerUtil.toBytesOrdered(p)).cooldown(0).name(args[1]).build().insert();
                            reload();
                        } else if (args.length == 3) {
                            cf.wangyu1745.sync.entity.Kit.builder().data(PlayerUtil.toBytesOrdered(p)).cooldown(Integer.parseInt(args[2])).name(args[1]).build().insert();
                            reload();
                        } else {
                            p.sendMessage("/kit create 礼包名称 冷却时间");
                            return true;
                        }
                        break;
                    }
                    case RELOAD: {
                        reload();
                        break;
                    }
                    case LIST: {
                        p.sendMessage(kitList.stream().map(cf.wangyu1745.sync.entity.Kit::getName).collect(Collectors.joining(",")));
                        break;
                    }
                    case REMOVE: {
                        if (args.length != 2) {
                            p.sendMessage("/kit remove 礼包名称");
                            return true;
                        }
                        Optional<cf.wangyu1745.sync.entity.Kit> first = kitList.stream().filter(e -> e.getName().equals(args[1])).findFirst();
                        if (first.isPresent()) {
                            first.get().deleteById();
                            reload();
                        } else {
                            p.sendMessage("礼包不存在");
                        }
                        break;
                    }
                    case GIVE: {
                        if (!wait.isDone()) {
                            // 等待领取记录入库之后才能进行下一次领取
                            return true;
                        }
                        if (args.length != 3) {
                        p.sendMessage("/kit give 礼包名字 玩家名字");
                        return true;
                    }
                    Optional<cf.wangyu1745.sync.entity.Kit> any = kitList.stream().filter(e -> e.getName().equals(args[1])).findAny();
                    if (!any.isPresent()) {
                        p.sendMessage("礼包不存在");
                        return true;
                    }
                    org.bukkit.inventory.ItemStack[] itemStacks = Arrays.stream(map.get(args[1]).getContents()).filter(Objects::nonNull).map(org.bukkit.inventory.ItemStack::clone).toArray(org.bukkit.inventory.ItemStack[]::new);
                        var dst = Bukkit.getPlayer(args[2]);
                        if (dst == null) {
                            p.sendMessage("玩家不存在");
                            return true;
                        }
                        CompletableFuture.runAsync(() -> {
                            KitLog kitLog = kitLogMapper.selectOne(Wrappers.<KitLog>lambdaQuery().eq(KitLog::getUsername, p.getName()).eq(KitLog::getKitId, any.get().getId()).orderBy(true,false,KitLog::getTime).last("limit 1"));
                            if (kitLog == null) {
                                scheduler.runTask(plugin, () -> {
                                    long leftSpace = Arrays.stream(p.getInventory().getStorageContents()).filter(Objects::nonNull).filter(e -> e.getType() != Material.AIR).count();
                                    if (leftSpace < itemStacks.length) {
                                        p.sendMessage("背包空间不足");
                                    } else {
                                        p.getInventory().addItem(itemStacks);
                                        wait = CompletableFuture.runAsync(() -> KitLog.builder().kitId(any.get().getId()).time(LocalDateTime.now()).username(p.getName()).build().insert());
                                    }
                                });
                                return;
                            }
                            if (any.get().getCooldown() == 0) {
                                // 冷却为0的是一次性礼包
                                return;
                            }
                            LocalDateTime now = LocalDateTime.now();
                            long between = ChronoUnit.SECONDS.between(kitLog.getTime(), now);
//                            "距%s上次领取礼包%s已经过去了%d天"
                            if (between > any.get().getCooldown()) {
                                scheduler.runTask(plugin, () -> {
                                    long leftSpace = Arrays.stream(p.getInventory().getStorageContents()).filter(Objects::nonNull).filter(e -> e.getType() != Material.AIR).count();
                                    if (leftSpace < itemStacks.length) {
                                        p.sendMessage("背包空间不足");
                                    } else {
                                        p.getInventory().addItem(itemStacks);
                                        wait = CompletableFuture.runAsync(() -> KitLog.builder().kitId(any.get().getId()).time(LocalDateTime.now()).username(p.getName()).build().insert());
                                    }
                                });
                            }
                        });
                        break;
                    }
                    case FORCE_GIVE: {
                        if (args.length != 3) {
                            p.sendMessage("/kit force_give 礼包名字 玩家名字");
                            return true;
                        }
                        Optional<cf.wangyu1745.sync.entity.Kit> any = kitList.stream().filter(e -> e.getName().equals(args[1])).findAny();
                        if (!any.isPresent()) {
                            p.sendMessage("礼包不存在");
                            return true;
                        }
                        org.bukkit.inventory.ItemStack[] itemStacks = Arrays.stream(map.get(args[1]).getContents()).filter(Objects::nonNull).map(org.bukkit.inventory.ItemStack::clone).filter(e -> e.getType() != Material.AIR).toArray(org.bukkit.inventory.ItemStack[]::new);
                        var dst = Bukkit.getPlayer(args[2]);
                        if (dst == null) {
                            p.sendMessage("玩家不存在");
                            return true;
                        }
                        long leftSpace = Arrays.stream(p.getInventory().getStorageContents()).filter(Objects::nonNull).filter(e -> e.getType() != Material.AIR).count();
                        if (leftSpace < itemStacks.length) {
                            p.sendMessage("背包空间不足");
                            return true;
                        } else {
                            p.getInventory().addItem(itemStacks);
                            CompletableFuture.runAsync(() -> KitLog.builder().kitId(any.get().getId()).time(LocalDateTime.now()).username(p.getName()).build().insert());
                        }
                        break;
                    }
                    case HELP: {
                        p.sendMessage(USAGE);
                    }
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(USAGE);
                return true;
            } catch (Exception e) {
                sender.sendMessage("内部错误");
                e.printStackTrace();
                return true;
            }
        }
        return true;
    }
}
