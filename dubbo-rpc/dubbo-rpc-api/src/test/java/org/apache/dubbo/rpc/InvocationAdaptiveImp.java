package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;

public class InvocationAdaptiveImp implements InvocationAdaptive{

    @Override
    public String sayHello(URL url, Invocation invocation) {
        return "Hello SPI Adaptive";
    }
}
