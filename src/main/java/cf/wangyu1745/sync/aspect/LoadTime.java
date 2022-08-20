package cf.wangyu1745.sync.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.logging.Logger;
@EnableAspectJAutoProxy
@Aspect
@RequiredArgsConstructor
public class LoadTime {
    private final Logger logger;

    @Around("execution(* cf.wangyu1745.sync.config.*.*(..))")
    public Object time(ProceedingJoinPoint point) {
        long start = System.currentTimeMillis();
        Object rt = null;
        try {
            rt = point.proceed(point.getArgs());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        // 获取执行的方法名
        long end = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) point.getSignature();
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
        // 打印耗时的信息
        logger.info(methodName + " 耗时" + (end - start) + "ms");
        return rt;
    }

}
