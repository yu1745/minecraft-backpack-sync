package cf.wangyu1745.sync.util;

import cf.wangyu1745.sync.Main;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.var;
import net.minecraft.server.v1_12_R1.IInventory;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;


@SuppressWarnings("unused")
@Component
@RequiredArgsConstructor
public class ItemStackUtil {
    public static boolean equalsIgnoreCountAndNBT(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return true;
        }
        if (!a.isEmpty() && !b.isEmpty()) {
            if ((a.getTag() != null) || (b.getTag() == null)) {
                if (a.getTag() == null) {
                    return true;
                }
                if (b.getTag() != null) {
                    try {
                        NBTTagCompound aTag = a.save(new NBTTagCompound());
                        NBTTagCompound bTag = b.save(new NBTTagCompound());
                        aTag.remove("Count");
//                        aTag.set("tag", new NBTTagCompound());
                        aTag.remove("tag");
                        bTag.remove("Count");
                        bTag.remove("tag");
//                        bTag.set("tag", new NBTTagCompound());
                        return aTag.equals(bTag);
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public static boolean equalsIgnoreCount(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return true;
        }
        if (!a.isEmpty() && !b.isEmpty()) {
            /*if ((a.getTag() != null) || (b.getTag() == null)) {
                if (a.getTag() == null) {
                    return true;
                }
                if (b.getTag() != null) {
                    try {
                        NBTTagCompound aTag = a.save(new NBTTagCompound());
                        NBTTagCompound bTag = b.save(new NBTTagCompound());
                        aTag.remove("Count");
                        bTag.remove("Count");
                        return aTag.equals(bTag);
                    } catch (Exception e) {
                        return false;
                    }
                }
            }*/
            NBTTagCompound aTag = a.save(new NBTTagCompound());
            NBTTagCompound bTag = b.save(new NBTTagCompound());
            aTag.remove("Count");
            bTag.remove("Count");
            return aTag.equals(bTag);
        }
        return false;
    }

    /*public static void save(ItemStack itemStack, DataOutput dataOutput) {
        try {
            NBTTagCompound nbtTagCompound = itemStack.save(new NBTTagCompound());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            Main.write.invoke(nbtTagCompound, dataOutputStream);
            dataOutput.writeInt(dataOutputStream.size());
//            dataOutput.writeByte(itemStack.getCount());
            dataOutput.write(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    /**
     * 一个int的位置 一个int的长度 然后是data
     *
     * @param inv        要序列化的inv
     * @param dataOutput 序列化目标
     */
    public static void saveOrdered(IInventory inv, DataOutput dataOutput) {
        for (int i = 0; i < inv.getSize(); i++) {
            try {
                if (CraftItemStack.asBukkitCopy(inv.getItem(i)).getType() == Material.AIR) continue;
                var itemStack = inv.getItem(i);
                NBTTagCompound nbtTagCompound = itemStack.save(new NBTTagCompound());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                Main.write.invoke(nbtTagCompound, dataOutputStream);
                dataOutput.writeInt(i);
                dataOutput.writeInt(dataOutputStream.size());
                dataOutput.write(byteArrayOutputStream.toByteArray());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveOrdered(ItemStack[] itemStacks, DataOutput dataOutput) {
        for (int i = 0; i < itemStacks.length; i++) {
            ItemStack itemStack = itemStacks[i];
            try {
                NBTTagCompound nbtTagCompound = itemStack.save(new NBTTagCompound());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
                Main.write.invoke(nbtTagCompound, dataOutputStream);
                dataOutput.writeInt(i);
                dataOutput.writeInt(dataOutputStream.size());
                dataOutput.write(byteArrayOutputStream.toByteArray());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveOrdered(List<ItemStack> itemStacks, DataOutput dataOutput) {
        saveOrdered(itemStacks.toArray(new ItemStack[0]), dataOutput);
    }


    public static ItemStack[] loadOrdered(DataInput dataInput) {
//        ArrayList<ItemStack> itemStacks = new ArrayList<>(60);
        //todo 复用
        ItemStack[] itemStacks = new ItemStack[1024];
        while (true) {
            try {
                int index = dataInput.readInt();
                int len = dataInput.readInt();
//            byte count = dataInput.readByte();
                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                Main.load.invoke(nbtTagCompound, dataInput, 4, new NBTReadLimiter(2097152));
                itemStacks[index] = new ItemStack(nbtTagCompound);
            } catch (Exception e) {
                return itemStacks;
            }
        }
    }

    @SneakyThrows
    public static org.bukkit.inventory.ItemStack bytes2itemStack(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new org.bukkit.inventory.ItemStack(Material.AIR);
        }
        NBTTagCompound nbt = new NBTTagCompound();
        Main.load.invoke(nbt, new DataInputStream(new ByteArrayInputStream(bytes)), 4, new NBTReadLimiter(2097152));
        return CraftItemStack.asBukkitCopy(new ItemStack(nbt));
    }
}
