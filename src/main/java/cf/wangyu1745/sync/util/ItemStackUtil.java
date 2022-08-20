package cf.wangyu1745.sync.util;

import cf.wangyu1745.sync.Sync;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.v1_12_R1.ItemStack;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.util.ArrayList;

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
            if ((a.getTag() != null) || (b.getTag() == null)) {
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
            }
        }
        return false;
    }

    public static void save(ItemStack itemStack, DataOutput dataOutput) {
        try {
            NBTTagCompound nbtTagCompound = itemStack.save(new NBTTagCompound());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
            DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            Sync.write.invoke(nbtTagCompound, dataOutputStream);
            dataOutput.writeInt(dataOutputStream.size());
//            dataOutput.writeByte(itemStack.getCount());
            dataOutput.write(byteArrayOutputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static ItemStack[] load(DataInput dataInput) {
        ArrayList<ItemStack> itemStacks = new ArrayList<>(60);
        try {
//            int len = dataInput.readInt();
//            byte count = dataInput.readByte();
            //noinspection InfiniteLoopStatement
            while (true) {
                NBTTagCompound nbtTagCompound = new NBTTagCompound();
                Sync.load.invoke(nbtTagCompound, dataInput, 4, new NBTReadLimiter(2097152));
                itemStacks.add(new ItemStack(nbtTagCompound));
            }
        } catch (Exception e) {
            return itemStacks.toArray(new ItemStack[0]);
        }
    }
}
