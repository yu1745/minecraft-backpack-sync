package cf.wangyu1745.sync.command;

import cf.wangyu1745.sync.util.ItemStackUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.minecraft.server.v1_12_R1.IInventory;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.TileEntity;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_12_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Copy implements CommandExecutor {
    private final Map<UUID, byte[]> map = new HashMap<>();
    private long last;

    @SneakyThrows
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        long now = System.currentTimeMillis();
        if (now - last < 50) {
            sender.sendMessage("命令使用太频繁");
            return true;
        }
        last = System.currentTimeMillis();
        try {
            if (cmd.getName().equalsIgnoreCase("cp") && sender instanceof Player) {
                Player p = (Player) sender;
                Block block = ((Player) sender).getTargetBlock(EnumSet.of(Material.AIR), 2);
                Field field = CraftBlock.class.getDeclaredField("chunk");
                field.setAccessible(true);
                CraftChunk chunk = (CraftChunk) field.get(block);
                TileEntity tileEntity = chunk.getCraftWorld().getTileEntityAt(block.getX(), block.getY(), block.getZ());
                IInventory inv;
                if (tileEntity instanceof IInventory) {
                    inv = (IInventory) tileEntity;
                } else {
                    return true;
                }
                if ("c".equals(args[0])) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                    DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                    for (int i = 0; i < inv.getSize(); i++) {
                        if (CraftItemStack.asBukkitCopy(inv.getItem(i)).getType() == Material.AIR) continue;
                        ItemStackUtil.save(inv.getItem(i), dataOutputStream);
                    }
                    map.put(p.getUniqueId(), byteArrayOutputStream.toByteArray());
                } else if ("v".equals(args[0])) {
                    DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(map.get(p.getUniqueId())));
                    ItemStack[] itemStacks = ItemStackUtil.load(dataInputStream);
                    PlayerInventory pInv = p.getInventory();
                    for (ItemStack itemStack : itemStacks) {
                        //todo 还未完工
                    }
                } else {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage("error");
            return true;
        }
    }
}
