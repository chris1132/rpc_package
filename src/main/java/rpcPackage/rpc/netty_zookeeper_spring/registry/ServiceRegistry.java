package rpcPackage.rpc.netty_zookeeper_spring.registry;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by wangchaohui on 2018/3/16.
 */
public class ServiceRegistry {


    //zookeeper的地址，由spring构造ServiceRegistry对象时传入
    private String registryAddress;

    //连接zookeeper的客户端
    private ZooKeeper zkClient = null;

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    private CountDownLatch latch = new CountDownLatch(1);

    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }


    /**
     * 向zookeeper注册服务
     */
    public void registerService(String serverAddress, String interfaceName) {

        if (this.zkClient == null) {
            logger.info("未连接zookeeper，准备建立连接...");
            connectServer();
        }
        logger.info("zookeeper连接建立成功，准备在zookeeper上创建相关节点...");
        //先判断父节点是否存在，如果不存在，则创建父节点
        if (!isExist(Constant.PARENT_NODE)) {
            logger.info("正在创建节点[{}]", Constant.PARENT_NODE);
            AddRootNode(Constant.PARENT_NODE, "");
        }
        //先判断接口节点 是否存在（即/rpc/interfacename），如果不存在，则先创建接口节点
        if (!isExist(Constant.PARENT_NODE + "/" + interfaceName)) {
            logger.info("正在创建节点[{}]", Constant.PARENT_NODE + "/" + interfaceName);
            AddRootNode(Constant.PARENT_NODE + "/" + interfaceName, "");
        }
        // 创建接口节点下的服务提供者节点（即/rpc/interfacename/provider00001）
        logger.info("正在创建节点[{}]", Constant.PARENT_NODE + "/" + interfaceName + Constant.SERVER_NAME + "+序列号");
        createNode(Constant.PARENT_NODE + "/" + interfaceName + Constant.SERVER_NAME, serverAddress);
        logger.info("zookeeper上相关节点已经创建成功...");
    }

    /**
     * 判断节点是否存在
     * */
    private boolean isExist(String node) {
        Stat stat = null;
        try {
            stat = zkClient.exists(node, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stat == null ? false : true;

    }

    private ZooKeeper connectServer() {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        } catch (IOException e) {
            logger.error("", e);
        } catch (InterruptedException ex) {
            logger.error("", ex);
        }
        return zk;
    }

    /**
     * 创建永久节点（父节点/rpc和其子节点即接口节点需要创建为此种类型）
     *
     * @param node 节点的名称，父节点为/rpc，接口接点则为/rpc/{interfacename}
     * @param data 节点的数据，可为空
     */
    private void AddRootNode(String node, String data) {
        try {
            zkClient.create(node, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建短暂序列化节点
     *
     * @param node 节点的名称，如/rpc/{interfacename}/{serverName}
     * @param data 节点的数据，为服务提供者的IP地址和端口号的格式化数据，如192.168.100.101:21881
     */
    private void createNode(String node, String data) {
        try {
            zkClient.create(node, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}