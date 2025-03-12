package org.lcb.test;

import org.apache.dubbo.common.extension.SPI;

/**
 * @author changbao.l Date: 2025-03-12 Time: 10:08
 * @version $
 */
@SPI
public interface Dog {
    void say();
}
