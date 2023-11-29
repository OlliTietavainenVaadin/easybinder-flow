package org.vaadin.easybinder.usagetest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.PropertyId;
import org.junit.Ignore;
import org.junit.Test;
import org.vaadin.easybinder.testentity.Flight;
import org.vaadin.easybinder.testentity.FlightId.LegType;
public abstract class BaseTests {

	static class MyForm {
		@PropertyId("flightId.airline")
		TextField airline = new TextField("Airline");
		@PropertyId("flightId.flightNumber")
		TextField flightNumber = new TextField("Flight number");
		@PropertyId("flightId.flightSuffix")
		TextField flightSuffix = new TextField("Flight suffix");
		@PropertyId("flightId.date")
		DatePicker date = new DatePicker("Date");
		@PropertyId("flightId.legType")
		ComboBox<LegType> legType = new ComboBox<>("Leg type", EnumSet.allOf(LegType.class));
		DateTimePicker sbt = new DateTimePicker("SBT");
		DateTimePicker ebt = new DateTimePicker("EBT");
		DateTimePicker abt = new DateTimePicker("ABT");
		TextField gate = new TextField("Gate");
		Checkbox canceled = new Checkbox("Canceled");
	}

	protected abstract void setBean(Flight flight);

	protected abstract Stream<HasValue<?, ?>> getFields();

	protected abstract boolean isValid();
	
	protected abstract void setStatusLabel(HasText label);

	static MyForm form = new MyForm();
	
	@Test
	@Ignore
	public void testBinding() {
		setBean(new Flight());
		assertEquals(10, getFields().collect(Collectors.toList()).size());
	}

	@Test
	public void testStringConversion() {
		Flight f = new Flight();
		setBean(f);

		// String->String model->presentation
		assertNull(f.getFlightId().getAirline());
		assertEquals("", form.airline.getValue());
		f.getFlightId().setAirline("BB");
		setBean(f);
		assertEquals("BB", form.airline.getValue());
		f.getFlightId().setAirline(null);
		setBean(f);
		assertEquals("", form.airline.getValue());

		// String->String presentation->model
		form.airline.setValue("AA");
		assertEquals("AA", f.getFlightId().getAirline());
		form.airline.setValue("");
		assertNull(f.getFlightId().getAirline());
		form.airline.setValue("CC");
		assertEquals("CC", f.getFlightId().getAirline());
	}

	@Test
	public void testStringConversion2() {
		Flight f = new Flight();
		setBean(f);

		// String->String model->presentation
		assertNull(f.getGate());
		assertEquals("", form.gate.getValue());
		f.setGate("G17");
		setBean(f);
		assertEquals("G17", form.gate.getValue());
		f.setGate(null);
		setBean(f);
		assertEquals("", form.gate.getValue());

		// String->String presentation->model
		form.gate.setValue("A19");
		assertEquals("A19", f.getGate());
		form.gate.setValue("");
		assertNull(f.getGate());
		form.gate.setValue("B2");
		assertEquals("B2", f.getGate());
	}

	@Test
	@Ignore
	public void testIntConversion() {
		Flight f = new Flight();
		setBean(f);

		// int->String model->presentation
		assertEquals(0, f.getFlightId().getFlightNumber());
		// TODO: Fix-me
		// assertEquals("", form.flightNumber.getValue());
		f.getFlightId().setFlightNumber(100);
		setBean(f);
		assertEquals("100", form.flightNumber.getValue());
		f.getFlightId().setFlightNumber(0);
		setBean(f);
		// TODO: Fix-me
		// assertEquals("", form.flightNumber.getValue());

		// String->int presentation->model
		form.flightNumber.setValue("99");
		assertEquals(99, f.getFlightId().getFlightNumber());
		assertNull(form.flightNumber.getErrorMessage());
		// Conversion fails - data is not written
		form.flightNumber.setValue("");
		assertNotNull(form.flightNumber.getErrorMessage());
		assertEquals(99, f.getFlightId().getFlightNumber());
		form.flightNumber.setValue("98");
		assertNull(form.flightNumber.getErrorMessage());
		assertEquals(98, f.getFlightId().getFlightNumber());
		form.flightNumber.setValue("XYZ");
		assertNotNull(form.flightNumber.getErrorMessage());
		assertEquals(98, f.getFlightId().getFlightNumber());
	}

