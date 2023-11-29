package org.vaadin.easybinder.example;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import org.vaadin.addonhelpers.AbstractTest;
import org.vaadin.easybinder.data.AutoBinder;
import org.vaadin.easybinder.testentity.Flight;

public class BuildAndBindExample extends AbstractTest {
	private static final long serialVersionUID = 1L;

	@Override
	public Component getTestComponent() {
		AutoBinder<Flight> binder = new AutoBinder<>(Flight.class);
		
		FormLayout f = new FormLayout();
		f.add(binder.buildAndBind("flightId"));
		
		Span statusLabel = new Span();
		binder.setStatusLabel(statusLabel);		
		f.add(statusLabel);

		binder.setBean(new Flight());

		return f;
	}
}
