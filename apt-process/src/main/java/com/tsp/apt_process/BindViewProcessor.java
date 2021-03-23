package com.tsp.apt_process;


import com.tsp.apt_annotation.BindView;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

/**
 * https://blog.csdn.net/cpcpcp123/article/details/103871815
 * https://blog.csdn.net/u013045971/article/details/53433874?spm=1001.2014.3001.5501
 * 需要手动新建目录和相应的文件
 */

@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("com.tsp.apt_annotation.BindView")
public class BindViewProcessor extends AbstractProcessor {

    private Filer mFilerUtils;       // 文件管理工具类
    private Types mTypesUtils;    // 类型处理工具类
    private Elements mElementsUtils;  // Element处理工具类
    private Map<TypeElement, Set<ViewInfo>> mToBindMap = new HashMap<>(); //用于记录需要绑定的View的名称和对应的id


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFilerUtils = processingEnv.getFiler();
        mTypesUtils = processingEnv.getTypeUtils();
        mElementsUtils = processingEnv.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        System.out.println("start process");
        if (set != null && set.size() != 0) {
            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindView.class);//获得被BindView注解标记的element

            categories(elements);//对不同的Activity进行分类

            //对不同的Activity生成不同的帮助类
            for (TypeElement typeElement : mToBindMap.keySet()) {
                String code = generateCode(typeElement);    //获取要生成的帮助类中的所有代码
                String helperClassName = typeElement.getQualifiedName() + "$$Autobind"; //构建要生成的帮助类的类名
                //输出帮助类的java文件，在这个例子中就是MainActivity$$Autobind.java文件
                //输出的文件在build->source->apt->目录下
                try {
                    JavaFileObject jfo = mFilerUtils.createSourceFile(helperClassName, typeElement);
                    Writer writer = jfo.openWriter();
                    writer.write(code);
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }


    private void categories(Set<? extends Element> elements) {
        for (Element element : elements) {
            VariableElement variableElement = (VariableElement) element;    //被@BindView标注的应当是变量，这里简单的强制类型转换
            TypeElement enclosingElement = (TypeElement) variableElement.getEnclosingElement(); //获取代表Activity的TypeElement
            Set<ViewInfo> views = mToBindMap.get(enclosingElement); //views储存着一个Activity中将要绑定的view的信息
            if (views == null) {    //如果views不存在就new一个
                views = new HashSet<>();
                mToBindMap.put(enclosingElement, views);
            }
            BindView bindAnnotation = variableElement.getAnnotation(BindView.class);    //获取到一个变量的注解
            int id = bindAnnotation.value();    //取出注解中的value值，这个值就是这个view要绑定的xml中的id
            views.add(new ViewInfo(variableElement.getSimpleName().toString(), id));    //把要绑定的View的信息存进views中
        }
    }

    /**
     * 字符串拼接 生成代码
     *
     * @param typeElement
     * @return
     */
    private String generateCode(TypeElement typeElement) {
        //1.获取需要绑定的view所在的类名称
        String rawClassName = typeElement.getSimpleName().toString();
        //2.获取需要绑定的view所在的包名
        String packageName = mElementsUtils.getPackageOf(typeElement).getQualifiedName().toString();
        //3.生成需要帮助的类名称
        String helperClassName = rawClassName + "$$Autobind";
        //4.生成代码
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n");   //构建定义包的代码
        sb.append("import com.tsp.apt_api.template.IBindHelper;\n\n");//导包
        sb.append("public class ").append(helperClassName).append(" implements ").append("IBindHelper");   //构建定义帮助类的代码
        sb.append(" {\n"); //代码格式，可以忽略
        sb.append("\t@Override\n");    //声明这个方法为重写IBindHelper中的方法
        sb.append("\tpublic void inject(" + "Object" + " target ) {\n");   //构建方法的代码
        for (ViewInfo viewInfo : mToBindMap.get(typeElement)) { //遍历每一个需要绑定的view
            sb.append("\t\t"); //代码格式，可以忽略
            sb.append(rawClassName + " substitute = " + "(" + rawClassName + ")" + "target;\n");    //强制类型转换
            sb.append("\t\t"); //代码格式，可以忽略
            sb.append("substitute." + viewInfo.viewName).append(" = ");    //构建赋值表达式
            sb.append("substitute.findViewById(" + viewInfo.id + ");\n");  //构建赋值表达式
        }
        sb.append("\t}\n");    //代码格式，可以忽略
        sb.append('\n');   //代码格式，可以忽略
        sb.append("}\n");  //代码格式，可以忽略
        return sb.toString();
    }


    //要绑定的View的信息载体
    class ViewInfo {
        String viewName;    //view的变量名
        int id; //xml中的id

        public ViewInfo(String viewName, int id) {
            this.viewName = viewName;
            this.id = id;
        }
    }
}