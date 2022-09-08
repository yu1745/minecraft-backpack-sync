package cf.wangyu1745.sync.aspect;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * 不知道为啥不生效fffffffffff
 */
@Aspect
@RequiredArgsConstructor
public class MainThreadExecutor {
    private final BukkitScheduler scheduler;
    private final JavaPlugin plugin;

    @NoArgsConstructor
    private static class RT {
        Object rt;
    }

    @Around("@annotation(MainThread)")
    public Object exe(ProceedingJoinPoint point) {
        System.out.println("MainThreadExecutor.exe");
        RT rt = new RT();
        synchronized (rt) {
            scheduler.runTask(plugin, () -> {
                synchronized (rt) {
                    try {
                        rt.rt = point.proceed(point.getArgs());
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            try {
                rt.wait(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return rt.rt;
    }
}
