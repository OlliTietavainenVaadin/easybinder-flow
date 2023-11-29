package org.vaadin.easybinder.usagetest.com.vaadin.data;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import jakarta.validation.*;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeanValidationBinderBeanLevelTest {

	@Target({ TYPE })
	@Retention(RUNTIME)
	@Constraint(validatedBy = MyValidator.class)
	public static @interface MyEntityValid {
		String message() default "At least one field must be set";

		Class<?>[] groups() default {};

		Class<? extends Payload>[] payload() default {};
	}

	public static class MyValidator implements ConstraintValidator<MyEntityValid, MyEntity> {

		@Override
		public void initialize(MyEntityValid constraintAnnotation) {
		}

		@Override
		public boolean isValid(MyEntity value, ConstraintValidatorContext context) {
			return value.s1 != null || value.s2 != null;
		}
	}

	@MyEntityValid
	public static class MyEntity {
		String s1;
		String s2;

		public String getS1() {
			return s1;
		}

		public void setS1(String s1) {
			this.s1 = s1;
		}

		public String getS2() {
			return s2;
		}

		public void setS2(String s2) {
			this.s2 = s2;
		}

	}

	ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
	Validator validator = factory.getValidator();

	@Test
	public void testBeanClassLevelValidation() {
		TextField field1 = new TextField();
		TextField field2 = new TextField();
		Span statusLabel = new Span();

		Binder<MyEntity> binder = new BeanValidationBinder<>(MyEntity.class);
		binder.forField(field1).withNullRepresentation("").bind("s1");
		binder.forField(field2).withNullRepresentation("").bind("s2");

		binder.setStatusLabel(statusLabel);

		MyEntity bean = new MyEntity();

		binder.setBean(bean);

		assertFalse(validator.validate(bean).isEmpty());
		assertTrue(binder.validate().isOk());

		// JSR303 bean level validation still missing:
		// https://github.com/vaadin/framework/issues/8498
		// assertFalse(binder.validate().isOk());
	}

}
