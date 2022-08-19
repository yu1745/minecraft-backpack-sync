package cf.wangyu1745.sync.util;

import lombok.var;
import org.apache.commons.lang3.tuple.Pair;

public class ArrayUtil {
    /**
     * @return [:i) [i:]
     */
    public static Pair<byte[], byte[]> split(byte[] bytes, int i) {
        var a = new byte[i];
        var b = new byte[bytes.length - i];
        System.arraycopy(bytes, 0, a, 0, i);
        System.arraycopy(bytes, i, b, 0, bytes.length - i);
        return Pair.of(a, b);
    }
}
