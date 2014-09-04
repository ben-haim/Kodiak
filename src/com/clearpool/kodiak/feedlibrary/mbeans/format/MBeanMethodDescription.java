package com.clearpool.kodiak.feedlibrary.mbeans.format;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MBeanMethodDescription
{
	String value();
}
