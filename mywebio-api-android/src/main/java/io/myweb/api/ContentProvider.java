package io.myweb.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.myweb.http.Method;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface ContentProvider {
	String value();
	Method[] methods() default { Method.GET, Method.POST, Method.PUT, Method.DELETE };
}
