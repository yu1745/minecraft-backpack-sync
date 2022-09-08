package cf.wangyu1745.nettyRPC;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Promise;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("NullableProblems")
public class Server {


    private static final Object NULL = new Object();
    //    private static final int PORT = 8080;
    private final int port;
    private final boolean debug;
    private final Map<String, ServiceWrapper> serviceWrapperMap = new ConcurrentHashMap<>();
    private final ExecutorService exe = Executors.newCachedThreadPool();

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
//    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private static class ServiceWrapper {
        final Method[] methods;
        final Object service;

        public ServiceWrapper(Class<?> c, Object service) {
            methods = c.getMethods();
            Arrays.sort(methods, Comparator.comparing(Method::toString));
            System.out.println("接口方法:" + Arrays.toString(methods));
            this.service = service;
        }
    }

    public Server(int port, boolean debug) {
        this.port = port;
        this.debug = debug;
    }

    public Server() {
        this.port = 8080;
        this.debug = false;
    }

    @SneakyThrows
    public ChannelFuture start() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.TCP_NODELAY, true)
//             .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel( SocketChannel ch) {
                        ObjectMapper mapper = new ObjectMapper();
                        ChannelPipeline p = ch.pipeline();
                        if (debug) {
                            p.addLast(new LoggingHandler(LogLevel.INFO));
                        }
                        p.addLast(new ReplayingDecoder<Client.Request>() {
                            @Override
                            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                                int reqLen = in.readInt();
                                ByteBuf reqBuf = in.readBytes(reqLen);
                                byte[] reqBytes = new byte[reqLen];
                                reqBuf.readBytes(reqBytes);
                                reqBuf.release();
                                int argsLen = in.readInt();
                                if (argsLen == 0) {
                                    Client.Request request = mapper.readValue(reqBytes, Client.Request.class);
                                    out.add(request);
                                    return;
                                }
                                ByteBuf argsBuf = in.readBytes(argsLen);
                                byte[] argsBytes = new byte[argsLen];
                                argsBuf.readBytes(argsBytes);
                                argsBuf.release();
                                Client.Request request = mapper.readValue(reqBytes, Client.Request.class);
                                ServiceWrapper serviceWrapper = serviceWrapperMap.get(request.clazz);
                                Method method = serviceWrapper.methods[request.methodIndex];
                                Class<?>[] parameterTypes = method.getParameterTypes();
//                                ByteArrayInputStream inputStream = new ByteArrayInputStream(argsBytes);
                                List<Object> args = new ArrayList<>();
                                int offset = 4;
                                DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(argsBytes));
                                for (Class<?> type : parameterTypes) {
                                    int len = dataInputStream.readInt();
                                    dataInputStream.skipBytes(len);
                                    args.add(mapper.readValue(argsBytes, offset, len, type));
                                    offset += 4 + len;
                                }
                                request.args = args.toArray();
                                out.add(request);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                System.out.println("Server.exceptionCaught");
                                cause.printStackTrace();
                                ctx.channel().close();
                            }
                        });
                        p.addLast(new SimpleChannelInboundHandler<Client.Request>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, Client.Request request) {
                                Promise<Object> promise = ctx.executor().newPromise();
                                ServiceWrapper serviceWrapper = serviceWrapperMap.get(request.clazz);
                                exe.submit(() -> {
                                    try {
                                        if (debug) {
                                            //noinspection RedundantStringFormatCall
                                            System.out.print(String.format("[%d]%s.%s%s", request.id, serviceWrapper.methods[request.getMethodIndex()].getDeclaringClass().getSimpleName(), serviceWrapper.methods[request.methodIndex].getName(), Arrays.toString(request.args)));
                                        }
                                        Object rt = serviceWrapper.methods[request.methodIndex].invoke(serviceWrapper.service, request.args);
                                        if (debug) {
                                            //noinspection RedundantStringFormatCall
                                            System.out.print(String.format("[%d]返回值:%s", request.id, rt));
                                        }
                                        promise.setSuccess(rt);
                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                        promise.setFailure(e);
                                    }
                                });
                                promise.addListener(future -> {
                                    if (future.isSuccess()) {
                                        Object rt = Optional.ofNullable(future.get()).orElse(NULL);
                                        ctx.channel().writeAndFlush(rt);
                                    } else {
                                        future.cause().printStackTrace();
                                        ctx.channel().writeAndFlush(NULL);
                                    }
                                });
                            }
                        });
                        p.addLast(new MessageToByteEncoder<Object>() {
                            @Override
                            protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
                                if (msg == NULL) {
                                    ctx.writeAndFlush(ctx.alloc().buffer().writeInt(0));
                                } else {
                                    byte[] bytes = mapper.writeValueAsBytes(msg);
                                    ctx.writeAndFlush(ctx.alloc().buffer().writeInt(bytes.length).writeBytes(bytes));
                                }
                            }
                        });
                    }
                });

        // Start the server.
        return serverBootstrap.bind(port);
    }

    public void register(Class<?> c, Object o) {
        serviceWrapperMap.put(c.getName(), new ServiceWrapper(c, o));
    }
}
