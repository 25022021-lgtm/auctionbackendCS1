package com.auction.common.annotations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validator for the {@link NoSpace} annotation. Rejects strings containing whitespace. */
public class NoSpaceValidator implements ConstraintValidator<NoSpace, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // Null value considered separately
    if (value == null) {
      return true;
    }

    // Returns false if string contains whitespace
    return !value.contains(" ");
  }
}
