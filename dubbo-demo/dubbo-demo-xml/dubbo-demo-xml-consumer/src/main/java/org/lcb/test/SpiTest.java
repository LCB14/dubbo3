package org.lcb.test;

import org.apache.dubbo.common.extension.ExtensionLoader;

/**
 * @author changbao.l Date: 2025-03-12 Time: 10:10
 * @version $
 */
public class SpiTest {
    public static void main(String[] args) {
        Person adaptiveExtension = ExtensionLoader.getExtensionLoader(Person.class).getAdaptiveExtension();
        adaptiveExtension.sayHello();

        Man man = (Man)adaptiveExtension;
        man.dog.say();

        BigDog bigDog = (BigDog)man.dog;
        bigDog.person.sayHello();
    }
}
