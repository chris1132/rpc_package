package rpcPackage.rpc.netty_zookeeper_spring.registry;

/**
 * Created by wangchaohui on 2018/3/16.
 */
public class Constant {

    // 连接zookeeper的超时时间
    static int ZK_SESSION_TIMEOUT = 5000;

    //zookeeper中保存服务消息的父节点
    static final String PARENT_NODE = "/36_rpc";

    // zookeeper中服务提供者的序列化名称
    static final String SERVER_NAME= "/server";
}
