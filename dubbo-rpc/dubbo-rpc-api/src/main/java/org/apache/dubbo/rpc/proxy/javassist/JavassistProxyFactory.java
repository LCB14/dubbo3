/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.rpc.proxy.javassist;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.bytecode.Proxy;
import org.apache.dubbo.common.bytecode.Wrapper;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.proxy.AbstractProxyFactory;
import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;
import org.apache.dubbo.rpc.proxy.InvokerInvocationHandler;

/**
 * JavaassistRpcProxyFactory
 */
public class JavassistProxyFactory extends AbstractProxyFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        // 生成 Proxy 子类（Proxy 是抽象类）。并调用 Proxy 子类的 newInstance 方法创建 Proxy 实例
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        // TODO Wrapper cannot handle this scenario correctly: the classname contains '$'
        // Wrapper 是一个抽象类，仅可通过 getWrapper(Class) 方法创建子类。
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);

        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName,
                                      Class<?>[] parameterTypes,
                                      Object[] arguments) throws Throwable {
                /**
                 * Wrapper 是一个抽象类，其中 invokeMethod 是一个抽象方法。Dubbo 会在运行时通过 Javassist 框架为 Wrapper 生成实现类，
                 * 并实现 invokeMethod 方法，该方法最终会根据调用信息调用具体的服务。以 DemoServiceImpl 为例，Javassist 为其生成的代理类如下:
                 * package org.apache.dubbo.common.bytecode;
                 *
                 * import java.lang.reflect.InvocationTargetException;
                 * import java.util.Map;
                 * import org.apache.dubbo.common.bytecode.ClassGenerator;
                 * import org.apache.dubbo.common.bytecode.NoSuchMethodException;
                 * import org.apache.dubbo.common.bytecode.NoSuchPropertyException;
                 * import org.apache.dubbo.common.bytecode.Wrapper;
                 * import org.apache.dubbo.demo.provider.DemoServiceImpl;
                 *
                 * public class Wrapper1 extends Wrapper implements ClassGenerator.DC {
                 *     public static String[] pns;
                 *     public static Map pts;
                 *     public static String[] mns;
                 *     public static String[] dmns;
                 *     public static Class[] mts0;
                 *     public static Class[] mts1;
                 *     public static Class[] mts2;
                 *     public static Class[] mts3;
                 *     public static Class[] mts4;
                 *
                 *     public String[] getPropertyNames() {
                 *         return pns;
                 *     }
                 *
                 *     public boolean hasProperty(String string) {
                 *         return pts.containsKey(string);
                 *     }
                 *
                 *     public Class getPropertyType(String string) {
                 *         return (Class) pts.get(string);
                 *     }
                 *
                 *     public String[] getMethodNames() {
                 *         return mns;
                 *     }
                 *
                 *     public String[] getDeclaredMethodNames() {
                 *         return dmns;
                 *     }
                 *
                 *     public void setPropertyValue(Object object, String string, Object object2) {
                 *         DemoServiceImpl demoServiceImpl;
                 *         try {
                 *             demoServiceImpl = (DemoServiceImpl) object;
                 *         } catch (Throwable throwable) {
                 *             throw new IllegalArgumentException(throwable);
                 *         }
                 *         if (string.equals("name")) {
                 *             demoServiceImpl.name = (String) object2;
                 *             return;
                 *         }
                 *         if (string.equals("age")) {
                 *             demoServiceImpl.age = (Integer) object2;
                 *             return;
                 *         }
                 *         if (string.equals("age")) {
                 *             demoServiceImpl.setAge((Integer) object2);
                 *             return;
                 *         }
                 *         if (string.equals("name")) {
                 *             demoServiceImpl.setName((String) object2);
                 *             return;
                 *         }
                 *         throw new NoSuchPropertyException(new StringBuffer().append("Not found property \"").append(string).append("\" field or setter method in class org.apache.dubbo.demo.provider.DemoServiceImpl.").toString());
                 *     }
                 *
                 *     public Object getPropertyValue(Object object, String string) {
                 *         DemoServiceImpl demoServiceImpl;
                 *         try {
                 *             demoServiceImpl = (DemoServiceImpl) object;
                 *         } catch (Throwable throwable) {
                 *             throw new IllegalArgumentException(throwable);
                 *         }
                 *         if (string.equals("name")) {
                 *             return demoServiceImpl.name;
                 *         }
                 *         if (string.equals("age")) {
                 *             return demoServiceImpl.age;
                 *         }
                 *         if (string.equals("age")) {
                 *             return demoServiceImpl.getAge();
                 *         }
                 *         if (string.equals("name")) {
                 *             return demoServiceImpl.getName();
                 *         }
                 *         throw new NoSuchPropertyException(new StringBuffer().append("Not found property \"").append(string).append("\" field or setter method in class org.apache.dubbo.demo.provider.DemoServiceImpl.").toString());
                 *     }
                 *
                 *     public Object invokeMethod(Object object, String string, Class[] classArray, Object[] objectArray) throws InvocationTargetException {
                 *         DemoServiceImpl demoServiceImpl;
                 *         try {
                 *             demoServiceImpl = (DemoServiceImpl) object;
                 *         } catch (Throwable throwable) {
                 *             throw new IllegalArgumentException(throwable);
                 *         }
                 *
                 *         try {
                 *             if ("getAge".equals(string) && classArray.length == 0) {
                 *                 return demoServiceImpl.getAge();
                 *             }
                 *             if ("sayHello".equals(string) && classArray.length == 1) {
                 *                 return demoServiceImpl.sayHello((String) objectArray[0]);
                 *             }
                 *             if ("setAge".equals(string) && classArray.length == 1) {
                 *                 demoServiceImpl.setAge((Integer) objectArray[0]);
                 *                 return null;
                 *             }
                 *             if ("getName".equals(string) && classArray.length == 0) {
                 *                 return demoServiceImpl.getName();
                 *             }
                 *             if ("setName".equals(string) && classArray.length == 1) {
                 *                 demoServiceImpl.setName((String) objectArray[0]);
                 *                 return null;
                 *             }
                 *         } catch (Throwable throwable) {
                 *             throw new InvocationTargetException(throwable);
                 *         }
                 *
                 *         throw new NoSuchMethodException(new StringBuffer().append("Not found method \"").append(string).append("\" in class org.apache.dubbo.demo.provider.DemoServiceImpl.").toString());
                 *     }
                 * }
                 */
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
            }
        };
    }

}
