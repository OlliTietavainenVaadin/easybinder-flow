package org.vaadin.easybinder.usagetest;

import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HasValue;
import org.junit.BeforeClass;
import org.vaadin.easybinder.data.ReflectionBinder;
import org.vaadin.easybinder.testentity.Flight;

import java.util.stream.Stream;

public class ManualReflectionBindingTest extends BaseTests {

    static ReflectionBinder<Flight> binder = new ReflectionBinder<>(Flight.class);

    @BeforeClass
    public static void setup() {
        binder.bind(form.airline, "flightId.airline");
        binder.bind(form.flightNumber, "flightId.flightNumber");
        binder.bind(form.flightSuffix, "flightId.flightSuffix");
        binder.bind(form.date, "flightId.date");
        binder.bind(form.legType, "flightId.legType");
        binder.bind(form.sbt, "sbt");
        binder.bind(form.ebt, "ebt");
        binder.bind(form.abt, "abt");
        binder.bind(form.gate, "gate");
        binder.bind(form.canceled, "canceled");
    }

    @Override
    protected void setBean(Flight flight) {
        binder.setBean(flight);
    }

	@Override
	protected Stream<HasValue<?, ?>> getFields() {
		return null;
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
