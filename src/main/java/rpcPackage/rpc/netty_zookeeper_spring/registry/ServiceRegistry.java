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


    //zookeeper�ĵ�ַ����spring����ServiceRegistry����ʱ����
    private String registryAddress;

    //����zookeeper�Ŀͻ���
    private ZooKeeper zkClient = null;

    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

    private CountDownLatch latch = new CountDownLatch(1);

    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }


    /**
     * ��zookeeperע�����
     */
    public void registerService(String serverAddress, String interfaceName) {

        if (this.zkClient == null) {
            logger.info("δ����zookeeper��׼����������...");
            connectServer();
        }
        logger.info("zookeeper���ӽ����ɹ���׼����zookeeper�ϴ�����ؽڵ�...");
        //���жϸ��ڵ��Ƿ���ڣ���������ڣ��򴴽����ڵ�
        if (!isExist(Constant.PARENT_NODE)) {
            logger.info("���ڴ����ڵ�[{}]", Constant.PARENT_NODE);
            AddRootNode(Constant.PARENT_NODE, "");
        }
        //���жϽӿڽڵ� �Ƿ���ڣ���/rpc/interfacename������������ڣ����ȴ����ӿڽڵ�
        if (!isExist(Constant.PARENT_NODE + "/" + interfaceName)) {
            logger.info("���ڴ����ڵ�[{}]", Constant.PARENT_NODE + "/" + interfaceName);
            AddRootNode(Constant.PARENT_NODE + "/" + interfaceName, "");
        }
        // �����ӿڽڵ��µķ����ṩ�߽ڵ㣨��/rpc/interfacename/provider00001��
        logger.info("���ڴ����ڵ�[{}]", Constant.PARENT_NODE + "/" + interfaceName + Constant.SERVER_NAME + "+���к�");
        createNode(Constant.PARENT_NODE + "/" + interfaceName + Constant.SERVER_NAME, serverAddress);
        logger.info("zookeeper����ؽڵ��Ѿ������ɹ�...");
    }

    /**
     * �жϽڵ��Ƿ����
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
     * �������ýڵ㣨���ڵ�/rpc�����ӽڵ㼴�ӿڽڵ���Ҫ����Ϊ�������ͣ�
     *
     * @param node �ڵ�����ƣ����ڵ�Ϊ/rpc���ӿڽӵ���Ϊ/rpc/{interfacename}
     * @param data �ڵ�����ݣ���Ϊ��
     */
    private void AddRootNode(String node, String data) {
        try {
            zkClient.create(node, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * �����������л��ڵ�
     *
     * @param node �ڵ�����ƣ���/rpc/{interfacename}/{serverName}
     * @param data �ڵ�����ݣ�Ϊ�����ṩ�ߵ�IP��ַ�Ͷ˿ںŵĸ�ʽ�����ݣ���192.168.100.101:21881
     */
    private void createNode(String node, String data) {
        try {
            zkClient.create(node, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}