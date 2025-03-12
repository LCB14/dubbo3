package org.lcb.test;

import org.apache.dubbo.common.extension.Adaptive;

/**
 * @author changbao.l Date: 2025-03-12 Time: 10:07
 * @version $
 */
@Adaptive
public class Man implements Person{

    public Dog dog;

    @Override
    public void sayHello() {
        System.out.println("hello, I am a man");
    }

    public void setDog(Dog dog) {
        this.dog = dog;
    }
}
