package org.vaadin.easybinder.example;

import java.util.EnumSet;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import org.vaadin.addonhelpers.AbstractTest;
import org.vaadin.easybinder.data.ReflectionBinder;
import org.vaadin.easybinder.testentity.Flight;
import org.vaadin.easybinder.testentity.FlightId.LegType;

@SuppressWarnings("serial")
public class ManualReflectionBindingExample extends AbstractTest {

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
		ReflectionBinder<Flight> binder = new ReflectionBinder<>(Flight.class);
		binder.bind(airline, "flightId.airline");
		binder.bind(flightNumber, "flightId.flightNumber");
		binder.bind(flightSuffix, "flightId.flightSuffix");
		binder.bind(date, "flightId.date");
		binder.bind(legType, "flightId.legType");
		binder.bind(sbt, "sbt");
		binder.bind(ebt, "ebt");
		binder.bind(abt, "abt");
		binder.bind(gate, "gate");
		binder.bind(canceled, "canceled");

		FormLayout f = new FormLayout();

		f.add(airline, flightNumber, flightSuffix, date, legType, sbt, ebt, abt, gate, canceled);

		Span statusLabel = new Span();
		binder.setStatusLabel(statusLabel);		
		f.add(statusLabel);
				
		binder.setBean(new Flight());

		return f;
	}
}
