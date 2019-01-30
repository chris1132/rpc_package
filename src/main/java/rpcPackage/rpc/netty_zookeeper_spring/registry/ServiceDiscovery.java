package rpcPackage.rpc.netty_zookeeper_spring.registry;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpcPackage.rpc.netty_zookeeper_spring.client.ZookeeperConnectManage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by wangchaohui on 2018/3/16.
 */
public class ServiceDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<>();

    private String registryAddress;
    private ZooKeeper zkClient = null;

    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
//        zookeeper = connectServer();
//        if (zookeeper != null) {
//            //查询服务地址，更新
//            watchNode(zookeeper);
//        }
    }

    /**
     * 发现服务方法 根据接口名称向zookeeper查询服务提供者的地址
     */
    public String discoverService(String interfaceName) {
        if (this.zkClient == null) {
            logger.info("未连接zookeeper，准备建立连接...");
            connectServer();
        }
        //构建需要查询的节点的完整名称
        String node = Constant.PARENT_NODE + "/" + interfaceName;
        //获取该节点所对应的服务提供者地址
        logger.info("zookeeper连接建立完毕，准备获取服务提供者地址[{}]...", node);
        String serverAddress = watchNode(node);
        logger.info("服务提供者地址获取完毕[{}]...", serverAddress);
        // 返回结果
        return serverAddress;

    }

    private void connectServer() {
        try {
            zkClient = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        } catch (IOException | InterruptedException e) {
            logger.error("", e);
        }
    }

    private String watchNode(String node) {
        String serverAddress = null;
        try {
            List<String> nodeList = zkClient.getChildren(node, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        watchNode(node);
                    }
                }
            });
            // 负载均衡：一致性hash算法
            ConsistentHash.initServers(nodeList);
            String firstChildren = ConsistentHash.getServer("children");
            // 构建该服务提供者的完整节点名称
            String firstChildrenNode = node + "/" + firstChildren;
            // 获取服务提供者节点的数据，得到serverAddress的byte数组
            byte[] serverAddressByte = zkClient.getData(firstChildrenNode, false, null);
            // 将byte数组转换为字符串，同时赋值给serverAddress
            serverAddress = new String(serverAddressByte);

            logger.debug("更新服务节点.");
            UpdateConnectedServer();
        } catch (KeeperException | InterruptedException e) {
            logger.error("", e);
        }
        return serverAddress;
    }

    private void UpdateConnectedServer() {
        ZookeeperConnectManage.getInstance().updateConnectedServer(this.dataList);
    }

    public void stop() {
        if (zkClient != null) {
            try {
                zkClient.close();
            } catch (InterruptedException e) {
                logger.error("", e);
            }
        }
    }
}
