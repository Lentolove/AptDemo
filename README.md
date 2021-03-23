# AptDemo
## 1.[概念篇](https://juejin.cn/post/6844903437834911757)

### 什么是注解

先来看看Java文档中的定义

> An annotation is a form of metadata, that can be added to Java source code. Classes, methods, variables, parameters and packages may be annotated. Annotations have no direct effect on the operation of the code they annotate.

注解是一种元数据, 可以添加到java代码中. **类**、**方法**、**变量**、**参数**、**包**都可以被注解，注解对注解的代码没有直接影响. 

首先, 明确一点: **注解并没有什么魔法, 之所以产生作用, 是对其解析后做了相应的处理. 注解仅仅只是个标记罢了.**

定义注解用的关键字是`@interface`

### 元注解

java内置的注解有Override, Deprecated, SuppressWarnings等, 作用相信大家都知道. 

现在查看Override注解的源码

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
```

发现Override注解上面有两个注解, 这就是元注解. 元注解就是**用来定义注解的注解**.其作用就是定义注解的作用范围, 使用在什么元素上等等, 下面来详细介绍. 

元注解共有四种`@Retention`, `@Target`, `@Inherited`, `@Documented`

- `@Retention` 保留的范围，默认值为CLASS. 可选值有三种

  - `SOURCE`, 只在源码中可用

    注解只保留在源文件，当Java文件编译成class文件的时候，注解被遗弃；用于做一些检查性的操作，比如 `@Override` 和 `@SuppressWarnings`

  - `CLASS`, 在源码和字节码中可用

    注解被保留到class文件，但jvm加载class文件时候被遗弃，这是默认的生命周期；用于在编译时进行一些预处理操作，比如生成一些辅助代码（如 `ButterKnife`）

  - `RUNTIME`, 在源码,字节码,运行时均可用

    注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在；用于在运行时去动态获取注解信息。这个注解大都会与反射一起使用

- `@Target` 可以用来修饰哪些程序元素，如 `TYPE`, `METHOD`, `CONSTRUCTOR`, `FIELD`, `PARAMETER`等，未标注则表示可修饰所有

- `@Inherited` 是否可以被继承，默认为false

- `@Documented` 是否会保存到 Javadoc 文档中

其中, `@Retention`是定义保留策略, 直接决定了我们用何种方式解析. SOUCE级别的注解是用来标记的, 比如Override, SuppressWarnings. 我们真正使用的类型是CLASS(编译时)和RUNTIME(运行时)

### 自定义注解

举个栗子, 结合例子讲解

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TestAnnotation {
    String value();
    String[] value2() default "value2";
}
```

元注解的的意义参考上面的讲解, 不再重复, 这里看注解值的写法: 

```java
类型 参数名() default 默认值;
```

其中默认值是可选的, 可以定义, 也可以不定义. 

### 处理运行时注解

Retention的值为**RUNTIME**时, 注解会保留到运行时, **因此使用反射来解析注解.** 

使用的注解就是上一步的`@TestAnnotation`, 解析示例如下: 

```java
public class Demo {

    @TestAnnotation("Hello Annotation!")
    private String testAnnotation;

    public static void main(String[] args) {
        try {
            // 获取要解析的类
            Class cls = Class.forName("myAnnotation.Demo");
            // 拿到所有Field
            Field[] declaredFields = cls.getDeclaredFields();
            for(Field field : declaredFields){
                // 获取Field上的注解
                TestAnnotation annotation = field.getAnnotation(TestAnnotation.class);
                if(annotation != null){
                    // 获取注解值
                    String value = annotation.value();
                    System.out.println(value);
                }

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
```

此处只演示了解析成员变量上的注解, 其他类型与此类似. 

### 解析编译时注解

**解析编译时注解需要继承AbstractProcessor类, 实现其抽象方法**

```java
public boolean process(Set annotations, RoundEnvironment roundEnv)
```

该方法返回ture表示该注解已经被处理, 后续不会再有其他处理器处理; 返回false表示仍可被其他处理器处理. 

处理示例: 

```java
// 指定要解析的注解
@SupportedAnnotationTypes("myAnnotation.TestAnnotation")
// 指定JDK版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class MyAnnotationProcesser extends AbstractProcessor {
    @Override
    public boolean process(Set annotations, RoundEnvironment roundEnv) {
        for (TypeElement te : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(te)) {
                TestAnnotation testAnnotation = element.getAnnotation(TestAnnotation.class);
                // do something
            }
        }
        return true;
    }
}
```

这里先大致介绍是怎么个套路, 接下来说具体实践过程.