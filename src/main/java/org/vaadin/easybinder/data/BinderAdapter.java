package org.vaadin.easybinder.data;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.*;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import org.vaadin.easybinder.data.BasicBinder.EasyBinding;


@SuppressWarnings("serial")
public class BinderAdapter<BEAN> extends Binder<BEAN> {
	transient BasicBinder<BEAN> binder;
	Class<BEAN> clz;

	BinderValidationStatusHandler<BEAN> statusHandler;

	public BinderAdapter(BasicBinder<BEAN> binder, Class<BEAN> clz) {
		this.binder = binder;
		this.clz = clz;

		binder.addStatusChangeListener(e -> {
			if (statusHandler != null) {
				statusHandler.statusChange(validate());
			}
		});
	}

	public BinderAdapter(ReflectionBinder<BEAN> binder) {
		this(binder, binder.getGenericType());
	}

	@Override
	public BEAN getBean() {
		return binder.getBean();
	}

	@Override
	public <FIELDVALUE> BindingBuilder<BEAN, FIELDVALUE> forField(HasValue<?, FIELDVALUE> field) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public <FIELDVALUE> BindingBuilder<BEAN, FIELDVALUE> forMemberField(HasValue<?, FIELDVALUE> field) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public <FIELDVALUE> Binding<BEAN, FIELDVALUE> bind(HasValue<?, FIELDVALUE> field,
													   ValueProvider<BEAN, FIELDVALUE> getter, Setter<BEAN, FIELDVALUE> setter) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public <FIELDVALUE> Binding<BEAN, FIELDVALUE> bind(HasValue<?, FIELDVALUE> field, String propertyName) {
		if (binder instanceof ReflectionBinder) {
			return ((ReflectionBinder<BEAN>) binder).bind(field, propertyName);
		} else {
			throw new UnsupportedOperationException("Not supported");
		}
	}

	@Override
	public void setBean(BEAN bean) {
		binder.setBean(bean);
	}

	@Override
	public void removeBean() {
		binder.removeBean();
	}

	@Override
	public void readBean(BEAN bean) {
		if (binder.getBean() == null) {
			try {
				Constructor<BEAN> ctor = clz.getConstructor();
				binder.setBean(ctor.newInstance());
			} catch (InvocationTargetException | IllegalArgumentException | IllegalAccessException
					| InstantiationException | NoSuchMethodException | SecurityException e) {
				throw new UnsupportedOperationException("Not supported");
			}
		}
		BEAN targetBean = binder.getBean();
		binder.removeBean();
		binder.getBindings().stream().forEach(e -> {
			@SuppressWarnings("unchecked")
			Setter<BEAN, Object> setter = (Setter<BEAN, Object>) e.setter;
			if (setter != null) {
				setter.accept(targetBean, e.getter.apply(bean));
			}
		});
		binder.setBean(targetBean);
	}

	@Override
	public void writeBean(BEAN bean) throws ValidationException {
		if (!binder.isValid()) {
			BasicBinderValidationStatus<BEAN> vs = binder.getValidationStatus();
			throw new ValidationException(vs.getFieldValidationErrors(), vs.getBeanValidationErrors());
		}

		BEAN sourceBean = binder.getBean();
		if (sourceBean == null) {
			return;
		}

		binder.getBindings().stream().forEach(e -> {
			@SuppressWarnings("unchecked")
			Setter<BEAN, Object> setter = (Setter<BEAN, Object>) e.setter;
			if (setter != null) {
				setter.accept(bean, e.getter.apply(sourceBean));
			}
		});
		// Trigger StatusChange (required by Grid editor).
		binder.removeBean();
		binder.setBean(sourceBean);
	}

	@Override
	public boolean writeBeanIfValid(BEAN bean) {
		if (!binder.isValid()) {
			return false;
		}
		try {
			writeBean(bean);
		} catch (ValidationException ex) {
			return false;
		}
		return true;
	}

	@Override
	public Binder<BEAN> withValidator(Validator<? super BEAN> validator) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public Binder<BEAN> withValidator(SerializablePredicate<BEAN> predicate, String message) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public Binder<BEAN> withValidator(SerializablePredicate<BEAN> predicate,
			ErrorMessageProvider errorMessageProvider) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public BinderValidationStatus<BEAN> validate() {
		BasicBinderValidationStatus<BEAN> vs = binder.getValidationStatus();
		return new BinderValidationStatus<BEAN>(this, vs.getFieldValidationStatuses(), vs.getBeanValidationResults());
	}

	@Override
	public boolean isValid() {
		return binder.isValid();
	}

	@Override
	public void setStatusLabel(HasText statusLabel) {
		super.setStatusLabel(statusLabel);
	}

	@Override
	public Optional<HasText> getStatusLabel() {
		return super.getStatusLabel();
	}

	@Override
	public void setReadOnly(boolean fieldsReadOnly) {
		binder.setReadonly(fieldsReadOnly);
	}

	@Override
	public void setValidationStatusHandler(BinderValidationStatusHandler<BEAN> statusHandler) {
		this.statusHandler = statusHandler;
	}

	@Override
	public BinderValidationStatusHandler<BEAN> getValidationStatusHandler() {
		return statusHandler;
	}

	@Override
	public Registration addStatusChangeListener(StatusChangeListener listener) {
		return binder.addStatusChangeListener(e -> listener.statusChange(new StatusChangeEvent(this, e.hasErrors())));
	}

	@Override
	public Registration addValueChangeListener(HasValue.ValueChangeListener<? super HasValue.ValueChangeEvent<?>> listener) {
		return binder.addValueChangeListener(listener);
	}

	@Override
	public boolean hasChanges() {
		return binder.hasChanges;
	}

	@Override
	public void bindInstanceFields(Object objectWithMemberFields) {
		if (binder instanceof AutoBinder) {
			((AutoBinder<BEAN>) binder).bindInstanceFields(objectWithMemberFields);
		} else {
			throw new UnsupportedOperationException("Not supported");
		}
	}

	@Override
	public Optional<Binding<BEAN, ?>> getBinding(String propertyName) {
		Optional<EasyBinding<BEAN, ?, ?>> binding = binder.getBinding(propertyName);
		if (binding.isPresent()) {
			return Optional.of(binding.get());
		} else {
			return Optional.empty();
		}
	}

	// @Override (Since 8.1)
	public Stream<HasValue<?, ?>> getFields() {
		return binder.getFields();
	}



	// @Override (Since 8.2)
	public void removeBinding(HasValue<?, ?> field) {
		binder.removeBinding(field);
	}

	// @Override (Since 8.2)
	@SuppressWarnings("unchecked")
	public void removeBinding(Binding<BEAN, ?> binding) {
		binder.removeBinding((EasyBinding<BEAN, ?, ?>) binding);
	}

	// @Override (Since 8.2)
	public void removeBinding(String propertyName) {
		binder.removeBinding(propertyName);
	}

}
