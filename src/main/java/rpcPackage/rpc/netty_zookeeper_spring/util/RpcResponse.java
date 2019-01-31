package rpcPackage.rpc.netty_zookeeper_spring.util;

/**
 * Created by wangchaohui on 2018/3/16.
 */
public class RpcResponse {
    private String requestId;

    private Throwable error;

    private Object result;

    public boolean isError() {
        return error != null;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
