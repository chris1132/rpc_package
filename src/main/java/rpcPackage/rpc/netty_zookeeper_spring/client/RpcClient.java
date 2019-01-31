package rpcPackage.rpc.netty_zookeeper_spring.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpcPackage.rpc.netty_zookeeper_spring.util.RpcDecoder;
import rpcPackage.rpc.netty_zookeeper_spring.util.RpcEncoder;
import rpcPackage.rpc.netty_zookeeper_spring.util.RpcRequest;
import rpcPackage.rpc.netty_zookeeper_spring.util.RpcResponse;

import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangchaohui on 2019/1/30.
 */
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {
    // RPC����˵ĵ�ַ
    private String host;
    // RPC����˵Ķ˿ں�
    private int port;
    // RPCResponse��Ӧ����
    private RpcResponse response;
    // log4j��־��¼
    private Logger logger = LoggerFactory.getLogger(RpcClient.class);

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * ��RPC����˷������󷽷�
     *
     * @param request RPC�ͻ�����RPC����˷��͵�request����
     * @return
     */
    public RpcResponse sendRequest(RpcRequest request) throws Exception {

        // ���ÿͻ���NIO�߳���
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024)
                    // ����TCP���ӳ�ʱʱ��
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel sc) throws Exception {
                            //��ӱ�������Rpc�������Ҫ�������RpcRequest������Ϊ��Ҫ���տͻ��˷��͹���������
                            sc.pipeline().addLast(new RpcDecoder(RpcResponse.class));
                            //��ӽ�����
                            sc.pipeline().addLast(new RpcEncoder(RpcRequest.class));
                            //���ҵ����handler
                            sc.pipeline().addLast(RpcClient.this);
                        }
                    });
            // �����첽���Ӳ�����ע��������bind���ͻ�������Ҫconnect��
            logger.info("׼�������첽���Ӳ���[{}:{}]", host, port);
            ChannelFuture f = b.connect(host, port).sync();
            // ��RPC����˷�������
            logger.info("׼����RPC����˷�������...");
            f.channel().writeAndFlush(request);


            // ��Ҫע����ǣ����û�н��յ�����˷������ݣ���ô��һֱͣ������ȴ�
            // �ȴ��ͻ�����·�ر�
            logger.info("׼���ȴ��ͻ�����·�ر�...");
            f.channel().closeFuture().sync();

        } finally {
            // �����˳����ͷ�NIO�߳���
            logger.info("�����˳����ͷ�NIO�߳���...");
            group.shutdownGracefully();
        }
        return response;
    }

    /**
     * ��ȡrpc����˵���Ӧ���������ֵ��response����
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse rpcResponse) throws Exception {
        logger.info("��RPC����˽��յ���Ӧ...");
        this.response = rpcResponse;
        // �ر������˵����ӣ������Ϳ���ִ��f.channel().closeFuture().sync();֮��Ĵ��룬�������˳�
        // �൱���������ر�����
        ctx.close();
    }

}

