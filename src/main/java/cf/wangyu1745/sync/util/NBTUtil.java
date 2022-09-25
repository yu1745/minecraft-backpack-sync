package cf.wangyu1745.sync.util;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.var;
import net.minecraft.server.v1_12_R1.NBTReadLimiter;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NBTTagList;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Objects;

@SuppressWarnings("unused")
@Component
@RequiredArgsConstructor
public class NBTUtil {
    private static Method listWrite;
    private static Method listLoad;
    private static Method write;
    private static Method load;

    static {
        try {
            // 反射nbt方法们
            write = NBTTagCompound.class.getDeclaredMethod("write", DataOutput.class);
            write.setAccessible(true);
            load = NBTTagCompound.class.getDeclaredMethod("load", DataInput.class, int.class, NBTReadLimiter.class);
            load.setAccessible(true);
            listWrite = NBTTagList.class.getDeclaredMethod("write", DataOutput.class);
            listWrite.setAccessible(true);
            listLoad = NBTTagList.class.getDeclaredMethod("load", DataInput.class, int.class, NBTReadLimiter.class);
            listLoad.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
            Runtime.getRuntime().exit(0);
        }
    }

    @SneakyThrows
    public static byte[] toBytes(NBTTagCompound nbt) {
        var bytes = new ByteArrayOutputStream();
        write.invoke(nbt, new DataOutputStream(bytes));
        return bytes.toByteArray();
    }

    @SneakyThrows
    public static byte[] toBytes(NBTTagList nbt) {
        var bytes = new ByteArrayOutputStream();
        listWrite.invoke(nbt, new DataOutputStream(bytes));
        return bytes.toByteArray();
    }

    @SneakyThrows
    public static NBTTagList toList(byte[] bytes) {
        var nbt = new NBTTagList();
        listLoad.invoke(nbt, new DataInputStream(new ByteArrayInputStream(Objects.requireNonNull(bytes))), 4, new NBTReadLimiter(2097152));
        return nbt;
    }

    @SneakyThrows
    public static NBTTagCompound toNbt(byte[] bytes) {
        var nbt = new NBTTagCompound();
        load.invoke(nbt, new DataInputStream(new ByteArrayInputStream(Objects.requireNonNull(bytes))), 4, new NBTReadLimiter(2097152));
        return nbt;
    }
}
