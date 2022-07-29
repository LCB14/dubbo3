package org.apache.dubbo.common.spi;

import org.apache.dubbo.common.extension.Adaptive;

public class B implements X {
    private X c;

    @Override
    public void say() {
        System.out.println("US" + ":" + c);
    }

    /**
     * 循环依赖
     */
    public void setC(X c) {
        this.c = c;
    }
}
