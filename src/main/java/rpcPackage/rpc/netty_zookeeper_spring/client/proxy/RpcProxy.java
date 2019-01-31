package rpcPackage.rpc.netty_zookeeper_spring.client.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rpcPackage.rpc.netty_zookeeper_spring.client.RpcClient;
import rpcPackage.rpc.netty_zookeeper_spring.client.ServiceDiscovery;
import rpcPackage.rpc.netty_zookeeper_spring.util.RpcRequest;
import rpcPackage.rpc.netty_zookeeper_spring.util.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;

/**
 * Created by wangchaohui on 2019/1/30
 */
public class RpcProxy {


    //���ڷ��ַ���Ķ���
    private ServiceDiscovery serviceDiscovery;

    private Logger logger = LoggerFactory.getLogger(RpcProxy.class);

    public RpcProxy(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    /**
     *
     * @SuppressWarnings("unchecked")
     * ����һ���ӿ�, ��java.lang����.
    ���ξ�����Ϣ(һ���Ǻ������õ��˹��ڵķ������������Ĳ������Ͳ���).
    ����ı�����ܴ��ھ���ʱ,���簲ȫ����,�����¾ͻ����һ����ɫ�Ĳ�����,��������������.����ע�������Ǹ�������һ��ָ��,�������Ա���ע�Ĵ���Ԫ���ڲ���ĳЩ���汣�־�Ĭ.
     */

    /**
     * ��ö�̬��������ͨ�÷�����ʵ��˼·���÷����У�������Ҫ�����ʵ���������Ϊ��invoke�����У����������Method�������
     * ֻ�ǻ�ȡ�䷽�������֣�Ȼ�����װ��netty�����У����͵�metty�����������Զ�̵��õĽ��
     *
     * @param interfaceClass ��Ҫ������Ľӿڵ����Ͷ���
     * @param <T>            ��Ӧ�ӿڵĴ������
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<?> interfaceClass) {
        T proxy = (T) Proxy.newProxyInstance(RpcProxy.class.getClassLoader(), new Class<?>[]{interfaceClass},
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        logger.info("׼������RPCRequest����...");
                        // ����RpcRequest
                        RpcRequest request = new RpcRequest();
                        //����requestId
                        request.setRequestId(UUID.randomUUID().toString());
                        // ���ýӿ�
                        String interfaceName = method.getDeclaringClass().getName();
                        request.setClassName(interfaceName);
                        request.setMethodName(method.getName());
                        request.setParameterTypes(method.getParameterTypes());
                        // ���ò����б�parameters
                        request.setParameters(args);

                        logger.info("RPCRequest���󹹽���ϣ�׼�����ַ���[{}]...", interfaceName);
                        // ���ַ��񣬵õ������ַ����ʽΪ host:port
                        String serverAddress = serviceDiscovery.discoverService(interfaceName);
                        //������񲻴��ڣ�null,����͹���rpc�ͻ��˽���Զ�̵���
                        if (serverAddress == null) {
                            logger.error("����[{}]���ṩ�߲����ڣ����ַ���ʧ��...", interfaceName);
                            return null;
                        } else {
                            logger.info("���ַ�����ϣ�׼�����������ַ[{}]...", serverAddress);
                            //���������ַ
                            String[] array = serverAddress.split(":");
                            String host = array[0];
                            int port = Integer.valueOf(array[1]);

                            logger.info("�����ַ������ϣ�׼������RPC�ͻ���...");
                            //����rpc�ͻ���
                            RpcClient client = new RpcClient(host, port);

                            logger.info("RPC�ͻ��˹�����ϣ�׼����RPC����˷�������...");

                            //��rpc����˷�������,������Ϣ
                            RpcResponse response = client.sendRequest(request);

                            if (response.isError()) {
                                throw response.getError();
                            } else {
                                //���û���쳣���򷵻ص��õĽ��
                                logger.info("[{}]Զ�̹��̵�����ϣ�Զ�̹��̵��óɹ�...", interfaceName);
                                return response.getResult();
                            }
                        }
                    }
                });
        return proxy;
    }
}
