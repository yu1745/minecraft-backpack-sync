package cf.wangyu1745.sync.listener;

import lombok.RequiredArgsConstructor;
import lombok.var;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.TileEntity;
import org.apache.commons.lang3.tuple.Triple;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class MoveHouseListener implements Listener {
    public static final String MOVE_STICK = "搬家棒";
    private final Map<Player, List<Location>> LocationsMap = new HashMap<>();
    private final Map<Player, Map<BlockPosition, Triple<Integer, Class<?>, NBTTagCompound>>> map = new HashMap<>();
    private final Map<Player, BlockPosition> baseMap = new HashMap<>();
    private final Logger logger;

    @EventHandler
    public void move(PlayerInteractEvent e) {
        try {
            if (e.getPlayer().isOp() && e.getItem() != null && MOVE_STICK.equals(e.getItem().getItemMeta().getDisplayName())) {
                Player p = e.getPlayer();
                World world = p.getWorld();
                switch (e.getAction()) {
                    case LEFT_CLICK_BLOCK: {
                        LocationsMap.computeIfPresent(p, (k, v) -> {
                            v.clear();
                            return v;
                        });
                        p.sendMessage("你已清除选取的点");
                        break;
                    }
                    case RIGHT_CLICK_BLOCK: {
                        List<Location> list = LocationsMap.compute(p, (k, v) -> {
                            if (v == null) {
                                return new ArrayList<>();
                            } else {
                                return v;
                            }
                        });
                        switch (list.size()) {
                            case 0: {
                                list.add(e.getClickedBlock().getLocation());
                                p.sendMessage("你已选择" + list.size() + "个点");
                                break;
                            }
                            case 1: {
                                // 复制
                                long start = System.nanoTime();
                                list.add(e.getClickedBlock().getLocation());
                                p.sendMessage("你已完成选取");
                                Map<BlockPosition, Triple<Integer, Class<?>, NBTTagCompound>> toBeCopyed = map.compute(p, (k, v) -> {
                                    if (v == null) {
                                        return new HashMap<>();
                                    } else {
                                        return v;
                                    }
                                });
                                toBeCopyed.clear();
                                Location a = list.get(0);
                                Location b = list.get(1);
                                // 交换xyz使得a的x,y,z小于b的x,y,z
                                if (a.getX() > b.getX()) {
                                    var temp = a.getX();
                                    a.setX(b.getX());
                                    b.setX(temp);
                                }
                                /*if (a.getY() > b.getY()) {
                                    var temp = a.getY();
                                    a.setY(b.getY());
                                    b.setY(temp);
                                }*/
                                if (a.getZ() > b.getZ()) {
                                    var temp = a.getZ();
                                    a.setZ(b.getZ());
                                    b.setZ(temp);
                                }
                                baseMap.put(p, new BlockPosition(a.getX(), a.getY(), a.getZ()));
                                for (int i = (int) a.getX(); i <= b.getX(); i++) {
                                    for (int j = 0; j <= 255; j++) {
                                        for (int k = a.getBlockZ(); k <= b.getZ(); k++) {
                                            Block block = world.getBlockAt(i, j, k);
                                            TileEntity te = ((CraftWorld) world).getTileEntityAt(block.getX(), block.getY(), block.getZ());
                                            //noinspection deprecation
                                            toBeCopyed.put(new BlockPosition(i, j, k), Triple.of(block.getTypeId(), te != null ? te.getClass() : null, te != null ? te.save(new NBTTagCompound()) : null));
                                        }
                                    }
                                }
                                logger.info("复制用时" + ((double) (System.nanoTime() - start) / 1000000) + "ms");
                                break;
                            }
                            default: {
                                // 粘贴
                                long start = System.nanoTime();
                                Block block = e.getClickedBlock();
                                BlockPosition offset_ = new BlockPosition(block.getX(), block.getY(), block.getZ()).b(baseMap.get(p));
                                BlockPosition offset = offset_.b(new BlockPosition(0,offset_.getY(),0));
                                Map<BlockPosition, Triple<Integer, Class<?>, NBTTagCompound>> toBePasted = map.get(p);
                                toBePasted.forEach((key, value) -> {
                                    try {
                                        BlockPosition blockPosition = key.a(offset);
                                        Block targetBlock = world.getBlockAt(blockPosition.getX(),blockPosition.getY(), blockPosition.getZ());
                                        //noinspection deprecation
                                        targetBlock.setTypeId(value.getLeft());
                                        if (value.getMiddle() != null) {
                                            TileEntity te = (TileEntity) value.getMiddle().newInstance();
                                            te.load(value.getRight());
//                                            te.a(((CraftWorld)world).getHandle());
//                                            te.A();
                                            ((CraftWorld)world).getHandle().setTileEntity(blockPosition,te);
//                                            ((CraftChunk) targetBlock.getChunk()).getHandle().a(new BlockPosition(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ()), te);
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                });
                                logger.info("粘贴用时" + ((double) (System.nanoTime() - start) / 1000000) + "ms");
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        } catch (NullPointerException ignored) {

        }
    }
}
