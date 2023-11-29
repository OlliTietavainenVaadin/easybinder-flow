package org.vaadin.easybinder.data;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.StatusChangeListener;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import org.junit.Test;
import org.vaadin.easybinder.data.BasicBinder.EasyBinding;
import org.vaadin.easybinder.testentity.Flight;
import org.vaadin.easybinder.testentity.FlightId;
import org.vaadin.easybinder.testentity.FlightId.LegType;

import java.util.Arrays;
import java.util.Date;

import static info.solidsoft.mockito.java8.AssertionMatcher.assertArg;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BinderAdapterTest {

	static class TestEntity {
		int test = 0;

		int getTest() {
			return test;
		}
	}


	@Test
	public void testReadWriteBean() throws ValidationException {
		AutoBinder<Flight> binder = new AutoBinder<>(Flight.class);
		BinderAdapter<Flight> adapter = new BinderAdapter<>(binder, Flight.class);


		binder.buildAndBind("flightId");

		Flight f1 = new Flight();
		FlightId id1 = new FlightId();
		id1.setDate(new Date());
		id1.setAirline("XX");
		id1.setFlightNumber(999);
		id1.setLegType(LegType.DEPARTURE);
		f1.setFlightId(id1);

		Flight f2 = new Flight();

		adapter.readBean(f1);

		assertTrue(binder.isValid());

		StatusChangeListener statusChangeListener = mock(StatusChangeListener.class);
		adapter.addStatusChangeListener(statusChangeListener);

		adapter.writeBean(f2);

		verify(statusChangeListener, atLeast(1)).statusChange(assertArg(sc -> assertFalse(sc.hasValidationErrors())));

		assertTrue(f1 != f2);
		assertEquals(f1.getFlightId().getAirline(), f2.getFlightId().getAirline());

		adapter.readBean(f1);
		TextField field = (TextField)binder.getFieldForProperty("flightId.flightNumber").get();
		field.setValue("-1");

		assertFalse(binder.isValid());

		try {
			adapter.writeBean(f2);
			assertTrue(false);
		} catch(ValidationException ex) {
			assertEquals(0, ex.getBeanValidationErrors().size());
			assertEquals(1, ex.getFieldValidationErrors().size());
		}
	}

	@Test
	public void testReadWriteBeanEmptySetter() throws ValidationException {
		@SuppressWarnings("unchecked")
		BasicBinder<TestEntity> binder = mock(BasicBinder.class);
		BinderAdapter<TestEntity> binderAdapter = new BinderAdapter<TestEntity>(binder, TestEntity.class);

		when(binder.getBean()).thenReturn(new TestEntity());
		when(binder.isValid()).thenReturn(true);

		EasyBinding<TestEntity, String, Integer> binding = new EasyBinding<>(
				binder,
				new TextField(),
				TestEntity::getTest,
				null,
				"test",
				new StringToIntegerConverter(""));

		when(binder.getBindings()).thenReturn(Arrays.asList(binding));

		TestEntity testEntity = new TestEntity();

		// Just verify that no null pointer exception is thrown
		binderAdapter.readBean(testEntity);
		binderAdapter.writeBean(testEntity);
	}


}
