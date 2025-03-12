package org.lcb.test;

import org.apache.dubbo.common.extension.Adaptive;

/**
 * @author changbao.l Date: 2025-03-12 Time: 10:08
 * @version $
 */
@Adaptive
public class BigDog implements Dog {

    public Person person;

    @Override
    public void say() {
        System.out.println("big汪汪汪");
    }

    public void setPerson(Person person) {
        this.person = person;
    }
}