	@Test
	public void testBoolConversion() {
		Flight f = new Flight();
		setBean(f);

		// boolean->String model->presentation
		assertEquals(false, f.isCanceled());
		assertEquals(false, form.canceled.getValue());
		f.setCanceled(true);
		setBean(f);
		assertEquals(true, form.canceled.getValue());
		f.setCanceled(false);
		setBean(f);

		// String->bool presentation->model
		form.canceled.setValue(true);
		assertEquals(true, f.isCanceled());
		form.canceled.setValue(false);
		assertEquals(false, f.isCanceled());
	}	
	
	
	@Test
	@Ignore
	public void testCharacterConversion() {
		Flight f = new Flight();
		setBean(f);

		// String->Character model->presentation
		assertEquals(null, f.getFlightId().getFlightSuffix());
		assertEquals("", form.flightSuffix.getValue());
		f.getFlightId().setFlightSuffix('C');
		setBean(f);
		assertEquals("C", form.flightSuffix.getValue());
		f.getFlightId().setFlightSuffix(null);
		setBean(f);
		assertEquals("", form.flightSuffix.getValue());

		// Character->String presentation->model
		form.flightSuffix.setValue("D");
		assertEquals(Character.valueOf('D'), f.getFlightId().getFlightSuffix());
		form.flightSuffix.setValue("");
		assertNull(f.getFlightId().getFlightSuffix());
		// Conversion fails - data is not written
		form.flightSuffix.setValue("D");
		assertNull(form.flightSuffix.getErrorMessage());
		form.flightSuffix.setValue("KK");
		assertNotNull(form.flightSuffix.getErrorMessage());
		assertEquals(Character.valueOf('D'), f.getFlightId().getFlightSuffix());
		form.flightSuffix.setValue("E");
		assertNull(form.flightSuffix.getErrorMessage());
		assertEquals(Character.valueOf('E'), f.getFlightId().getFlightSuffix());

		// TODO: Write tests for other conversion fields
	}

	@Test
	public void testFieldValidation() {
		setBean(new Flight());

		// Check binding validation
		assertFalse(isValid());

		form.airline.setValue("SK");
		form.flightNumber.setValue("100");
		form.flightSuffix.setValue("S");
		form.date.setValue(LocalDate.now());
		form.legType.setValue(LegType.ARRIVAL);
		LocalDateTime now = LocalDateTime.now();
		form.sbt.setValue(now);
		form.ebt.setValue(now);
		form.abt.setValue(now);
		form.gate.setValue("A16");

		// Check binding validation
		assertTrue(isValid());
	}

	@Test
	public void testBeanLevelValidation() {
		setBean(new Flight());
		form.airline.setValue("SK");
		form.flightNumber.setValue("100");
		form.flightSuffix.setValue("S");
		form.date.setValue(LocalDate.now());
		form.legType.setValue(LegType.ARRIVAL);
		LocalDateTime now = LocalDateTime.now();
		form.sbt.setValue(now);
		form.ebt.setValue(now);
		form.abt.setValue(now);
		form.gate.setValue("A16");

		Span label = new Span();
		setStatusLabel(label);
				
		assertTrue(isValid());
		assertEquals("", label.getText());

		form.sbt.setValue(null);
		assertNull(form.sbt.getValue());
		assertFalse(isValid());
		//Below assert holds for Vaadin < 8.4. Seems that null conversion has been changed slightly in 8.4
		//assertNotEquals("", label.getValue());
		
		form.sbt.setValue(now);
		assertTrue(isValid());
		assertEquals("", label.getText());
	}

	@Test
	public void testBeanGroupValidation() {
		setBean(new Flight());
		form.airline.setValue("SK");
		form.flightNumber.setValue("100");
		form.flightSuffix.setValue("S");
		form.date.setValue(LocalDate.now());
		form.legType.setValue(LegType.ARRIVAL);
		LocalDateTime now = LocalDateTime.now();
		form.sbt.setValue(now);
		form.ebt.setValue(now);
		form.abt.setValue(now);
		form.gate.setValue("A16");

		assertTrue(isValid());

		form.gate.setValue("");

		assertFalse(isValid());
		form.ebt.setValue(null);
		form.abt.setValue(null);		
		form.sbt.setValue(null);
		assertTrue(isValid());

	}

	@Test
	@Ignore
	public void testRequiredIndicator() {
		assertTrue(form.airline.isRequiredIndicatorVisible());
		assertTrue(form.flightNumber.isRequiredIndicatorVisible());
		assertFalse(form.flightSuffix.isRequiredIndicatorVisible());
		assertTrue(form.date.isRequiredIndicatorVisible());
		assertTrue(form.legType.isRequiredIndicatorVisible());
		assertFalse(form.sbt.isRequiredIndicatorVisible());
		assertFalse(form.ebt.isRequiredIndicatorVisible());
		assertFalse(form.abt.isRequiredIndicatorVisible());
		// TODO: Fix this - Required indicator should consider groups
		// assertFalse(form.gate.isRequiredIndicatorVisible());
	}
}
