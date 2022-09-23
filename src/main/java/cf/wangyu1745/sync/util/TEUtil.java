package cf.wangyu1745.sync.util;

import lombok.SneakyThrows;
import net.minecraft.server.v1_12_R1.TileEntity;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_12_R1.block.CraftBlock;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.EnumSet;

public class TEUtil {
    private static final Field field;

    static {
        try {
            field = CraftBlock.class.getDeclaredField("chunk");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    //todo 检查主线程
    @SneakyThrows
    public static TileEntity getTE(Player p) {
        Block block = p.getTargetBlock(EnumSet.of(Material.AIR, Material.WATER), 3);
        CraftChunk chunk = (CraftChunk) field.get(block);
        return chunk.getCraftWorld().getTileEntityAt(block.getX(), block.getY(), block.getZ());
    }
}
