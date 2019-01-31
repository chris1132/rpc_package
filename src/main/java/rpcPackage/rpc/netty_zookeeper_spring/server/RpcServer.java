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
 * RPCServer��Ҫ�������ļ�������
 * 1������Ҫ�����ķ��񱣴浽һ��map��
 * 2������netty����˳���
 * 3����zookeeperע����Ҫ�����ķ���
 */
@Component
public class RpcServer implements InitializingBean {
    //���������û�����ʵ�������keyΪʵ����Ľӿ����ƣ�valueΪʵ�������
    public Map<String, Object> handlerMap = new HashMap<>();

    private String serverAddress;

    private ServiceRegistry serviceRegistry;

    private Logger logger = LoggerFactory.getLogger(RpcServer.class);

    public RpcServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }


    /**
     * ���ڱ���ʵ����InitializingBean�ӿڣ�spring�ڹ��������ж���֮������afterPropertiesSet����
     * �ڸ÷����У�������ע�ᵽzookeeper��ͬʱ����netty����˳��򣬸÷�������Ҫ��netty��ܵĴ���
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        logger.info("׼������RPC����ˣ���������RPC�ͻ��˵�����...");
        // ���÷����NIO�߳���
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel sc) throws Exception {
                            //��ӱ�������Rpc�������Ҫ�������RpcRequest������Ϊ��Ҫ���տͻ��˷��͹���������
                            sc.pipeline().addLast(new RpcDecoder(RpcRequest.class));
                            //��ӽ�����
                            sc.pipeline().addLast(new RpcEncoder(RpcResponse.class));
                            //���ҵ����handler
                            sc.pipeline().addLast(new RpcHandler(handlerMap));
                        }
                    });

            String[] array = serverAddress.split(":");
            String host = array[0];
            int port = Integer.valueOf(array[1]);

            // �󶨶˿ڣ�ͬ���ȴ��ɹ����÷�����ͬ�������ģ��󶨳ɹ��󷵻�һ��ChannelFuture
            logger.info("׼���󶨷����ṩ�ߵ�ַ�Ͷ˿�[{}:{}]", host, port);
            ChannelFuture f = b.bind(host, port).sync();

            // ��zookeeperע��
            logger.info("�󶨷����ṩ�ߵ�ַ�Ͷ˿ڳɹ���׼����zookeeperע�����...");
            for (String interfaceName : handlerMap.keySet()) {
                serviceRegistry.registerService(serverAddress, interfaceName);
            }
            // �ȴ�����˼����˿ڹرգ��������ȴ��������·�ر�֮��main�������˳�
            logger.info("��zookeeperע�����ɹ������ڼ�������RPC�ͻ��˵���������...");
            f.channel().closeFuture().sync();
        } finally {
            //�����˳����ͷ��̳߳���Դ
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
