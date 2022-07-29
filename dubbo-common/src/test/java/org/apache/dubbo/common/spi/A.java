package org.apache.dubbo.common.spi;

import org.apache.dubbo.common.extension.Adaptive;

public class A implements X {
    private X a;

    @Override
    public void say() {
        System.out.println("china" + ":" + a);
    }

    /**
     * 自依赖
     */
    public void setA(X a) {
        this.a = a;
    }
}
