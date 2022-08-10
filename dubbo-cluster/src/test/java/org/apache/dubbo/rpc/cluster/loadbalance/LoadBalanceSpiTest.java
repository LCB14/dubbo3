package org.apache.dubbo.rpc.cluster.loadbalance;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.cluster.LoadBalance;
import org.junit.jupiter.api.Test;

public class LoadBalanceSpiTest {
    @Test
    public void spiAdaptiveTest(){
        LoadBalance adaptiveExtension = ExtensionLoader.getExtensionLoader(LoadBalance.class).getAdaptiveExtension();
    }
}
