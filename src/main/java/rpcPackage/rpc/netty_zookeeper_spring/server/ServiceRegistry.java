package rpcPackage.rpc.netty_zookeeper_spring.server;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Created by wangchaohui on 2019/1/30
 */
public class ServiceRegistry {

    //zookeeper�б��������Ϣ�ĸ��ڵ�
    private final String parentNode = "/rpc";
    // zookeeper�з����ṩ�ߵ����л�����
    private final String serverName = "server";
    //zookeeper�ĵ�ַ����spring����ServiceRegistry����ʱ����
    private String registryAddress;
    // ����zookeeper�ĳ�ʱʱ��
    private int sessionTimeout = 2000;
    //����zookeeper�Ŀͻ���
    private ZooKeeper zkClient = null;
    // ����ȷ��zookeeper���ӳɹ���Ž��к����Ĳ���
    private CountDownLatch latch = new CountDownLatch(1);
    // log4j��־��¼
    private Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

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
        if (!isExist(parentNode)) {
            logger.info("���ڴ����ڵ�[{}]", parentNode);
            createPNode(parentNode, "");
        }
        //���жϽӿڽڵ� �Ƿ���ڣ���/rpc/interfacename������������ڣ����ȴ����ӿڽڵ�
        if (!isExist(parentNode + "/" + interfaceName)) {
            logger.info("���ڴ����ڵ�[{}]", parentNode + "/" + interfaceName);
            createPNode(parentNode + "/" + interfaceName, "");
        }
        // �����ӿڽڵ��µķ����ṩ�߽ڵ㣨��/rpc/interfacename/provider00001��
        logger.info("���ڴ����ڵ�[{}]", parentNode + "/" + interfaceName + "/" + serverName + "+���к�");
        createESNode(parentNode + "/" + interfaceName + "/" + serverName, serverAddress);
        logger.info("zookeeper����ؽڵ��Ѿ������ɹ�...");
    }

    /**
     * �������ýڵ㣨���ڵ�/rpc�����ӽڵ㼴�ӿڽڵ���Ҫ����Ϊ�������ͣ�
     *
     * @param node �ڵ�����ƣ����ڵ�Ϊ/rpc���ӿڽӵ���Ϊ/rpc/interfacename
     * @param data �ڵ�����ݣ���Ϊ��
     */
    private void createPNode(String node, String data) {
        try {
            zkClient.create(node, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * �����������л��ڵ�
     *
     * @param node �ڵ�����ƣ���/rpc/interfacename/server00001
     * @param data �ڵ�����ݣ�Ϊ�����ṩ�ߵ�IP��ַ�Ͷ˿ںŵĸ�ʽ�����ݣ���192.168.100.101:21881
     */
    private void createESNode(String node, String data) {
        try {
            zkClient.create(node, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ��������
     */
    private boolean isExist(String node) {
        Stat stat = null;
        try {
            stat = zkClient.exists(node, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stat == null ? false : true;

    }

    private void connectServer() {
        try {
            zkClient = new ZooKeeper(registryAddress, sessionTimeout, new Watcher() {
                // ע������¼������ӳɹ�������process����
                @Override
                public void process(WatchedEvent watchedEvent) {
                    // ���״̬Ϊ�����ӣ���ʹ��CountDownLatch��������1
                    if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                        logger.info("���ӳɹ��ˡ�����");
                    }
                }
            });
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
