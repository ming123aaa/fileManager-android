package com.ohuang.filemanager.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RefInvoke {


    public static boolean isLog=false;
    public static  Object invokeStaticMethod(String class_name, String method_name, Class[] pareTyple, Object[] pareVaules){
        return invokeStaticMethod(ClassLoader.getSystemClassLoader(),class_name,method_name,pareTyple,pareVaules);
    }
    public static  Object invokeStaticMethod(ClassLoader classLoader,String class_name, String method_name, Class[] pareTyple, Object[] pareVaules){

        try {
            Class obj_class = classLoader.loadClass(class_name);
            Method method = obj_class.getDeclaredMethod(method_name,pareTyple);
            method.setAccessible(true);
            return method.invoke(null, pareVaules);
        } catch (SecurityException | IllegalArgumentException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {

            e.printStackTrace();
        }
        return null;

    }

    public static  Object invokeMethod(String class_name, String method_name, Object obj ,Class[] pareTyple, Object[] pareVaules){

        return invokeMethod(ClassLoader.getSystemClassLoader(),class_name,method_name,obj,pareTyple,pareVaules);

    }

    public static  Object invokeMethod(ClassLoader classLoader,String class_name, String method_name, Object obj ,Class[] pareTyple, Object[] pareVaules){

        try {
            Class obj_class = classLoader.loadClass(class_name);
            //获取类中的所有方法，但不包括继承父类的方法
            Method method = obj_class.getDeclaredMethod(method_name,pareTyple);
            method.setAccessible(true);
            return method.invoke(obj, pareVaules);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassNotFoundException e) {
           if (isLog) {
               e.printStackTrace();
           }
        }

        return null;

    }
    public static Object getFieldOjbect(String class_name,Object obj, String filedName){
        return getFieldOjbect(ClassLoader.getSystemClassLoader(),class_name,obj,filedName);
    }

    public static Object getFieldOjbect(ClassLoader classLoader,String class_name,Object obj, String filedName){
        try {
            Class obj_class = classLoader.loadClass(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException | ClassNotFoundException e) {
            if (isLog) {
                e.printStackTrace();
            }
        }
        return null;

    }
    public static Object getStaticFieldOjbect(String class_name, String filedName){
        return getStaticFieldOjbect(ClassLoader.getSystemClassLoader(),class_name,filedName);
    }

    public static Object getStaticFieldOjbect(ClassLoader classLoader,String class_name, String filedName){

        try {
            Class obj_class = classLoader.loadClass(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            return field.get(null);
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException | ClassNotFoundException e) {
            if (isLog) {
                e.printStackTrace();
            }
        }
        return null;

    }

    public static void setFieldOjbect(String classname, String filedName, Object obj, Object filedVaule){
        setFieldOjbect(ClassLoader.getSystemClassLoader(),classname,filedName,obj,filedVaule);
    }

    public static void setFieldOjbect(ClassLoader classLoader,String classname, String filedName, Object obj, Object filedVaule){
        try {
            Class obj_class = classLoader.loadClass(classname);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            field.set(obj, filedVaule);
        } catch (SecurityException | ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchFieldException e) {
           if (isLog) {
               e.printStackTrace();
           }
        }
    }

    public static void setStaticOjbect(String class_name, String filedName, Object filedVaule){
        setStaticOjbect(ClassLoader.getSystemClassLoader(),class_name,filedName,filedVaule);
    }

    public static void setStaticOjbect(ClassLoader classLoader,String class_name, String filedName, Object filedVaule){
        try {
            Class obj_class = classLoader.loadClass(class_name);
            Field field = obj_class.getDeclaredField(filedName);
            field.setAccessible(true);
            field.set(null, filedVaule);
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException | ClassNotFoundException e) {
          if (isLog) {
              e.printStackTrace();
          }
        }
    }

    public static boolean matchClass(Class<?> aclass,String className,ClassLoader classLoader){
        Class<?> iClass=null;
        try {
            iClass = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (iClass==null){
            return false;
        }
        if (!iClass.isInterface()) {

            Class<?> thisClass = aclass;
            while (thisClass != null) {
                if (thisClass.getName().equals(className)) {
                    return true;
                }
                thisClass = thisClass.getSuperclass();
            }
        }else {

            Class<?> thisClass = aclass;
            while (thisClass != null) {
                for (Class<?> anInterface : thisClass.getInterfaces()) {
                    if (anInterface.getName().equals(className)) {
                        return true;
                    }
                }
                thisClass = thisClass.getSuperclass();
            }

        }
        return false;
    }

}