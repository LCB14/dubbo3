package org.apache.dubbo.common.spi;

import org.apache.dubbo.common.extension.Adaptive;

public class C implements X {
    private X b;

    @Override
    public void say() {
        System.out.println("England" + ":" + b);
    }

    /**
     * 循环依赖
     */
    public void setB(X b) {
        this.b = b;
    }
}
