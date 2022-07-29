package org.apache.dubbo.rpc;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;

public class InvocationAdaptiveTest {
    public static void main(String[] args) {
        InvocationAdaptive adaptiveExtension = ExtensionLoader.getExtensionLoader(InvocationAdaptive.class).getAdaptiveExtension();
        URL url = URL.valueOf("/context/path?version=1.0.0&application=morgan");
        adaptiveExtension.sayHello(url,new RpcInvocation());
    }
}
