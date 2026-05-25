package com.auction.common.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Custom validation annotation that rejects values containing spaces. */
@Documented
@Constraint(validatedBy = NoSpaceValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoSpace {

  // Error message
  String message() default "Spaces are not allowed";

  // Required by Jakarta Validation spec
  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
