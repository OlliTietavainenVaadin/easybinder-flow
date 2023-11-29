package org.vaadin.easybinder.usagetest.com.vaadin.data;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.group.GroupSequenceProvider;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeanValidationBinderGroupingTest {

	public static interface MyGroup {
	}

	public static class MyEntityGroupProvider implements DefaultGroupSequenceProvider<MyEntity> {
		@Override
		public List<Class<?>> getValidationGroups(final MyEntity object) {
			final List<Class<?>> classes = new ArrayList<>();

			classes.add(MyEntity.class);

			if (object != null && object.s1 != null) {
				classes.add(MyGroup.class);
			}

			return classes;
		}
	}

	@GroupSequenceProvider(MyEntityGroupProvider.class)
	public static class MyEntity {
		String s1;
		@NotNull(groups = MyGroup.class)
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

		assertTrue(validator.validate(bean).isEmpty());

		bean.setS1("test");

		assertFalse(validator.validate(bean).isEmpty());

		assertFalse(validator.validateValue(MyEntity.class, "s2", null, MyGroup.class).isEmpty());

		binder.setBean(bean);

		// FIXME
		// assertFalse(binder.validate().isOk());
	}

}
