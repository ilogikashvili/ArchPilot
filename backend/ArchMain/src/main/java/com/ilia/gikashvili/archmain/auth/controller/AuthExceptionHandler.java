package com.ilia.gikashvili.archmain.auth.controller;
import org.springframework.web.bind.annotation.*; import org.springframework.core.annotation.Order; import org.springframework.http.*;
@RestControllerAdvice @Order(-10) public class AuthExceptionHandler {
 @ExceptionHandler({org.springframework.security.authentication.BadCredentialsException.class,io.jsonwebtoken.JwtException.class}) public ProblemDetail unauthorized(Exception e){return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e instanceof org.springframework.security.authentication.BadCredentialsException ? "Wrong email or password" : "Your session has expired. Please log in again.");}
 @ExceptionHandler({IllegalArgumentException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,org.springframework.http.converter.HttpMessageNotReadableException.class}) public ProblemDetail invalid(Exception e){return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Invalid request");}
}
