package org.vaadin.easybinder.ui;

import com.vaadin.flow.component.HasValue;
import org.vaadin.easybinder.data.HasGenericType;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.customfield.CustomField;

@SuppressWarnings("serial")
public class EGTypeComponentAdapter<T> extends CustomField<T> implements HasGenericType<T> {

	Class<T> genericType;
	Component component;
	HasValue<?, T> hasValue;

	T value;

	@SuppressWarnings("unchecked")
	public EGTypeComponentAdapter(Class<T> genericType, Component adaptee) {
		this.genericType = genericType;
		this.component = adaptee;
		hasValue = (HasValue<?, T>) adaptee;
		hasValue.addValueChangeListener(e -> setValue(e.getValue()));
	}

	@Override
	public Class<T> getGenericType() {
		return genericType;
	}

	@Override
	public T getValue() {
		return hasValue.getValue();
	}

	public Component getEmbeddedComponent() {
		return component;
	}

	@Override
	protected T generateModelValue() {
		return hasValue.getValue();
	}

	@Override
	protected void setPresentationValue(T t) {
		hasValue.setValue(t);
	}
}
