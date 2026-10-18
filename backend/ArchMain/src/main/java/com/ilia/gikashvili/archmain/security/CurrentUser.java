package com.ilia.gikashvili.archmain.security;

import java.lang.annotation.*;

/**
 * Annotation to inject the currently authenticated user into a controller method parameter.
 * <p>
 * Usage: {@code @CurrentUser AuthenticatedUser user}
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
