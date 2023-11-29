package org.vaadin.easybinder.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.RequiredFieldConfigurator;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.LocalDateTimeToDateConverter;
import com.vaadin.flow.data.converter.LocalDateToDateConverter;
import org.vaadin.addonhelpers.AbstractTest;
import org.vaadin.easybinder.testentity.Flight;
import org.vaadin.easybinder.testentity.FlightId.LegType;
import org.vaadin.easybinder.testentity.FlightValid;
import org.vaadin.easybinder.testentity.FlightValidator;

import javax.validation.constraints.Min;
import java.time.ZoneId;
import java.util.EnumSet;

public class VaadinBeanBinderExample extends AbstractTest {
	private static final long serialVersionUID = 1L;

	TextField airline = new TextField("Airline");
	TextField flightNumber = new TextField("Flight number");
	TextField flightSuffix = new TextField("Flight suffix");
	DatePicker date = new DatePicker("Date");
	RadioButtonGroup<LegType> legType = new RadioButtonGroup<>("Leg type", EnumSet.allOf(LegType.class));
	DateTimePicker sbt = new DateTimePicker("SBT");
	DateTimePicker ebt = new DateTimePicker("EBT");
	DateTimePicker abt = new DateTimePicker("ABT");
	TextField gate = new TextField("Gate");
	Checkbox canceled = new Checkbox("Canceled");

	@Override
	public Component getTestComponent() {
		BeanValidationBinder<Flight> binder = new BeanValidationBinder<>(Flight.class);

		RequiredFieldConfigurator MIN = (annotation, ignore) -> annotation.annotationType().equals(Min.class)
				&& ((Min) annotation).value() > 0;

		binder.setRequiredConfigurator(MIN.chain(RequiredFieldConfigurator.DEFAULT));

		Converter<String, Integer> c = Converter.from(e -> {
			if (e.length() == 0) {
				return Result.error("Must be a number");
			}
			try {
				return Result.ok(Integer.parseInt(e));
			} catch (NumberFormatException ex) {
				return Result.error("Must be a number");
			}
		}, e -> Integer.toString(e));

		binder.forField(airline).bind("flightId.airline");
		binder.forField(flightNumber).withConverter(c).bind("flightId.flightNumber");
		binder.forField(flightSuffix)
				.withConverter(Converter.from(
						e -> e.length() == 0 ? Result.ok(null)
								: (e.length() == 1 ? Result.ok(e.charAt(0)) : Result.error("Must be 1 character")),
						f -> f == null ? "" : "" + f))
				.bind("flightId.flightSuffix");
		binder.forField(date).withConverter(new LocalDateToDateConverter()).bind("flightId.date");
		binder.forField(legType).bind("flightId.legType");
		binder.forField(sbt).withConverter(new LocalDateTimeToDateConverter(ZoneId.systemDefault())).bind("sbt");
		binder.forField(ebt).withConverter(new LocalDateTimeToDateConverter(ZoneId.systemDefault())).bind("ebt");
		binder.forField(abt).withConverter(new LocalDateTimeToDateConverter(ZoneId.systemDefault())).bind("abt");
		Binder.Binding<Flight, String> scheduledDependingBinding = binder.forField(gate).withNullRepresentation("")
				.withValidator(e -> sbt.getValue() == null ? true : e != null, "Gate should be set when scheduled")
				.bind(Flight::getGate, Flight::setGate);
		sbt.addValueChangeListener(e -> scheduledDependingBinding.validate());

		binder.bindInstanceFields(this);

		binder.withValidator(e -> new FlightValidator().isValid(e, null), FlightValid.MESSAGE);

		flightNumber.setRequiredIndicatorVisible(true);

		FormLayout f = new FormLayout();

		f.add(airline, flightNumber, flightSuffix, date, legType, sbt, ebt, abt, gate, canceled);

		Span statusLabel = new Span();
		binder.setStatusLabel(statusLabel);		
		f.add(statusLabel);
				
		binder.setBean(new Flight());

		return f;
	}

}
