package cf.wangyu1745.sync.listener;

import cf.wangyu1745.sync.util.ItemStackUtil;
import cf.wangyu1745.sync.util.TEUtil;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_12_R1.IInventory;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.TileEntity;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static cf.wangyu1745.sync.command.Copy.map;

@Component
@RequiredArgsConstructor
public class CopyListener implements Listener {
    public static final String COPY_STICK = "复制棒";

    @EventHandler
    public void copy(PlayerInteractEvent e) {
        try {
            if (e.getAction() == Action.RIGHT_CLICK_BLOCK && COPY_STICK.equals(e.getItem().getItemMeta().getDisplayName())) {
                Player p = e.getPlayer();
                Pair<Class<?>, byte[]> pair = map.get(p.getUniqueId());
                if (pair == null) {
                    p.sendMessage("请先复制再粘贴");
                    return;
                }
                e.setCancelled(true);
                TileEntity te = TEUtil.getTE(p);
                IInventory inv = (IInventory) te;
                if (pair.getLeft().isAssignableFrom(te.getClass())) {
                    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(pair.getRight()));
                    // 复制模板
                    ItemStack[] itemStacks = ItemStackUtil.loadOrdered(dataInputStream);
                    PlayerInventory pInv = p.getInventory();
                    // 玩家背包
                    List<ItemStack> contents = Arrays.stream(pInv.getContents()).map(CraftItemStack::asNMSCopy).collect(Collectors.toList());
                    for (int i = 0; i < itemStacks.length; i++) {
                        // 要复制的物品
                        ItemStack itemStack = itemStacks[i];
                        if (itemStack == null) continue;
                        for (int j = 0; j < contents.size(); j++) {
                            ItemStack found = contents.get(j);
                            if (found == null) continue;
                            if (ItemStackUtil.equalsIgnoreCount(found, itemStack)) {
                                if (pInv.getItem(j).getAmount() > itemStack.getCount()) {
                                    pInv.getItem(j).setAmount(pInv.getItem(j).getAmount() - itemStack.getCount());
                                    inv.setItem(i, itemStack);
                                    break;
                                } else if (pInv.getItem(j).getAmount() == itemStack.getCount()) {
                                    pInv.setItem(j, new org.bukkit.inventory.ItemStack(Material.AIR));
                                    inv.setItem(i, itemStack);
                                    break;
                                } else {
                                    p.sendMessage("物品不足");
                                    break;
                                }
                            }
                            if (j == contents.size()) {
                                p.sendMessage("物品不足");
                            }
                        }
                    }
                }
            }
        } catch (NullPointerException ignored) {
        }
    }

}
