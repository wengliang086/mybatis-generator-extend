package com.my.util.mybatis.test.util;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Date;

public class Util {

    public static String uncapitalize(String s) {
        return StringUtils.uncapitalize(s);
    }

    public static String capitalize(String s) {
        return StringUtils.capitalize(s);
    }

    public static boolean isNumberType(Class clazz) {
        Class targtClass = clazz;
        if (clazz.isPrimitive()) {
            targtClass = ClassUtils.primitiveToWrapper(clazz);
        }
        return Number.class.isAssignableFrom(targtClass);
    }

    public static String getValue(Class clazz, String property) {
        if (clazz == Date.class) {
            return "new Date()";
        }

        if (clazz == Long.class || clazz == long.class) {
            return "1l";
        }

        if (clazz == Boolean.class || clazz == boolean.class) {
            return "true";
        }

        if (isNumberType(clazz)) {
            return "1";
        }


        return "\"" + uncapitalize(property) + "\"";
    }

    public static String getValueAndAppend12345(Class clazz, String property) {
        if (clazz == Date.class) {
            return "new Date()";
        }

        if (clazz == Long.class || clazz == long.class) {
            return "1l";
        }

        if (clazz == Boolean.class || clazz == boolean.class) {
            return "true";
        }

        if (isNumberType(clazz)) {
            return "1";
        }


        return "\"" + uncapitalize(property) + "12345\"";
    }

    public static boolean equals(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return false;
        }
        if (!(o1.getClass() == o2.getClass())) {
            return false;
        }
        if (o1.equals(o2)) {
            return true;
        }
        BeanInfo bi;
        try {
            bi = Introspector.getBeanInfo(o1.getClass(), Object.class);
        } catch (IntrospectionException e) {
            e.printStackTrace();
            return false;
        }
        PropertyDescriptor[] propertyDescriptors = bi.getPropertyDescriptors();
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            Method readMethod = propertyDescriptor.getReadMethod();
            Object value1 = null;
            Object value2 = null;
            try {
                value1 = readMethod.invoke(o1, null);
                value2 = readMethod.invoke(o2, null);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            if (value1 == null && value2 == null) {
                continue;
            }
            if (value1 instanceof Date) {
                if (((Date) value1).getTime() / 1000 - ((Date) value2).getTime() / 1000 <= 1) {
                    return true;
                }
            }
            if (value1.getClass().isArray()) {
                Object[] array1 = (Object[]) value1;
                Object[] array2 = (Object[]) value2;
                if (array1.length != array2.length) {
                    return false;
                }
                for (int i = 0; i < array1.length; i++) {
                    if (!array1[i].equals(array2[i])) {
                        return false;
                    }
                }
                return true;
            }

            if (!value1.equals(value2)) {
                return false;
            }
        }
        return true;
    }


    public static String getJavaProperty(String name) {
        String[] split = name.split("\\_");
        StringBuilder sb = new StringBuilder();
        sb.append(split[0]);
        for (int i = 1; i < split.length; i++) {
            sb.append(StringUtils.capitalize(split[i]));
        }
        return sb.toString();
    }


}
