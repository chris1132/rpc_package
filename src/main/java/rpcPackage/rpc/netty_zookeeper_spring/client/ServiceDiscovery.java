package rpcPackage.rpc.netty_zookeeper_spring.client;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by wangchaohui on 2019/1/30
 */
public class ServiceDiscovery {

    // zookeeper�б��������Ϣ�ĸ��ڵ�
    private final String parentNode = "/rpc";
    // zookeeper�ĵ�ַ����spring����ServiceDiscovery����ʱ����
    private String registryAddress;

    private int sessionTimeout = 2000;

    private ZooKeeper zkClient = null;
    // ����ȷ��zookeeper���ӳɹ���Ž��к����Ĳ���
    private CountDownLatch latch = new CountDownLatch(1);
    // log4j��־��¼
    private Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);

    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
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
        String node = parentNode + "/" + interfaceName;
        //��ȡ�ýڵ�����Ӧ�ķ����ṩ�ߵ�ַ
        logger.info("zookeeper���ӽ�����ϣ�׼����ȡ�����ṩ�ߵ�ַ[{}]...", node);
        String serverAddress = getServerAddress(node);
        logger.info("�����ṩ�ߵ�ַ��ȡ���[{}]...", serverAddress);
        // ���ؽ��
        return serverAddress;

    }

    /**
     * ��������
     */
    private void connectServer() {
        try {
            zkClient = new ZooKeeper(registryAddress, sessionTimeout, new Watcher() {

                // ע������¼������ӳɹ�������process����
                // ��ʱ�ٵ���latch��countDown����ʹCountDownLatch��������1
                // ��Ϊ����CountDownLatch����ʱ���õ�ֵΪ1����1���Ϊ0������ִ�и÷�����latch.await()�����ж�
                // �Ӷ�ȷ�����ӳɹ���Ż�ִ�к���zookeeper����ز���
                @Override
                public void process(WatchedEvent event) {
                    // ���״̬Ϊ�����ӣ���ʹ��CountDownLatch��������1
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ��ȡ��Ӧ�ӿ����ķ����ַ
     */
    private String getServerAddress(String node) {
        String serverAddress = null;
        try {
            // �Ȼ�ȡ�ӿ����ڵ���ӽڵ㣬�ӽڵ����Ƿ��������б�
            // ��Ҫע����ǣ���������ڸýڵ㣬�����쳣����ʱ����Ĵ���Ͳ���ִ��
            List<String> children = zkClient.getChildren(node, false);

            // ���ؾ��⣺һ����hash�㷨
            ConsistentHash.initServers(children);
            String firstChildren = ConsistentHash.getServer("children");
            // �����÷����ṩ�ߵ������ڵ�����
            String firstChildrenNode = node + "/" + firstChildren;
            // ��ȡ�����ṩ�߽ڵ�����ݣ��õ�serverAddress��byte����
            byte[] serverAddressByte = zkClient.getData(firstChildrenNode, false, null);
            // ��byte����ת��Ϊ�ַ�����ͬʱ��ֵ��serverAddress
            serverAddress = new String(serverAddressByte);
        } catch (Exception e) {
            logger.error("�ڵ�[{}]�����ڣ��޷���ȡ�����ṩ�ߵ�ַ...", node);
            logger.error(e.getMessage());
        }
        return serverAddress;
    }



    public static void main(String[] args) throws Exception {
//        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("localhost:2181");
//        serviceDiscovery.connectServer();
//        String serverAddress = serviceDiscovery.getServerAddress("/rpc/com.jyxmust.UserService");
//        System.out.println("serverAddress: " + serverAddress);


        List<String> servers = new ArrayList<>();
        servers.add("192.168.0.0:111");
        servers.add("192.168.0.1:111");
        servers.add("192.168.0.3:111");
        ConsistentHash.initServers(servers);
        String firstChildren = ConsistentHash.getServer("166");
        System.out.println(firstChildren);
    }
}
