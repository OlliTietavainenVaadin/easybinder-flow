package org.vaadin.easybinder.testentity;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.RetentionPolicy.*;
import static java.lang.annotation.ElementType.*;
@Target({ TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = FlightValidator.class)
public @interface FlightValid {
	public static String MESSAGE = "Please set SBT if EBT is set and set EBT if ABT is set";

	String message() default MESSAGE;

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}