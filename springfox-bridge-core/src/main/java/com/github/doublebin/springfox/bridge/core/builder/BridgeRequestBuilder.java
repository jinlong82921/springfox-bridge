package com.github.doublebin.springfox.bridge.core.builder;

import com.github.doublebin.springfox.bridge.core.SpringfoxBridge;
import com.github.doublebin.springfox.bridge.core.builder.annotations.BridgeModelProperty;
import com.github.doublebin.springfox.bridge.core.util.FileUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javassist.*;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.*;
import lombok.extern.slf4j.Slf4j;
import com.github.doublebin.springfox.bridge.core.exception.BridgeException;
import com.github.doublebin.springfox.bridge.core.util.JavassistUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class BridgeRequestBuilder {

    private static final ClassPool pool = ClassPool.getDefault();

    public static Class newRequestClass(Method method, String simpleClassName) {
        Parameter[] parameters = method.getParameters();

        String newClassName = BridgeClassNameBuilder.buildNewClassName(BridgeClassNameBuilder.NEW_REQUEST_CLASS_NAME_PRE, simpleClassName);


        try {

            CtClass newCtClass = pool.makeClass(newClassName);

            ClassFile ccFile = newCtClass.getClassFile();
            ConstPool constpool = ccFile.getConstPool();

            int i = 0;

            for (Parameter parameter : parameters) {
                Class parameterClass = parameter.getType();

                Annotation apiModelAnnotation = getApiModelAnnotation(constpool);
                JavassistUtil.addAnnotationForCtClass(newCtClass, apiModelAnnotation);

                CtField ctField = new CtField(pool.get(parameterClass.getName()), "param" + i, newCtClass); //
                ctField.setModifiers(Modifier.PRIVATE);
                newCtClass.addField(ctField); //添加属性

                Annotation apiModelParopertyAnnotation = getApiModelPropertyAnnotation(parameter, constpool);
                JavassistUtil.addAnnotationForCtField(ctField, apiModelParopertyAnnotation);

                JavassistUtil.addGetterForCtField(ctField);

                JavassistUtil.addSetterForCtField(ctField);

                i++;

            }
            newCtClass.writeFile(SpringfoxBridge.getBridgeClassFilePath());

            return newCtClass.toClass();

        } catch (Exception e) {

            String parameterInfo = "";
            for(Parameter parameter: parameters){
                parameterInfo+=parameter.toString()+"|";
            }
            log.error("new request class failed, simple class name is {}, parameters are {}.", simpleClassName, parameterInfo, e);
            throw new BridgeException(e);
        }
    }

    private static Annotation getApiModelPropertyAnnotation(Parameter parameter, ConstPool constpool) {
        BridgeModelProperty bridgeModelProperty = parameter.getAnnotation(BridgeModelProperty.class);

        String[] annotationMethodNames = new String[]{"value", "name", "allowableValues", "access", "notes",
                "dataType", "required", "position", "hidden", "example", "readOnly", "reference"};

       return JavassistUtil.copyAnnotationValues(bridgeModelProperty, ApiModelProperty.class, constpool, annotationMethodNames);
    }

    private static Annotation getApiModelAnnotation(ConstPool constpool) {
        Annotation apiModelAnnotation = new Annotation(ApiModel.class.getName(), constpool);
        apiModelAnnotation.addMemberValue("description",
            new StringMemberValue("Assembled request class, desc.", constpool)); //TODO D
        return apiModelAnnotation;
    }


}
