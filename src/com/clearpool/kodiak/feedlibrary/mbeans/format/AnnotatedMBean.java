package com.clearpool.kodiak.feedlibrary.mbeans.format;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.StandardMBean;

public class AnnotatedMBean<T> extends StandardMBean
{
	private static final Map<String, Class<?>> primitiveClasses = new HashMap<String, Class<?>>();

	public AnnotatedMBean(Class<T> mBeanInterfaceClass)
	{
		super(mBeanInterfaceClass, false);
	}

	@Override
	protected String getDescription(MBeanOperationInfo op)
	{
		String descr = op.getDescription();
		Method m = methodFor(getMBeanInterface(), op);
		if (m != null)
		{
			MBeanMethodDescription d = m.getAnnotation(MBeanMethodDescription.class);
			if (d != null) descr = d.value();
		}
		return descr;
	}

	@Override
	protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int paramNo)
	{
		String name = param.getName();
		Method m = methodFor(getMBeanInterface(), op);
		if (m != null)
		{
			ParameterDescription pname = getParameterAnnotation(m, paramNo, ParameterDescription.class);
			if (pname != null) name = pname.value();
		}
		return name;
	}

	static <A extends Annotation> A getParameterAnnotation(Method m, int paramNo, Class<A> annot)
	{
		for (Annotation a : m.getParameterAnnotations()[paramNo])
		{
			if (annot.isInstance(a)) return annot.cast(a);
		}
		return null;
	}

	private static Method methodFor(Class<?> mbeanInterface, MBeanOperationInfo op)
	{
		final MBeanParameterInfo[] params = op.getSignature();
		final String[] paramTypes = new String[params.length];
		for (int i = 0; i < params.length; i++)
			paramTypes[i] = params[i].getType();

		return findMethod(mbeanInterface, op.getName(), paramTypes);
	}

	private static Method findMethod(Class<?> mbeanInterface, String name, String... paramTypes)
	{
		try
		{
			final ClassLoader loader = mbeanInterface.getClassLoader();
			final Class<?>[] paramClasses = new Class<?>[paramTypes.length];
			for (int i = 0; i < paramTypes.length; i++)
				paramClasses[i] = classForName(paramTypes[i], loader);
			return mbeanInterface.getMethod(name, paramClasses);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	static Class<?> classForName(String name, ClassLoader loader) throws ClassNotFoundException
	{
		Class<?> c = primitiveClasses.get(name);
		if (c == null) c = Class.forName(name, false, loader);
		return c;
	}

	static
	{
		Class<?>[] prims = { byte.class, short.class, int.class, long.class, float.class, double.class, char.class, boolean.class, };
		for (Class<?> c : prims)
			primitiveClasses.put(c.getName(), c);
	}
}
