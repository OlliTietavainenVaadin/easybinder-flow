package org.vaadin.easybinder.data;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.RequiredFieldConfigurator;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.Test;
import org.vaadin.easybinder.data.BasicBinder.EasyBinding;
import org.vaadin.easybinder.ui.EComboBox;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReflectionBinderTest {
	static enum TestEnum {
		Test1,
		Test2
	}

	static class TestEntity{
		@NotNull
		String testString;
		int testInt;
		Integer testInteger;
		Set<String> testSet;
		EnumSet<TestEnum> testEnumSet;
		@Min(value = 1)
		int testIntMin1;
		@Min(value = 0)
		int testIntMin0;

		public String getTestString() {
			return testString;
		}
		public void setTestString(String testString) {
			this.testString = testString;
		}
		public int getTestInt() {
			return testInt;
		}
		public void setTestInt(int testInt) {
			this.testInt = testInt;
		}
		public Integer getTestInteger() {
			return testInteger;
		}
		public void setTestInteger(Integer testInteger) {
			this.testInteger = testInteger;
		}
		public Set<String> getTestSet() {
			return testSet;
		}
		public void setTestSet(Set<String> testSet) {
			this.testSet = testSet;
		}
		public void setTestEnumSet(EnumSet<TestEnum> testEnumSet) {
			this.testEnumSet = testEnumSet;
		}
		public EnumSet<TestEnum> getTestEnumSet() {
			return testEnumSet;
		}
		public int getTestIntMin1() {
			return testIntMin1;
		}
		public void setTestIntMin1(int testIntMin1) {
			this.testIntMin1 = testIntMin1;
		}
		public int getTestIntMin0() {
			return testIntMin0;
		}
		public void setTestIntMin0(int testIntMin0) {
			this.testIntMin1 = testIntMin0;
		}
	}

	class TestEntityChild extends TestEntity {

	}

	TextField testString = new TextField();
	TextField testInt = new TextField();
	TextField testInteger = new TextField();

	ConverterRegistry converterRegistry = mock(ConverterRegistry.class);
	ReflectionBinder<TestEntity> binder = new ReflectionBinder<>(TestEntity.class, converterRegistry);

	@Test
	public void testStringConverterNullValue() {
		Converter<String,?> converter = binder.createToStringConverter();
		assertEquals("", converter.convertToPresentation(null, null));
	}

	@Test
	public void testStringConverterNonNullValue() {
		Converter<String,Object> converter = binder.createToStringConverter();
		assertEquals("test", converter.convertToPresentation(new Object() {
			@Override
			public String toString() {
				return "test";
			}
		}, null));
	}

	@Test
	public void testCastingConverterPrimitiveType() {
		Converter<Integer, Integer> intConverter = binder.createCastConverter(int.class);
		Result<Integer> res = intConverter.convertToModel(10, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(Integer.valueOf(10), e));
		assertEquals(Integer.valueOf(21), intConverter.convertToPresentation(Integer.valueOf(21), null));
	}

	@Test
	public void testCastingConverterNonPrimitiveType() {
		Converter<Integer, Integer> intConverter = binder.createCastConverter(Integer.class);
		Result<Integer> res = intConverter.convertToModel(Integer.valueOf(10), null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(Integer.valueOf(10), e));
		assertEquals(Integer.valueOf(21), intConverter.convertToPresentation(Integer.valueOf(21), null));
	}

	@Test
	public void testGetFieldTypeForAnonymousInstanceOfGenericField() {
		@SuppressWarnings("serial")
		RadioButtonGroup<TestEnum> r = new RadioButtonGroup<TestEnum>() {};
		assertTrue(binder.getPresentationTypeForField(r).isPresent());
		assertEquals(TestEnum.class, binder.getPresentationTypeForField(r).get());
	}

	@Test
	public void testGetFieldTypeForHasGenericType() {
		EComboBox<TestEnum> r = new EComboBox<>(TestEnum.class);
		assertTrue(binder.getPresentationTypeForField(r).isPresent());
		assertEquals(TestEnum.class, binder.getPresentationTypeForField(r).get());
	}

	@Test
	public void testGetFieldTypeForGenericFieldWithEmptyValue() {
		@SuppressWarnings("unchecked")
		HasValue<?, TestEnum> r = mock(HasValue.class);
		when(r.getEmptyValue()).thenReturn(TestEnum.Test1);
		assertTrue(binder.getPresentationTypeForField(r).isPresent());
		assertEquals(TestEnum.class, binder.getPresentationTypeForField(r).get());
	}

	@Test
	public void testGetFieldTypeForGenericFieldWithValue() {
		RadioButtonGroup<TestEnum> r = new RadioButtonGroup<TestEnum>();
		r.setValue(TestEnum.Test1);
		assertTrue(binder.getPresentationTypeForField(r).isPresent());
		assertEquals(TestEnum.class, binder.getPresentationTypeForField(r).get());
	}

	@Test
	public void testGetFieldTypeForGenericFieldWithItems() {
		EnumSet<TestEnum> set = EnumSet.allOf(TestEnum.class);
		RadioButtonGroup<TestEnum> r = new RadioButtonGroup<TestEnum>("", set);
		assertTrue(binder.getPresentationTypeForField(r).isPresent());
		assertEquals(TestEnum.class, binder.getPresentationTypeForField(r).get());
	}

	@Test
	public void testGetFieldTypeForGenericFieldWithNoItems() {
		EnumSet<TestEnum> set = EnumSet.noneOf(TestEnum.class);
		RadioButtonGroup<TestEnum> r = new RadioButtonGroup<TestEnum>("", set);
		// We don't expect any more than an empty optional
		assertNotNull(binder.getPresentationTypeForField(r));
	}

	@Test
	public void testGetFieldTypeForGenericFieldWithNoInfo() {
		RadioButtonGroup<TestEnum> r = new RadioButtonGroup<TestEnum>("");
		// We don't expect any more than an empty optional
		assertNotNull(binder.getPresentationTypeForField(r));
	}

	@Test
	public void testGetFieldTypeForFieldWithNoInfo() {
		@SuppressWarnings("unchecked")
		HasValue<?, TestEnum> r = mock(HasValue.class);
		// We don't expect any more than an empty optional
		assertNotNull(binder.getPresentationTypeForField(r));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBindNoSuchField() {
		binder.bind(mock(TextField.class), "noSuchField");
	}

	@Test
	public void testBindNoConverterIdentity() {
		when(converterRegistry.getConverter(String.class, String.class)).thenReturn(null);
		EasyBinding<TestEntity, String, String> binding = binder.bind(new TextField(), "testString");
		assertNotNull(binding);
	}

	@Test
	public void testBindNoConverterUnrelatedStringPresentation() {
		when(converterRegistry.getConverter(String.class, Integer.class)).thenReturn(null);
		EasyBinding<TestEntity, String, Integer> binding = binder.bind(new TextField(), "testInt");
		assertNotNull(binding);
	}


	@Test
	public void testBindTypeErasure() {
		when(converterRegistry.getConverter(String.class, String.class)).thenReturn(null);
		EasyBinding<TestEntity, String, String> binding = binder.bind(new RadioButtonGroup<String>(), "testString");
		assertNotNull(binding);
		verify(converterRegistry, never()).getConverter(any(), any());

		Result<String> res = binding.converterValidatorChain.convertToModel("giraf", null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals("giraf", e));
		assertEquals("bird", binding.converterValidatorChain.convertToPresentation("bird", null));
	}

	@Test(expected = RuntimeException.class)
	public void testBindTypeErasureUnrelatedTypes() {
		when(converterRegistry.getConverter(Double.class, Integer.class)).thenReturn(null);
		EasyBinding<TestEntity, Double, Integer> binding = binder.bind(new RadioButtonGroup<Double>(), "testInt");
		assertNotNull(binding);
		verify(converterRegistry, never()).getConverter(any(), any());

		binding.converterValidatorChain.convertToModel(Double.valueOf(10.0), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBindUnknownProperty() {
		binder.bind(new TextField(), "noSuchField");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBindWithConverterUnknownProperty() {
		@SuppressWarnings("unchecked")
		Converter<String, Integer> c = mock(Converter.class);
		binder.bind(new TextField(), "noSuchField", c);
	}

	@Test
	public void testCreateConverterPrimitiveToPrimitive() {
		int emptyValue = 0;

		when(converterRegistry.getConverter(int.class, int.class)).thenReturn(null);
		Converter<Integer, Integer> converter = binder.createConverter(int.class, int.class, emptyValue);
		assertNotNull(converter);
		Result<Integer> res = converter.convertToModel(10, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(Integer.valueOf(10), e));
		assertEquals(Integer.valueOf(21), converter.convertToPresentation(21, null));

		res = converter.convertToModel(emptyValue, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(Integer.valueOf(emptyValue), e));
		assertEquals(Integer.valueOf(emptyValue), converter.convertToPresentation(emptyValue, null));
	}

	@Test
	public void testCreateConverterPrimitiveToNonPrimitive() {
		int emptyValue = 0;

		when(converterRegistry.getConverter(int.class, Integer.class)).thenReturn(null);
		Converter<Integer, Integer> converter = binder.createConverter(int.class, Integer.class, emptyValue);
		assertNotNull(converter);
		Result<Integer> res = converter.convertToModel(Integer.valueOf(10), null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(Integer.valueOf(10), e));
		assertEquals(Integer.valueOf(21), converter.convertToPresentation(Integer.valueOf(21), null));

		res = converter.convertToModel(emptyValue, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(null, e));
		assertEquals(Integer.valueOf(emptyValue), converter.convertToPresentation(null, null));
	}

	@Test
	public void testCreateConverterNonPrimitiveToPrimitive() {
		Integer emptyValue = null;

		when(converterRegistry.getConverter(Integer.class, int.class)).thenReturn(null);
		Converter<Integer, Integer> converter = binder.createConverter(Integer.class, int.class, emptyValue);
		assertNotNull(converter);
		Result<Integer> res = converter.convertToModel(Integer.valueOf(10), null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(Integer.valueOf(10), e));
		assertEquals(Integer.valueOf(21), converter.convertToPresentation(Integer.valueOf(21), null));

		res = converter.convertToModel(emptyValue, null);
		// Should fail
		assertTrue(res.isError());
	}

	@Test
	public void testCreateConverterNonPrimitiveToNonPrimitive() {
		Integer emptyValue = null;

		when(converterRegistry.getConverter(Integer.class, Integer.class)).thenReturn(null);
		Converter<Integer, Integer> converter = binder.createConverter(Integer.class, Integer.class, emptyValue);
		assertNotNull(converter);
		Result<Integer> res = converter.convertToModel(Integer.valueOf(10), null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(Integer.valueOf(10), e));
		assertEquals(Integer.valueOf(21), converter.convertToPresentation(Integer.valueOf(21), null));

		res = converter.convertToModel(emptyValue, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(null, e));

		assertEquals(emptyValue, converter.convertToPresentation(null, null));
	}

	@Test
	public void testCreateConverterNonPrimitiveToNonPrimitiveEmptyValue() {
		Integer emptyValue = Integer.valueOf(0);

		when(converterRegistry.getConverter(Integer.class, Integer.class)).thenReturn(null);
		Converter<Integer, Integer> converter = binder.createConverter(Integer.class, Integer.class, emptyValue);
		assertNotNull(converter);
		Result<Integer> res = converter.convertToModel(Integer.valueOf(10), null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(Integer.valueOf(10), e));
		assertEquals(Integer.valueOf(21), converter.convertToPresentation(Integer.valueOf(21), null));

		res = converter.convertToModel(emptyValue, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(null, e));

		assertEquals(emptyValue, converter.convertToPresentation(null, null));
	}

	@Test
	public void testCreateConverterStringToString() {
		String emptyValue = null;

		when(converterRegistry.getConverter(String.class, String.class)).thenReturn(null);
		Converter<String, String> converter = binder.createConverter(String.class, String.class, emptyValue);
		assertNotNull(converter);
		Result<String> res = converter.convertToModel("abc", null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals("abc", e));
		assertEquals("def", converter.convertToPresentation("def", null));

		res = converter.convertToModel(emptyValue, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(null, e));

		assertEquals(emptyValue, converter.convertToPresentation(null, null));
	}

	@Test
	public void testCreateConverterStringToStringEmpty() {
		String emptyValue = "";

		when(converterRegistry.getConverter(String.class, String.class)).thenReturn(null);
		Converter<String, String> converter = binder.createConverter(String.class, String.class, emptyValue);
		assertNotNull(converter);
		Result<String> res = converter.convertToModel("abc", null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals("abc", e));
		assertEquals("def", converter.convertToPresentation("def", null));

		res = converter.convertToModel(emptyValue, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(null, e));

		assertEquals(emptyValue, converter.convertToPresentation(null, null));
	}

	@Test
	public void testCreateConverterBooleanToboolean() {
		Boolean emptyValue = false;

		when(converterRegistry.getConverter(Boolean.class, boolean.class)).thenReturn(null);
		Converter<Boolean, Boolean> converter = binder.createConverter(Boolean.class, boolean.class, emptyValue);
		assertNotNull(converter);
		Result<Boolean> res = converter.convertToModel(Boolean.FALSE, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(false, e));
		res = converter.convertToModel(Boolean.TRUE, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(true, e));

		assertEquals(false, converter.convertToPresentation(false, null));
		assertEquals(true, converter.convertToPresentation(true, null));
	}

	@Test
	public void testCreateConverterEnumToEnum() {
		TestEnum emptyValue = null;

		when(converterRegistry.getConverter(TestEnum.class, TestEnum.class)).thenReturn(null);
		Converter<TestEnum, TestEnum> converter = binder.createConverter(TestEnum.class, TestEnum.class, emptyValue);
		assertNotNull(converter);
		Result<TestEnum> res = converter.convertToModel(TestEnum.Test1, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(TestEnum.Test1, e));
		assertEquals(TestEnum.Test2, converter.convertToPresentation(TestEnum.Test2, null));

		res = converter.convertToModel(emptyValue, null);
		assertFalse(res.isError());
		res.ifOk(e -> assertEquals(null, e));

		assertEquals(emptyValue, converter.convertToPresentation(null, null));
	}

	@Test
	public void testGetGenericType() {
		assertEquals(TestEntity.class, binder.getGenericType());
	}

	@Test
	public void testGetDeclaredFieldByName() {
		Optional<Field> field = binder.getDeclaredFieldByName(TestEntityChild.class, "testInt");
		assertTrue(field.isPresent());
	}

	@Test
	public void testGetDeclaredFieldByNameInvalidName() {
		Optional<Field> field = binder.getDeclaredFieldByName(TestEntityChild.class, "testGiraf");
		assertFalse(field.isPresent());
	}

	@Test
	public void testRequiredIndicatorVisible() {
		AbstractField<?, String> field = mock(TextField.class);
		EasyBinding<?,?,?> binding = binder.bind(field, "testIntMin1", new StringToIntegerConverter(""));
		assertNotNull(binding);
		verify(field, times(1)).setRequiredIndicatorVisible(true);
	}

	@Test
	public void testRequiredIndicatorNotVisible() {
		AbstractField<?, String> field = mock(TextField.class);
		EasyBinding<?,?,?> binding = binder.bind(field, "testIntMin0", new StringToIntegerConverter(""));
		assertNotNull(binding);
		verify(field, never()).setRequiredIndicatorVisible(true);
	}

	@Test
	public void testRequiredIndicatorNotVisibleNoAnnotation() {
		AbstractField<?, String> field = mock(TextField.class);
		EasyBinding<?,?,?> binding = binder.bind(field, "testInt", new StringToIntegerConverter(""));
		assertNotNull(binding);
		verify(field, never()).setRequiredIndicatorVisible(true);
	}

	@Test
	public void testRequiredIndicatorVisibleCustomIndicator() {
		AbstractField<?, String> field = mock(TextField.class);
		binder.setRequiredConfigurator((RequiredFieldConfigurator) (annotation, bindingBuilder) -> true);
		assertNotNull(binder.getRequiredConfigurator());
		EasyBinding<?,?,?> binding = binder.bind(field, "testIntMin0", new StringToIntegerConverter(""));
		assertNotNull(binding);
		verify(field, times(1)).setRequiredIndicatorVisible(true);
	}

}
