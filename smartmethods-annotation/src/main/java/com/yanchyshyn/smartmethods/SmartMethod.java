package com.yanchyshyn.smartmethods;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface SmartMethod {
	boolean oneShot() default false;
	boolean threadSafe() default false;
	boolean enabled() default true;
	boolean debug() default false;
	String customClassName() default "";
	String customPackageName() default "";
}