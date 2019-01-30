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
//            //��ѯ�����ַ������
//            watchNode(zookeeper);
//        }
    }

    /**
     * ���ַ��񷽷� ���ݽӿ�������zookeeper��ѯ�����ṩ�ߵĵ�ַ
     */
    public String discoverService(String interfaceName) {
        if (this.zkClient == null) {
            logger.info("δ����zookeeper��׼����������...");
            connectServer();
        }
        //������Ҫ��ѯ�Ľڵ����������
        String node = Constant.PARENT_NODE + "/" + interfaceName;
        //��ȡ�ýڵ�����Ӧ�ķ����ṩ�ߵ�ַ
        logger.info("zookeeper���ӽ�����ϣ�׼����ȡ�����ṩ�ߵ�ַ[{}]...", node);
        String serverAddress = watchNode(node);
        logger.info("�����ṩ�ߵ�ַ��ȡ���[{}]...", serverAddress);
        // ���ؽ��
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
            // ���ؾ��⣺һ����hash�㷨
            ConsistentHash.initServers(nodeList);
            String firstChildren = ConsistentHash.getServer("children");
            // �����÷����ṩ�ߵ������ڵ�����
            String firstChildrenNode = node + "/" + firstChildren;
            // ��ȡ�����ṩ�߽ڵ�����ݣ��õ�serverAddress��byte����
            byte[] serverAddressByte = zkClient.getData(firstChildrenNode, false, null);
            // ��byte����ת��Ϊ�ַ�����ͬʱ��ֵ��serverAddress
            serverAddress = new String(serverAddressByte);

            logger.debug("���·���ڵ�.");
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
