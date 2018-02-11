package com.fengcone.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对象转化工具类
 * 
 * @author fengcone
 *
 */
public class BeanConvertUtil {
	private static Map<String, Map<String, Method>> methodMap;

	private static BeanConvertUtil beanConvertUtil = null;

	private BeanConvertUtil() {

	}

	public static BeanConvertUtil newInstance() {
		if (beanConvertUtil == null) {
			synchronized (BeanConvertUtil.class) {
				if (beanConvertUtil == null) {
					beanConvertUtil = new BeanConvertUtil();
				}
			}
		}
		return beanConvertUtil;
	}

	public <T> List<T> convertList(Collection<? extends Object> sourceList, Class<T> targetClass) {
		List<T> targetList = new ArrayList<T>();
		if (sourceList == null) {
			return targetList;
		}
		for (Object object : sourceList) {
			targetList.add(copyProperties(object, targetClass));
		}
		return targetList;
	}

	public <T> T copyProperties(Object object, Class<T> targetClass) {
		try {
			if (object == null || targetClass == null) {
				return null;
			}
			T t = targetClass.newInstance();
			Map<String, Map<String, Method>> cacheMap = getCacheMap();
			Map<String, Method> targetMap = cacheMap.get(targetClass.getCanonicalName() + "_set");
			if (targetMap == null) {
				targetMap = initSetMethodMap(targetClass);
				cacheMap.put(targetClass.getCanonicalName() + "_set", targetMap);
			}
			Map<String, Method> sourceMap = cacheMap.get(object.getClass().getCanonicalName() + "_get");
			if (sourceMap == null) {
				sourceMap = initGetMethodMap(object.getClass());
				cacheMap.put(object.getClass().getCanonicalName() + "_get", sourceMap);
			}
			for (Entry<String, Method> entry : targetMap.entrySet()) {
				String key = entry.getKey();
				Method sourceMethod = sourceMap.get(key);
				if (sourceMethod == null) {
					continue;
				}
				Class<?> returnType = sourceMethod.getReturnType();
				Class<?> paramType = entry.getValue().getParameterTypes()[0];
				if (compareClass(returnType, paramType)) {
					entry.getValue().invoke(t, sourceMethod.invoke(object, new Object[0]));
					continue;
				}
				if (returnType.equals(String.class)) {
					String sourceObject = (String) sourceMethod.invoke(object, new Object[0]);
					if (sourceObject == null) {
						continue;
					}
					Object targetObject = convertString(sourceObject, paramType);
					if (targetObject != null) {
						entry.getValue().invoke(t, targetObject);
					}
					continue;
				}
				if (paramType.equals(String.class)) {
					Object targetObject = sourceMethod.invoke(object, new Object[0]);
					if (targetObject == null) {
						continue;
					}
					if (targetObject instanceof Date) {
						targetObject = SimpleDateFormat.getDateTimeInstance().format((Date) targetObject);
					}
					entry.getValue().invoke(t, String.valueOf(targetObject));
				}
			}
			return t;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private Object convertString(String source, Class<?> targetClass) {
		if (targetClass.equals(Integer.class) || targetClass.equals(int.class)) {
			return Integer.valueOf(source);
		}
		if (targetClass.equals(Long.class) || targetClass.equals(long.class)) {
			return Long.valueOf(source);
		}
		if (targetClass.equals(Double.class) || targetClass.equals(double.class)) {
			return Double.valueOf(source);
		}
		if (targetClass.equals(Float.class) || targetClass.equals(float.class)) {
			return Float.valueOf(source);
		}
		if (targetClass.equals(Boolean.class) || targetClass.equals(boolean.class)) {
			return Boolean.valueOf(source);
		}
		if (targetClass.equals(Byte.class) || targetClass.equals(byte.class)) {
			return Byte.valueOf(source);
		}
		if (targetClass.equals(Short.class) || targetClass.equals(short.class)) {
			return Short.valueOf(source);
		}
		if (targetClass.equals(BigDecimal.class)) {
			return new BigDecimal(source);
		}
		return null;
	}

	private boolean compareClass(Class<?> source, Class<?> target) {
		// Boolean, Byte, Short, Integer, Long, Float, Double, Character
		if (source == target) {
			return true;
		}
		if (source.equals(int.class) && target.equals(Integer.class)) {
			return true;
		} else if (source.equals(boolean.class) && target.equals(Boolean.class)) {
			return true;
		} else if (source.equals(double.class) && target.equals(Double.class)) {
			return true;
		} else if (source.equals(float.class) && target.equals(Float.class)) {
			return true;
		} else if (source.equals(byte.class) && target.equals(Byte.class)) {
			return true;
		} else if (source.equals(short.class) && target.equals(Short.class)) {
			return true;
		} else if (source.equals(char.class) && target.equals(Character.class)) {
			return true;
		} else if (source.equals(long.class) && target.equals(Long.class)) {
			return true;
		}
		return false;
	}

	private <T> Map<String, Method> initSetMethodMap(Class<T> targetClass) {
		Map<String, Method> resultMap = new HashMap<>();
		Method[] methods = targetClass.getMethods();
		for (Method method : methods) {
			if (method.getName().startsWith("set")) {
				resultMap.put(method.getName().substring(3), method);
			}
		}
		return resultMap;
	}

	private <T> Map<String, Method> initGetMethodMap(Class<T> targetClass) {
		Map<String, Method> resultMap = new HashMap<>();
		Method[] methods = targetClass.getMethods();
		for (Method method : methods) {
			if (method.getName().startsWith("get")) {
				resultMap.put(method.getName().substring(3), method);
			}
		}
		return resultMap;
	}

	private Map<String, Map<String, Method>> getCacheMap() {
		if (methodMap == null) {
			synchronized (BeanConvertUtil.class) {
				if (methodMap == null) {
					methodMap = new ConcurrentHashMap<String, Map<String, Method>>();
				}
			}
		}
		return methodMap;
	}
}
