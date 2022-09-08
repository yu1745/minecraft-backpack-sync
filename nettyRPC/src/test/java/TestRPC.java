import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import cf.wangyu1745.nettyRPC.Client;
import cf.wangyu1745.nettyRPC.Server;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestRPC {

    @SuppressWarnings("UnusedReturnValue")
    public interface ITest {
        String test(String s);

        String null_();

        int identify(int a);

        void A(A a);
    }


    public static class ITestImpl implements ITest {
//        private final AtomicInteger i = new AtomicInteger(0);

        @Override
        public String test(String s) {
            return "nmsl " + s;
        }

        @Override
        public String null_() {
            return null;
        }

        @Override
        public int identify(int a) {
            return a;
        }

        @Override
        public void A(A a) {
            System.out.println(a.toString());
        }

        @Override
        public String toString() {
            System.out.println("ITestImpl.toString");
            return "toString";
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class A {
        String a;
        int b;
        byte[] c;
    }

    @Test
    void normal() {
        System.out.println("TestRPC.normal");
        Client client = new Client();
        ITest service = client.getService(ITest.class);
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            int nextInt = random.nextInt();
            System.out.println("nextInt = " + nextInt);
            Assertions.assertEquals(nextInt, service.identify(nextInt));
        }
    }

    @Test
    public void qps() {
        System.out.println("TestRPC.qps");
        Client client = new Client();
        ITest ITest = client.getService(ITest.class);
        AtomicInteger integer = new AtomicInteger();
        int num = 2 << 20;
        int threads = 2;
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked") CompletableFuture<Void>[] futures = new CompletableFuture[threads];
        for (int i = 0; i < (2 << 10); i++) {
            sb.append('a');
        }
        String s = sb.toString();
        for (int i = 0; i < threads; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < num; j++) {
                    ITest.test(s);
                    integer.incrementAndGet();
                }
            });
        }
        new Thread(() -> {
            while (true) {
                try {
                    int i = integer.get();
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(integer.get() - i);
                    if (integer.get() - i == 0) {
                        return;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        CompletableFuture.allOf(futures).join();
    }


    @Test
    public void sync() {
        System.out.println("TestRPC.sync");
        Client client = new Client();
        ITest ITest = client.getService(ITest.class);
        int num = 2 << 14;
        ExecutorService service = Executors.newCachedThreadPool();
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            for (int i = 0; i < num; i++) {
                Assertions.assertEquals(i, ITest.identify(i));
            }
        }, service);
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            for (int i = num; i < num * 2; i++) {
                Assertions.assertEquals(i, ITest.identify(i));
            }
        }, service);
        CompletableFuture.allOf(future, future1).join();
    }

    @Test
    public void bootTime() {
        System.out.println("TestRPC.bootTime");
        Client client = new Client();
        ITest ITest = client.getService(ITest.class);
        int num = 1000;
        List<Double> list = new Vector<>(num);
        ExecutorService service = Executors.newCachedThreadPool();
        @SuppressWarnings("unchecked") CompletableFuture<Void>[] futures = new CompletableFuture[num];
        long l = System.nanoTime();
        for (int i = 0; i < num; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                ITest.test("");
                list.add((double) (System.nanoTime() - l) / 1000000);
            }, service);
        }
        CompletableFuture.allOf(futures).join();
        Collections.sort(list);
        System.out.println(list.get(list.size() - 1) - list.get(0) + "ms");
    }

    @Test
//    @Disabled
    public void hugeConnections() {
        System.out.println("TestRPC.hugeConnections");
        Client client = new Client();
        ITest ITest = client.getService(ITest.class);
        int num = 5000;
        ExecutorService service = Executors.newCachedThreadPool();
        @SuppressWarnings("unchecked") CompletableFuture<Void>[] futures = new CompletableFuture[num];
        for (int i = 0; i < num; i++) {
            int finalI = i;
            futures[i] = CompletableFuture.runAsync(() -> ITest.identify(finalI), service);
        }
        CompletableFuture.allOf(futures).join();
    }

    @Test
    void args() {
        ITest service = new Client().getService(ITest.class);
        service.A(new A("dass", 1, new byte[]{1, 2, 34, 5}));
    }

    @BeforeAll
    public static void server() {
        System.out.println("TestRPC.server");
        Server server = new Server();
        server.register(ITest.class, new ITestImpl());
        server.start();
    }

    @Test
    void testNull() {
        ITest service = new Client().getService(ITest.class);
        Assertions.assertNull(service.null_());
    }


    @Test
    void shutdown() {
        Client client = new Client();
        ITest ITest = client.getService(ITest.class);
        AtomicInteger integer = new AtomicInteger();
        int num = 2 << 20;
        int threads = 1;
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("unchecked") CompletableFuture<Void>[] futures = new CompletableFuture[threads];
        for (int i = 0; i < (2 << 1); i++) {
            sb.append('a');
        }
        String s = sb.toString();
        for (int i = 0; i < threads; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < num; j++) {
                    ITest.test(s);
                    integer.incrementAndGet();
                }
            });
        }
        new Thread(() -> {
            while (true) {
                try {
                    int i = integer.get();
                    TimeUnit.SECONDS.sleep(1);
                    System.out.println(integer.get() - i);
                    if (integer.get() - i == 0) {
                        return;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        CompletableFuture.runAsync(() -> {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            client.stop();
        });
        CompletableFuture.allOf(futures).join();
    }

    @SneakyThrows
    @Test
    void mapper(){
        ObjectMapper mapper = new ObjectMapper();
//        System.out.println(new String(mapper.writeValueAsBytes("sadads")));
        String s = mapper.readValue("\"wangyu\"", String.class);
        System.out.println(s);
    }

    /*@Test
    void toString_(){
        ITest service = new Client().getService(ITest.class);
        Assertions.assertEquals(service.toString(),"toString");
    }*/

    /*@Test
    void a() throws JsonProcessingException {
        @Data
        class A {
            String a = "nmsl";
            @JsonIgnore
            Condition condition;
        }
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(new A()));
    }

    @Test
    void argsName() throws NoSuchMethodException {
        class A {
            public void a(String s, int i, double[] doubles) {
            }
        }
        Method method = A.class.getMethod("a", String.class, int.class, double[].class);
        String[] argsName = Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).toArray(String[]::new);
        Arrays.stream(argsName).forEach(System.out::println);
    }

    @Test
    public void methodEqual() {
        class A implements nmsl {
            @Override
            public String nmsl_(String s) {
                return null;
            }
        }
        nmsl nmsl_ = (nmsl) Proxy.newProxyInstance(A.class.getClassLoader(), new Class[]{nmsl.class}, ((proxy, method, args) -> {
            System.out.println(method.equals(nmsl.class.getMethod("nmsl_", String.class)));
            return null;
        }));
        nmsl_.nmsl_("");
    }

    @Test
    public void port() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.close();
            }
        });

        // Start the server.
        ChannelFuture f = b.bind(0).sync();
        System.out.println(f.channel().localAddress().toString());
    }

    @Test
    public void tryFinally() {
        try {
            System.out.println("1");
            throw new RuntimeException();
        } finally {
            System.out.println("2");
        }
    }

    @Test
    public void toString_() {
        Arrays.stream(this.getClass().getMethods()).map(Method::getName).forEach(System.out::println);
    }*/


}
