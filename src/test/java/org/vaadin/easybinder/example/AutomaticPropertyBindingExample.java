package org.vaadin.easybinder.example;

import java.util.EnumSet;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.binder.PropertyId;
import org.vaadin.addonhelpers.AbstractTest;
import org.vaadin.easybinder.data.AutoBinder;
import org.vaadin.easybinder.testentity.Flight;
import org.vaadin.easybinder.testentity.FlightId.LegType;
import com.vaadin.flow.component.textfield.TextField;

@SuppressWarnings("serial")
public class AutomaticPropertyBindingExample extends AbstractTest {

	@PropertyId("flightId.airline")
	TextField airline = new TextField("Airline");
	@PropertyId("flightId.flightNumber")
	TextField flightNumber = new TextField("Flight number");
	@PropertyId("flightId.flightSuffix")
	TextField flightSuffix = new TextField("Flight suffix");
	@PropertyId("flightId.date")
	DatePicker date = new DatePicker("Date");
	@PropertyId("flightId.legType")
	RadioButtonGroup<LegType> legType = new RadioButtonGroup<>("Leg type", EnumSet.allOf(LegType.class));
	DateTimePicker sbt = new DateTimePicker("SBT");
	DateTimePicker ebt = new DateTimePicker("EBT");
	DateTimePicker abt = new DateTimePicker("ABT");
	TextField gate = new TextField("Gate");
	Checkbox canceled = new Checkbox("Canceled");
	
	@Override
	public Component getTestComponent() {
		AutoBinder<Flight> binder = new AutoBinder<>(Flight.class);
		binder.bindInstanceFields(this);

		FormLayout f = new FormLayout();
		f.add(airline, flightNumber, flightSuffix, date, legType, sbt, ebt, abt, gate, canceled);

		Span statusLabel = new Span();
		binder.setStatusLabel(statusLabel);		
		f.add(statusLabel);
		
		binder.setBean(new Flight());

		return f;
	}
}
