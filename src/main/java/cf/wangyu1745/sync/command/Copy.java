package cf.wangyu1745.sync.command;

import cf.wangyu1745.sync.util.ItemStackUtil;
import cf.wangyu1745.sync.util.TEUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.minecraft.server.v1_12_R1.IInventory;
import net.minecraft.server.v1_12_R1.TileEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Copy implements CommandExecutor {
    public static final String COPY = "copy";
    public static final Map<UUID, Pair<Class<?>, byte[]>> map = new HashMap<>();
//    private long last;

    @SneakyThrows
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//        long now = System.currentTimeMillis();
//        if (now - last < 50) {
//            sender.sendMessage("命令使用太频繁");
//            return true;
//        }
//        last = System.currentTimeMillis();
        try {
            if (cmd.getName().equalsIgnoreCase(COPY) && sender instanceof Player) {
                Player p = (Player) sender;
                TileEntity tileEntity = TEUtil.getTE(p);
//                System.out.println("实现的接口");
//                Reflect.recursiveInterfaces(tileEntity.getClass()).forEach(System.out::println);
                IInventory inv;
                if (tileEntity instanceof IInventory) {
                    inv = (IInventory) tileEntity;
                } else {
                    return true;
                }
//                System.out.println("inv.getSize() = " + inv.getSize());
//                for (int i = 0; i < inv.getSize(); i++) {
//                    ItemStack item = inv.getItem(i);
//                    if (CraftItemStack.asBukkitCopy(item).getType() != Material.AIR) {
//                        System.out.println(i + ": " + item.getItem().getName());
//                    }
//                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                ItemStackUtil.saveOrdered(inv, dataOutputStream);
                map.put(p.getUniqueId(), Pair.of(tileEntity.getClass(), byteArrayOutputStream.toByteArray()));
                org.bukkit.inventory.ItemStack stick = new org.bukkit.inventory.ItemStack(Material.STICK);
                ItemMeta itemMeta = stick.getItemMeta();
                itemMeta.setDisplayName("复制棒");
                stick.setItemMeta(itemMeta);
                p.sendMessage("你已获得复制棒");
                p.getInventory().addItem(stick);
//                switch (args[0]) {
//                    case "c": {
//                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
//                        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
//                        ItemStackUtil.saveOrdered(inv, dataOutputStream);
//                        map.put(p.getUniqueId(), Pair.of(tileEntity.getClass(), byteArrayOutputStream.toByteArray()));
//                        org.bukkit.inventory.ItemStack stick = new org.bukkit.inventory.ItemStack(Material.STICK);
//                        ItemMeta itemMeta = stick.getItemMeta();
//                        itemMeta.setDisplayName("复制棒");
//                        stick.setItemMeta(itemMeta);
//                        p.sendMessage("你已获得复制棒");
//                        p.getInventory().addItem(stick);
//                        break;
//                    }
//                    case "v": {
//                        Pair<Class<?>, byte[]> pair = map.get(p.getUniqueId());
//                        byte[] bytes = pair.getRight();
//                        if (bytes == null) {
//                            p.sendMessage("请先复制再粘贴");
//                            return true;
//                        }
//                        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes));
//                        // 复制模板
//                        ItemStack[] itemStacks = ItemStackUtil.loadOrdered(dataInputStream);
//                        PlayerInventory pInv = p.getInventory();
//                        // 玩家背包
//                        List<ItemStack> contents = Arrays.stream(pInv.getContents()).map(CraftItemStack::asNMSCopy).collect(Collectors.toList());
//                        for (int i = 0; i < itemStacks.length; i++) {
//                            ItemStack itemStack = itemStacks[i];
//                            if (itemStack == null) continue;
//                            for (int j = 0; j < contents.size(); j++) {
//                                ItemStack found = contents.get(j);
//                                if (found == null) continue;
//                                if (ItemStackUtil.equalsIgnoreCount(found, itemStack)) {
//                                    pInv.setItem(j, new org.bukkit.inventory.ItemStack(Material.AIR));
//                                    inv.setItem(i, found);
//                                    break;
//                                }
//                            }
//                        }
//                    }
//                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage("error");
            return true;
        }
    }
}
