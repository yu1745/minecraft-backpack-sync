package cf.wangyu1745.sync.util;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class Reflect {
    public static List<String> getAllFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields()).map(f -> Pair.of(f.getType(), f.getName())).map(Objects::toString).collect(Collectors.toList());
    }

    public static List<Class<?>> recursiveInterfaces(Class<?> c) {
        List<Class<?>> l = new ArrayList<>();
        for (Class<?> anInterface : c.getInterfaces()) {
            l.add(anInterface);
            l.addAll(recursiveInterfaces(anInterface));
        }
        return l;
    }
}
