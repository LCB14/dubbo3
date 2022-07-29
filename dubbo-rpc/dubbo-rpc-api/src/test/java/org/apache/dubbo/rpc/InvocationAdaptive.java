package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

@SPI
public interface InvocationAdaptive {

    @Adaptive
    String sayHello(URL url, Invocation invocation);
}
