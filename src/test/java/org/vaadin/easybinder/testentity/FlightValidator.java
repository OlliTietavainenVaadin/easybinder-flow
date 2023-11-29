package org.vaadin.easybinder.testentity;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FlightValidator implements ConstraintValidator<FlightValid, Flight> {

	@Override
	public void initialize(FlightValid constraintAnnotation) {
	}

	@Override
	public boolean isValid(Flight flight, ConstraintValidatorContext context) {
		return (flight.getAbt() == null || flight.getEbt() != null)
				&& (flight.getEbt() == null || flight.getSbt() != null);
	}
}
