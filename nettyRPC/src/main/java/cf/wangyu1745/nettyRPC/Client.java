package cf.wangyu1745.nettyRPC;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"SynchronizationOnLocalVariableOrMethodParameter", "NullableProblems"})
public class Client {
    private final String host;
    private final int port;
    private final boolean debug;
    private final NioEventLoopGroup group = new NioEventLoopGroup(1);
    //rpc调用返回为null时的标记对象
    private static final Object NULL = new Object();
    private final ExecutorService exe = Executors.newCachedThreadPool();
    private final Map<Channel, Request> requestMap = new ConcurrentHashMap<>();

    private final Deque<Channel> deque = new ConcurrentLinkedDeque<>();
    private final AtomicInteger i = new AtomicInteger();


    @Getter
    @NoArgsConstructor
    @ToString
    protected static class Request {
        public Request(int id, Object[] args, String clazz, int methodIndex, Method method) {
            this.id = id;
            this.args = args;
            this.clazz = clazz;
            this.methodIndex = methodIndex;
            this.method = method;
        }

        int id;
        @JsonIgnore
        Object[] args;
        String clazz;
        int methodIndex;
        @JsonIgnore
        Method method;

        @JsonIgnore
        Object rt;

        @JsonIgnore
        Channel channel;

        @JsonIgnore
        boolean errorOccur;
        @JsonIgnore
        boolean notified;
    }

    private class InvokeHandler implements InvocationHandler {
        /*@AllArgsConstructor
        private class Connection {
            final Channel channel;
            final Condition condition;
        }*/

        //连接池
        //method映射到int
        private final Map<Method, Integer> methodIntegerMap = new HashMap<>();

