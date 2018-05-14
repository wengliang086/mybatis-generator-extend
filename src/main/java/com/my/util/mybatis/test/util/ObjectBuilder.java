package com.my.util.mybatis.test.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

public class ObjectBuilder {

	public static <T> T build(Class<T> clazz) {
		return build(clazz, null);

	}

	public static <T> T build(Class<T> clazz, String... excludeProperties) {
		try {
			T newInstance = clazz.newInstance();
			BeanInfo bi = null;
			try {
				bi = Introspector.getBeanInfo(clazz, Object.class);
			} catch (IntrospectionException e) {
				e.printStackTrace();
			}
			PropertyDescriptor[] propertyDescriptors = bi.getPropertyDescriptors();
			for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
				if (excludeProperties != null && contains(excludeProperties, propertyDescriptor.getName())) {
					continue;
				}
				Method writeMethod = propertyDescriptor.getWriteMethod();
				if (writeMethod != null) {
					Object arg0 = getValue(propertyDescriptor.getPropertyType(), propertyDescriptor.getName());

					if (arg0 != null && writeMethod.getParameterTypes().length == 1) {
						writeMethod.invoke(newInstance, new Object[] { arg0 });
					}
				}
			}
			return newInstance;
		} catch (InstantiationException e) {
			//            throw HExceptionBuilder.newBuilder(e).setReturnCode(HUtilReturnCode.NEW_INSTANCE_FAIELD).addAttribute("class", clazz.getName()).build();
		} catch (IllegalAccessException e) {
			//            throw HExceptionBuilder.newBuilder(e).setReturnCode(HUtilReturnCode.NEW_INSTANCE_FAIELD).addAttribute("class", clazz.getName()).build();
		} catch (IllegalArgumentException e) {
			//            throw HExceptionBuilder.newBuilder(e).setReturnCode(HUtilReturnCode.NEW_INSTANCE_FAIELD).addAttribute("class", clazz.getName()).build();
		} catch (InvocationTargetException e) {
			//            throw HExceptionBuilder.newBuilder(e).setReturnCode(HUtilReturnCode.NEW_INSTANCE_FAIELD).addAttribute("class", clazz.getName()).build();
		}
		return null;
	}

	private static boolean contains(String[] excludeProperties, String name) {
		for (String property : excludeProperties) {
			if (property.equals(name)) {
				return true;
			}
		}
		return false;
	}

	public static Object getValue(Class clazz, String property) {
		if (clazz == Date.class) {
			return new Date(System.currentTimeMillis());
		}

		if (clazz == Long.class || clazz == long.class) {
			return 1L;
		}

		if (clazz == Double.class || clazz == double.class) {
			return 1.0D;
		}

		if (clazz == Float.class || clazz == float.class) {
			return 1.0F;
		}

		if (clazz == Boolean.class || clazz == boolean.class) {
			return true;
		}

		if (isNumberType(clazz)) {
			return 1;
		}

		if (String.class == clazz) {
			return StringUtils.uncapitalize(property);
		}
		return null;
	}

	private static boolean isNumberType(Class clazz) {
		Class targtClass = clazz;
		if (clazz.isPrimitive()) {
			targtClass = ClassUtils.primitiveToWrapper(clazz);
		}
		return Number.class.isAssignableFrom(targtClass);
	}

}
