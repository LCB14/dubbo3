package org.apache.dubbo.common.spi;

import org.apache.dubbo.common.extension.ExtensionLoader;
import org.junit.jupiter.api.Test;

public class SPITest {
    @Test
    public void selfDependenceTest() {
        X a = ExtensionLoader.getExtensionLoader(X.class).getExtension("a");
        System.out.println(a);
        a.say();
    }

    @Test
    public void cycleDependenceTest() {
        X b = ExtensionLoader.getExtensionLoader(X.class).getExtension("b");
        System.out.println(b);
        b.say();
    }
}
