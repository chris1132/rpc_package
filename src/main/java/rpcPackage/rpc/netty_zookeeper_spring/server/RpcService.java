package rpcPackage.rpc.netty_zookeeper_spring.server;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wangchaohui on 2018/3/16.
 */
@Target({ElementType.TYPE})   // �Զ���ע���ʹ�÷�Χ��ElementType.TYPE��ʾ�Զ����ע������������ӿ���
@Retention(RetentionPolicy.RUNTIME) // ע��Ŀɼ���Χ��RetentionPolicy.RUNTIME��ʾ�Զ���ע��������������ڼ�Ҳ�ɼ�
@Component
public @interface RpcService {
    Class<?> value();
}