        public InvokeHandler(Class<?> c) {
            final int[] i = {0};
            Arrays.stream(c.getMethods()).sorted(Comparator.comparing(Method::toString)).forEachOrdered(e -> methodIntegerMap.put(e, i[0]++));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("hashCode")) {
                return 0;
            }
            if (method.getName().equals("equals")) {
                if (args[0] != null) {
                    return proxy == args[0];
                } else {
                    return false;
                }
            }
            if (group.isShutdown()) {
                return null;
            }
            Channel poll = deque.poll();
            if (poll == null) {
                //建立新连接
                if (debug) {
                    System.out.println("新连接");
                }
                Request request = new Request(i.getAndIncrement(), args, method.getDeclaringClass().getName(), methodIntegerMap.get(method), method);
                newConnection().addListener((ChannelFutureListener) future -> {
                    Channel channel = future.channel();
                        /*try {
                            lock.lock();
                            request.channel = channel;
                        } finally {
                            lock.unlock();
                        }*/
                    synchronized (request) {
                        request.channel = channel;
                    }
                    channel.writeAndFlush(request);
                });
                //防止永久阻塞
                    /*if (!condition.await(3, TimeUnit.SECONDS)) {
                        // 超时
                        System.out.println(request + " 超时");
                        if (request.channel != null) {
                            //已经建立了连接，需要销毁连接和request
                            requestMap.remove(request.channel);
                            request.channel.close();
                        }
                    } else {
                        // 不超时
                        if (request.channel != null) {
                            deque.offer(new Connection(request.channel, condition));
                        }
                    }*/
                synchronized (request) {
                    request.wait(3000);
                    if (request.errorOccur) {
                        System.out.println(request + "出错");
                    } else if (!request.notified) {
                        System.out.println(request + "超时");
                    } else {
                        deque.offer(request.channel);
                    }
                    return request.rt;
                }
            } else {
                //复用连接
                if (debug) {
                    System.out.println("复用连接");
                }
                if (!poll.isActive()) {
                    // 获取到了关闭的channel,于是递归获取能用的channel
                    if (debug) {
                        System.out.println("channel失效");
                    }
                    return invoke(proxy, method, args);
                }
                Request request = new Request(i.getAndIncrement(), args, method.getDeclaringClass().getName(), methodIntegerMap.get(method), method);
                poll.writeAndFlush(request);
                //防止永久阻塞
                /*if (!poll.condition.await(3, TimeUnit.SECONDS)) {
                    // 超时
                    System.out.println(request + " 超时");
                    if (request.channel != null) {
                        //已经建立了连接，需要销毁连接和request
                        requestMap.remove(request.channel);
                        request.channel.close();
                    }
                } else {
                    // 不超时
                    deque.offer(poll);
                }*/
                synchronized (request) {
                    request.wait(1000);
                    if (request.errorOccur) {
                        System.out.println(request + "出错");
                    } else if (!request.notified) {
                        System.out.println(request + "超时");
                    } else {
                        deque.offer(poll);
                    }
                    return request.rt;
                }
            }

        }
    }


    public Client(String host, int port, boolean debug) {
        this.host = host;
        this.port = port;
        this.debug = debug;
    }

    public Client() {
        this.host = "localhost";
        this.port = 8080;
        this.debug = false;
    }

    private ChannelFuture newConnection() {
        Bootstrap b = new Bootstrap();
        ObjectMapper mapper = new ObjectMapper();
        b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                if (debug) {
                    p.addLast(new LoggingHandler(LogLevel.INFO));
                }
                p.addLast(new MessageToByteEncoder<Request>() {
                    @Override
                    protected void encode(ChannelHandlerContext ctx, Request request, ByteBuf out) throws Exception {
                        requestMap.put(ctx.channel(), request);
                        if (debug) {
                            System.out.println(request);
                        }
                        byte[] reqBytes = mapper.writeValueAsBytes(request);
                        out.writeInt(reqBytes.length).writeBytes(reqBytes);
                        ArrayList<byte[]> args = new ArrayList<>();
                        if (request.args != null) {
                            for (Object arg : request.args) {
                                args.add(mapper.writeValueAsBytes(arg));
                            }
                            Integer len = args.stream().map(e -> e.length).reduce(0, Integer::sum);
                            out.writeInt(len + 4 * request.args.length);
                            args.forEach(e -> out.writeInt(e.length).writeBytes(e));
                        } else {
                            out.writeInt(0);
                        }
                    }
                });
                p.addLast(new ReplayingDecoder<Object>() {
                    @Override
                    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                        Request request = requestMap.get(ctx.channel());
                        int len = in.readInt();
                        if (len == 0) {
                            out.add(NULL);
                            return;
                        }
                        ByteBuf byteBuf = in.readBytes(len);
                        byte[] bytes = new byte[len];
                        byteBuf.readBytes(bytes);
                        Object object = mapper.readValue(bytes, request.method.getReturnType());
                        out.add(object);
                        byteBuf.release();
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                        cause.printStackTrace();
                        ctx.channel().close();
                        Request request = requestMap.remove(ctx.channel());
                        synchronized (request) {
                            request.errorOccur = true;
                            request.notify();
                        }
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) {
                        ctx.channel().close();
                        Request request = requestMap.remove(ctx.channel());
                        if (request != null) {
                            synchronized (request) {
                                request.errorOccur = true;
                                request.notify();
                            }
                        }
                    }
                });
                p.addLast(new SimpleChannelInboundHandler<Object>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
                        Request request = requestMap.remove(ctx.channel());
                        exe.submit(() -> {
                            synchronized (request) {
                                if (msg == NULL) {
                                    request.rt = null;
                                } else {
                                    request.rt = msg;
                                }
                                request.notified = true;
                                request.notify();
                            }
                        });
                    }
                });
            }
        });
        return b.connect(host, port);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T> T getService(Class<T> c) {
        return (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[]{c}, new InvokeHandler(c));
    }

    public void stop() {
        System.out.println("Client.stop");
        deque.clear();
        group.shutdownGracefully();
    }
}
