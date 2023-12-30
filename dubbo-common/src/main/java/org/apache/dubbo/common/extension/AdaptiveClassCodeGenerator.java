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
package org.apache.dubbo.common.extension;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.StringUtils;

/**
 * Code generator for Adaptive class
 */
public class AdaptiveClassCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveClassCodeGenerator.class);

    private static final String CLASSNAME_INVOCATION = "org.apache.dubbo.rpc.Invocation";

    private static final String CODE_PACKAGE = "package %s;\n";

    private static final String CODE_IMPORTS = "import %s;\n";

    private static final String CODE_CLASS_DECLARATION = "public class %s$Adaptive implements %s {\n";

    private static final String CODE_METHOD_DECLARATION = "public %s %s(%s) %s {\n%s}\n";

    private static final String CODE_METHOD_ARGUMENT = "%s arg%d";

    private static final String CODE_METHOD_THROWS = "throws %s";

    private static final String CODE_UNSUPPORTED = "throw new UnsupportedOperationException(\"The method %s of interface %s is not adaptive method!\");\n";

    private static final String CODE_URL_NULL_CHECK = "if (arg%d == null) throw new IllegalArgumentException(\"url == null\");\n%s url = arg%d;\n";

    private static final String CODE_EXT_NAME_ASSIGNMENT = "String extName = %s;\n";

    private static final String CODE_EXT_NAME_NULL_CHECK = "if(extName == null) "
            + "throw new IllegalStateException(\"Failed to get extension (%s) name from url (\" + url.toString() + \") use keys(%s)\");\n";

    private static final String CODE_INVOCATION_ARGUMENT_NULL_CHECK = "if (arg%d == null) throw new IllegalArgumentException(\"invocation == null\"); "
            + "String methodName = arg%d.getMethodName();\n";


    private static final String CODE_EXTENSION_ASSIGNMENT = "%s extension = (%<s)%s.getExtensionLoader(%s.class).getExtension(extName);\n";

    private static final String CODE_EXTENSION_METHOD_INVOKE_ARGUMENT = "arg%d";

    private final Class<?> type;

    private String defaultExtName;

    public AdaptiveClassCodeGenerator(Class<?> type, String defaultExtName) {
        this.type = type;
        this.defaultExtName = defaultExtName;
    }

    /**
     * test if given type has at least one method annotated with <code>SPI</code>
     */
    private boolean hasAdaptiveMethod() {
        return Arrays.stream(type.getMethods()).anyMatch(m -> m.isAnnotationPresent(Adaptive.class));
    }

    /**
     * generate and return class code
     */
    public String generate() {
        // no need to generate adaptive class since there's no adaptive method found.
        // 获取目标拓展接口对应的自适应实现（在不存在被@Adaptive注解修饰的类的前提喜爱），如果也不存在被@Adaptive注解修饰的方法，则认为该拓展接口不支持自适应拓展。
        if (!hasAdaptiveMethod()) {
            throw new IllegalStateException("No adaptive method exist on extension " + type.getName() + ", refuse to create the adaptive class!");
        }

        StringBuilder code = new StringBuilder();
        code.append(generatePackageInfo());
        code.append(generateImports());
        code.append(generateClassDeclaration());

        Method[] methods = type.getMethods();
        for (Method method : methods) {
            code.append(generateMethod(method));
        }
        code.append("}");

        if (logger.isDebugEnabled()) {
            logger.debug(code.toString());
        }

        /**
         * 下面动态代理生成的代码源于：
         * @see org.apache.dubbo.rpc.cluster.loadbalance.LoadBalanceSpiTest#spiAdaptiveTest()
         *
         * package org.apache.dubbo.rpc.cluster;
         *
         * import org.apache.dubbo.common.extension.ExtensionLoader;
         *
         * public class LoadBalance$Adaptive implements org.apache.dubbo.rpc.cluster.LoadBalance {
         *     public org.apache.dubbo.rpc.Invoker select(java.util.List arg0, org.apache.dubbo.common.URL arg1, org.apache.dubbo.rpc.Invocation arg2) throws org.apache.dubbo.rpc.RpcException {
         *         if (arg1 == null)
         *             throw new IllegalArgumentException("url == null");
         *         org.apache.dubbo.common.URL url = arg1;
         *
         *         if (arg2 == null)
         *             throw new IllegalArgumentException("invocation == null");
         *         String methodName = arg2.getMethodName();
         *
         *         String extName = url.getMethodParameter(methodName, "loadbalance", "random");
         *         if (extName == null)
         *             throw new IllegalStateException("Failed to get extension (org.apache.dubbo.rpc.cluster.LoadBalance) name from url (" + url.toString() + ") use keys([loadbalance])");
         *
         *        org.apache.dubbo.rpc.cluster.LoadBalance extension = (org.apache.dubbo.rpc.cluster.LoadBalance) ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.cluster.LoadBalance.class).getExtension(extName);
         *         return extension.select(arg0, arg1, arg2);
         *     }
         * }
         */
        return code.toString();
    }

    /**
     * generate package info
     */
    private String generatePackageInfo() {
        return String.format(CODE_PACKAGE, type.getPackage().getName());
    }

    /**
     * generate imports
     */
    private String generateImports() {
        return String.format(CODE_IMPORTS, ExtensionLoader.class.getName());
    }

    /**
     * generate class declaration
     */
    private String generateClassDeclaration() {
        return String.format(CODE_CLASS_DECLARATION, type.getSimpleName(), type.getCanonicalName());
    }

    /**
     * generate method not annotated with Adaptive with throwing unsupported exception
     */
    private String generateUnsupported(Method method) {
        return String.format(CODE_UNSUPPORTED, method, type.getName());
    }

    /**
     * get index of parameter with type URL
     */
    private int getUrlTypeIndex(Method method) {
        int urlTypeIndex = -1;
        Class<?>[] pts = method.getParameterTypes();
        for (int i = 0; i < pts.length; ++i) {
            if (pts[i].equals(URL.class)) {
                urlTypeIndex = i;
                break;
            }
        }
        return urlTypeIndex;
    }

    /**
     * generate method declaration
     */
    private String generateMethod(Method method) {
        String methodReturnType = method.getReturnType().getCanonicalName();
        String methodName = method.getName();
        String methodContent = generateMethodContent(method);
        String methodArgs = generateMethodArguments(method);
        String methodThrows = generateMethodThrows(method);
        return String.format(CODE_METHOD_DECLARATION, methodReturnType, methodName, methodArgs, methodThrows, methodContent);
    }

    /**
     * generate method arguments
     */
    private String generateMethodArguments(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        return IntStream.range(0, pts.length)
                .mapToObj(i -> String.format(CODE_METHOD_ARGUMENT, pts[i].getCanonicalName(), i))
                .collect(Collectors.joining(", "));
    }

    /**
     * generate method throws
     */
    private String generateMethodThrows(Method method) {
        Class<?>[] ets = method.getExceptionTypes();
        if (ets.length > 0) {
            String list = Arrays.stream(ets).map(Class::getCanonicalName).collect(Collectors.joining(", "));
            return String.format(CODE_METHOD_THROWS, list);
        } else {
            return "";
        }
    }

    /**
     * generate method URL argument null check
     */
    private String generateUrlNullCheck(int index) {
        return String.format(CODE_URL_NULL_CHECK, index, URL.class.getName(), index);
    }

    /**
     * generate method content
     */
    private String generateMethodContent(Method method) {
        Adaptive adaptiveAnnotation = method.getAnnotation(Adaptive.class);
        StringBuilder code = new StringBuilder(512);
        if (adaptiveAnnotation == null) {
            // 代理接口中，未被@Adaptive注解修饰的方法，生成的代理类实现直接抛出异常
            return generateUnsupported(method);
        } else {
            // 获取当前被@Adaptive注解修饰的方法参数中，参数类型为URL的参数在所有参数中的次序号。
            int urlTypeIndex = getUrlTypeIndex(method);

            // found parameter in URL type
            if (urlTypeIndex != -1) {
                // Null Point check
                /**
                 * 生成代码参考：
                 * if (arg1 == null)
                 *     throw new IllegalArgumentException("url == null");
                 * com.alibaba.dubbo.common.URL url = arg1;
                 */
                code.append(generateUrlNullCheck(urlTypeIndex));
            } else {
                // did not find parameter in URL type
                /**
                 * 生成代码参考：
                 * if (arg0 == null)
                 *     throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument == null");
                 * if (arg0.getUrl() == null)
                 *     throw new IllegalArgumentException("com.alibaba.dubbo.rpc.Invoker argument getUrl() == null");
                 * com.alibaba.dubbo.common.URL url = arg0.getUrl();
                 */
                code.append(generateUrlAssignmentIndirectly(method));
            }

            // 如果@Adaptice注解未指定value值，就使用被代理接口的名称（例如接口名称为：xY，处理后：x.y）作为value值
            String[] value = getMethodAdaptiveValue(adaptiveAnnotation);

            // 判断被代理的方法是否含有Invocation类型的参数
            boolean hasInvocation = hasInvocationArgument(method);

            // 生成Invocation类型参数的非空判断
            code.append(generateInvocationArgumentNullCheck(method));

            // 重点：生成拓展名即获取真正拓展接口实现类的key
            code.append(generateExtNameAssignment(value, hasInvocation));

            // check extName == null?
            code.append(generateExtNameNullCheck(value));

            // 生成通过拓展名获取目标拓展实现类的代码
            code.append(generateExtensionAssignment());

            // return statement （生成目标代理方法的调用逻辑代码）
            code.append(generateReturnAndInvocation(method));
        }

        return code.toString();
    }

    /**
     * generate code for variable extName null check
     */
    private String generateExtNameNullCheck(String[] value) {
        return String.format(CODE_EXT_NAME_NULL_CHECK, type.getName(), Arrays.toString(value));
    }

    /**
     * generate extName assigment code
     */
    private String generateExtNameAssignment(String[] value, boolean hasInvocation) {
        // TODO: refactor it
        String getNameCode = null;
        for (int i = value.length - 1; i >= 0; --i) {
            // value.length 等于1或第一次遍历时该条件才会成立
            if (i == value.length - 1) {
                /**
                 * defaultExtName 变量的初始化位置(defaultExtName 变量的值来源于@SPI注解中指定)
                 * @see ExtensionLoader#createAdaptiveExtensionClass()
                 */
                if (null != defaultExtName) {
                    // 通过下面源码阅读可知，可以在 @Adaptive 注解中通过 @Adaptive("protocol") 来明确指定拓展实现类名称在URL中的取值属性。
                    if (!"protocol".equals(value[i])) {
                        /**
                         * Dubbo 生成拓展名逻辑, 为什么受 Invocation 类型参数影响?
                         * 因为目标拓展的代理方法中关于拓展名的获取，需要借助Invocation#getMethodName()返回的值作为参数，从URL信息中提取。
                         *
                         * @see URL#getMethodParameter(String, String) 获取拓展名方法中的第一个参数值就是来源于：
                         * @see org.apache.dubbo.rpc.Invocation#getMethodName()
                         */
                        if (hasInvocation) {
                            /**
                             * methodName 初始化位置如下，来源于Invocation类型参数的getMethodName()方法
                             * @see AdaptiveClassCodeGenerator#generateInvocationArgumentNullCheck(Method)
                             *
                             * value[i] 初始化位置如下，来源于@Adaptive注解指定或被代理接口名称的转换（代理接口：xY，转换成 x.y ）
                             * @see AdaptiveClassCodeGenerator#getMethodAdaptiveValue(Adaptive)
                             *
                             * defaultExtName 值来源于cachedDefaultName，cachedDefaultName来源于@SPI注解中指定
                             * @see org.apache.dubbo.common.extension.ExtensionLoader#createAdaptiveExtensionClass()
                             *
                             * url.getMethodParameter 调用方法参考
                             * @see URL#getMethodParameter(String, String, String)
                             */
                            getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                        } else {
                            getNameCode = String.format("url.getParameter(\"%s\", \"%s\")", value[i], defaultExtName);
                        }
                    } else {
                        getNameCode = String.format("( url.getProtocol() == null ? \"%s\" : url.getProtocol() )", defaultExtName);
                    }
                } else {
                    if (!"protocol".equals(value[i])) {
                        if (hasInvocation) {
                            /**
                             * 此处可能导致 getNameCode 值为 null，不过还好，在 generateExtNameNullCheck 方法中有生成针对拓展名非空的校验
                             * @see org.apache.dubbo.common.extension.AdaptiveClassCodeGenerator.generateExtNameNullCheck
                             */
                            getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                        } else {
                            /**
                             * url.getParameter 调用方法参考
                             * @see URL#getParameter(String)
                             */
                            getNameCode = String.format("url.getParameter(\"%s\")", value[i]);
                        }
                    } else {
                        getNameCode = "url.getProtocol()";
                    }
                }
            } else {
                if (!"protocol".equals(value[i])) {
                    if (hasInvocation) {
                        getNameCode = String.format("url.getMethodParameter(methodName, \"%s\", \"%s\")", value[i], defaultExtName);
                    } else {
                        getNameCode = String.format("url.getParameter(\"%s\", %s)", value[i], getNameCode);
                    }
                } else {
                    getNameCode = String.format("url.getProtocol() == null ? (%s) : url.getProtocol()", getNameCode);
                }
            }
        }

        return String.format(CODE_EXT_NAME_ASSIGNMENT, getNameCode);
    }

    /**
     * @return
     */
    private String generateExtensionAssignment() {
        return String.format(CODE_EXTENSION_ASSIGNMENT, type.getName(), ExtensionLoader.class.getSimpleName(), type.getName());
    }

    /**
     * generate method invocation statement and return it if necessary
     */
    private String generateReturnAndInvocation(Method method) {
        String returnStatement = method.getReturnType().equals(void.class) ? "" : "return ";

        String args = IntStream.range(0, method.getParameters().length)
                .mapToObj(i -> String.format(CODE_EXTENSION_METHOD_INVOKE_ARGUMENT, i))
                .collect(Collectors.joining(", "));

        return returnStatement + String.format("extension.%s(%s);\n", method.getName(), args);
    }

    /**
     * test if method has argument of type <code>Invocation</code>
     */
    private boolean hasInvocationArgument(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        return Arrays.stream(pts).anyMatch(p -> CLASSNAME_INVOCATION.equals(p.getName()));
    }

    /**
     * generate code to test argument of type <code>Invocation</code> is null
     */
    private String generateInvocationArgumentNullCheck(Method method) {
        Class<?>[] pts = method.getParameterTypes();
        return IntStream.range(0, pts.length).filter(i -> CLASSNAME_INVOCATION.equals(pts[i].getName()))
                .mapToObj(i -> String.format(CODE_INVOCATION_ARGUMENT_NULL_CHECK, i, i))
                .findFirst().orElse("");
    }

    /**
     * get value of adaptive annotation or if empty return splitted simple name
     */
    private String[] getMethodAdaptiveValue(Adaptive adaptiveAnnotation) {
        String[] value = adaptiveAnnotation.value();
        // value is not set, use the value generated from class name as the key
        if (value.length == 0) {
            String splitName = StringUtils.camelToSplitName(type.getSimpleName(), ".");
            value = new String[]{splitName};
        }
        return value;
    }

    /**
     * get parameter with type <code>URL</code> from method parameter:
     * <p>
     * test if parameter has method which returns type <code>URL</code>
     * <p>
     * if not found, throws IllegalStateException
     */
    private String generateUrlAssignmentIndirectly(Method method) {
        Class<?>[] pts = method.getParameterTypes();

        // find URL getter method
        for (int i = 0; i < pts.length; ++i) {
            for (Method m : pts[i].getMethods()) {
                String name = m.getName();
                if ((name.startsWith("get") || name.length() > 3)
                        && Modifier.isPublic(m.getModifiers())
                        && !Modifier.isStatic(m.getModifiers())
                        && m.getParameterTypes().length == 0
                        && m.getReturnType() == URL.class) {
                    return generateGetUrlNullCheck(i, pts[i], name);
                }
            }
        }

        // getter method not found, throw
        throw new IllegalStateException("Failed to create adaptive class for interface " + type.getName()
                + ": not found url parameter or url attribute in parameters of method " + method.getName());

    }

    /**
     * 1, test if argi is null
     * 2, test if argi.getXX() returns null
     * 3, assign url with argi.getXX()
     */
    private String generateGetUrlNullCheck(int index, Class<?> type, String method) {
        // Null point check
        StringBuilder code = new StringBuilder();
        code.append(String.format("if (arg%d == null) throw new IllegalArgumentException(\"%s argument == null\");\n",
                index, type.getName()));
        code.append(String.format("if (arg%d.%s() == null) throw new IllegalArgumentException(\"%s argument %s() == null\");\n",
                index, method, type.getName(), method));

        code.append(String.format("%s url = arg%d.%s();\n", URL.class.getName(), index, method));
        return code.toString();
    }

}
