package rpcPackage.rpc.netty_zookeeper_spring.server;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wangchaohui on 2018/3/16.
 */
@Target({ElementType.TYPE})   // 自定义注解的使用范围，ElementType.TYPE表示自定义的注解可以用在类或接口上
@Retention(RetentionPolicy.RUNTIME) // 注解的可见范围，RetentionPolicy.RUNTIME表示自定义注解在虚拟机运行期间也可见
@Component
public @interface RpcService {
    Class<?> value();
}
