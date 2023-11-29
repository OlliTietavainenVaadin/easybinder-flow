package org.vaadin.easybinder.usagetest;

import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.BeforeClass;
import org.vaadin.easybinder.data.AutoBinder;
import org.vaadin.easybinder.testentity.Flight;
import org.vaadin.easybinder.testentity.FlightId.LegType;

import java.util.stream.Stream;


public class BuildAndBindTest extends BaseTests {

	static AutoBinder<Flight> binder = new AutoBinder<>(Flight.class);

	@SuppressWarnings("unchecked")
	@BeforeClass
	public static void setup() {
		binder.buildAndBind("flightId");

		form.airline = (TextField) binder.getFieldForProperty("flightId.airline").get();
		form.flightNumber = (TextField) binder.getFieldForProperty("flightId.flightNumber").get();
		form.flightSuffix = (TextField) binder.getFieldForProperty("flightId.flightSuffix").get();
		form.date = (DatePicker) binder.getFieldForProperty("flightId.date").get();
		form.legType = (ComboBox<LegType>) binder.getFieldForProperty("flightId.legType").get();
		form.sbt = (DateTimePicker) binder.getFieldForProperty("sbt").get();
		form.ebt = (DateTimePicker) binder.getFieldForProperty("ebt").get();
		form.abt = (DateTimePicker) binder.getFieldForProperty("abt").get();
		form.gate = (TextField) binder.getFieldForProperty("gate").get();
		form.canceled = (Checkbox) binder.getFieldForProperty("canceled").get();
	}

	@Override
	protected void setBean(Flight flight) {
		binder.setBean(flight);
	}

	@Override
	protected Stream<HasValue<?, ?>> getFields() {
		return binder.getFields();
	}

	@Override
	protected boolean isValid() {
		return binder.isValid();
	}
	
	@Override
	protected void setStatusLabel(HasText label) {
		binder.setStatusLabel(label);
	}	
}
