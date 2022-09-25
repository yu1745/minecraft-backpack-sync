package cf.wangyu1745.sync.util;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.var;
import net.minecraft.server.v1_12_R1.IInventory;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


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


    public static void saveOrdered(IInventory inv, DataOutput dataOutput) {
        ItemStack[] itemStacks = inv.getContents().toArray(new ItemStack[0]);
        saveOrdered(itemStacks, dataOutput);
    }

    public static void saveOrdered(List<ItemStack> itemStacks, DataOutput dataOutput) {
        saveOrdered(itemStacks.toArray(new ItemStack[0]), dataOutput);
    }

    /**
     * 一个int的位置 一个int的长度 然后是data
     */
    public static void saveOrdered(ItemStack[] itemStacks, DataOutput dataOutput) {
        for (int i = 0; i < itemStacks.length; i++) {
            ItemStack itemStack = itemStacks[i];
            try {
                NBTTagCompound nbtTagCompound = itemStack.save(new NBTTagCompound());
                byte[] bytes = NBTUtil.toBytes(nbtTagCompound);
                dataOutput.writeInt(i);
                dataOutput.writeInt(bytes.length);
                dataOutput.write(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static ItemStack[] loadOrdered(DataInput dataInput) {
//        ArrayList<ItemStack> itemStacks = new ArrayList<>(60);
        //todo 复用
        ItemStack[] itemStacks = new ItemStack[1024];
        while (true) {
            try {
                int index = dataInput.readInt();
                int len = dataInput.readInt();
                var bytes = new byte[len];
                dataInput.readFully(bytes);
                NBTTagCompound nbtTagCompound = NBTUtil.toNbt(bytes);
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
        NBTTagCompound nbtTagCompound = NBTUtil.toNbt(bytes);
        return CraftItemStack.asBukkitCopy(new ItemStack(nbtTagCompound));
    }

    /**
     * 1个int的长度,长度个byte的数据
     */
    public static byte[] itemStacks2Bytes(org.bukkit.inventory.ItemStack[] itemStacks) {
        var all = new ByteArrayOutputStream();
        var out = new DataOutputStream(all);
        Arrays.stream(itemStacks).filter(Objects::nonNull)
                // .peek(e->System.out.println(e.getType()))
                .map(CraftItemStack::asNMSCopy)
                // .peek(e->System.out.println(e.getItem().getName()))
                .forEach(e -> {
                    var nbt = new NBTTagCompound();
                    e.save(nbt);
                    var bytes = new ByteArrayOutputStream();
                    byte[] array = NBTUtil.toBytes(nbt);
                    if (array.length != 0) {
                        try {
                            out.writeInt(array.length);
                            out.write(array);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }

                });
        return all.toByteArray();
    }

    public static List<byte[]> itemStacks2BytesList(org.bukkit.inventory.ItemStack[] itemStacks) {
        var out = new ArrayList<byte[]>(itemStacks.length);
        Arrays.stream(itemStacks).filter(Objects::nonNull).map(CraftItemStack::asNMSCopy).forEach(e -> {
            var nbt = e.save(new NBTTagCompound());
            var bytes = NBTUtil.toBytes(nbt);
            if (bytes.length != 0) {
                out.add(bytes);
            }
        });
        return out;
    }

    /*public static org.bukkit.inventory.ItemStack[] bytes2ItemStacks(byte[] bytes) {
        var l = new ArrayList<org.bukkit.inventory.ItemStack>();
        var in = new DataInputStream(new ByteArrayInputStream(bytes));
        int len;
        try {
            while (in.available() > 0) {
                len = in.readInt();
                var b = new byte[len];
                //noinspection ResultOfMethodCallIgnored
                in.read(b);
                var nbt = new NBTTagCompound();
                load.invoke(nbt, new DataInputStream(new ByteArrayInputStream(b)), 4, new NBTReadLimiter(2097152));
                l.add(CraftItemStack.asBukkitCopy(new net.minecraft.server.v1_12_R1.ItemStack(nbt)));
            }
        } catch (Exception ignored) {
        }
        return l.toArray(new org.bukkit.inventory.ItemStack[]{});
    }*/
}
