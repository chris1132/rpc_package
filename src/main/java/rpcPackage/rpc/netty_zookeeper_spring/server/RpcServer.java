package rpcPackage.rpc.netty_zookeeper_spring.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import rpcPackage.rpc.netty_zookeeper_spring.util.RpcDecoder;
import rpcPackage.rpc.netty_zookeeper_spring.util.RpcEncoder;
import rpcPackage.rpc.netty_zookeeper_spring.util.RpcRequest;
import rpcPackage.rpc.netty_zookeeper_spring.util.RpcResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangchaohui on 2019/1/30.
 *
 */

/**
 * RPCServer主要完成下面的几个功能
 * 1、将需要发布的服务保存到一个map中
 * 2、启动netty服务端程序
 * 3、向zookeeper注册需要发布的服务
 */
@Component
public class RpcServer implements InitializingBean {
    //用来保存用户服务实现类对象，key为实现类的接口名称，value为实现类对象
    public Map<String, Object> handlerMap = new HashMap<>();

    private String serverAddress;

    private ServiceRegistry serviceRegistry;

    private Logger logger = LoggerFactory.getLogger(RpcServer.class);

    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }


    /**
     * 由于本类实现了InitializingBean接口，spring在构造完所有对象之后会调用afterPropertiesSet方法
     * 在该方法中，将服务注册到zookeeper，同时启动netty服务端程序，该方法中主要是netty框架的代码
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        logger.info("准备构建RPC服务端，监听来自RPC客户端的请求...");
        // 配置服务端NIO线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel sc) throws Exception {
                            //添加编码器，Rpc服务端需要解码的是RpcRequest对象，因为需要接收客户端发送过来的请求
                            sc.pipeline().addLast(new RpcDecoder(RpcRequest.class));
                            //添加解码器
                            sc.pipeline().addLast(new RpcEncoder(RpcResponse.class));
                            //添加业务处理handler
                            sc.pipeline().addLast(new RpcHandler(handlerMap));
                        }
                    });

            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.valueOf(array[1]);

            // 绑定端口，同步等待成功，该方法是同步阻塞的，绑定成功后返回一个ChannelFuture
            logger.info("准备绑定服务提供者地址和端口[{}:{}]", host, port);
            ChannelFuture f = b.bind(host, port).sync();

            // 向zookeeper注册
            logger.info("绑定服务提供者地址和端口成功，准备向zookeeper注册服务...");
            for (String interfaceName : handlerMap.keySet()) {
                serviceRegistry.registerService(serverAddress, interfaceName);
            }
            // 等待服务端监听端口关闭，阻塞，等待服务端链路关闭之后main函数才退出
            logger.info("向zookeeper注册服务成功，正在监听来自RPC客户端的请求连接...");
            f.channel().closeFuture().sync();
        } finally {
            //优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
