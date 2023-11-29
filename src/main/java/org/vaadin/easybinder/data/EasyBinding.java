package org.vaadin.easybinder.data;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.binder.*;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class EasyBinding<BEAN, FIELDVALUE, TARGET> implements Binder.Binding<BEAN, TARGET> {
    protected final HasValue<?, FIELDVALUE> field;
    protected final ValueProvider<BEAN, TARGET> getter;
    protected final Setter<BEAN, TARGET> setter;
    protected final String property;

    protected final Converter<FIELDVALUE, TARGET> converterValidatorChain;
    private final BasicBinder<BEAN> binder;
    protected Registration registration;

    protected String conversionError = null;
    protected String validationError = null;

    protected BindingValidationStatusHandler statusHandler = s -> {
        HasValue<?, ?> field = s.getField();
        if (s.getMessage().isPresent()) {
            if (field instanceof Component) {
                ComponentUtil.setData((Component) field, "error", s.getMessage().get());
            }
        } else {
            if (field instanceof Component) {
                ComponentUtil.setData((Component) field, "error", null);
            }
        }
    };
    private boolean asRequiredSet = false;
    private boolean validatorsDisabled = false;
    private boolean convertBackToPresentation = true;

    public EasyBinding(BasicBinder<BEAN> binder, HasValue<?, FIELDVALUE> field, ValueProvider<BEAN, TARGET> getter,
                       Setter<BEAN, TARGET> setter, String property,
                       Converter<FIELDVALUE, TARGET> converterValidatorChain) {
        this.field = field;
        this.getter = getter;
        this.setter = setter;
        this.property = property;
        this.binder = binder;
        this.converterValidatorChain = converterValidatorChain;

        registration = field.addValueChangeListener(e -> {
            if (binder.getBean() != null) {
                if (binder.fieldToBean(this)) {
                    binder.fireValueChangeEvent(e);
                }
            }
        });

        if (setter == null) {
            field.setReadOnly(true);
        }
    }

    public void beanToField(BEAN bean) {
        field.setValue(converterValidatorChain.convertToPresentation(getter.apply(bean), createValueContext()));
    }

    @Override
    public HasValue<?, FIELDVALUE> getField() {
        return field;
    }

    /**
     * Creates a value context from the current state of the binding and its field.
     *
     * @return the value context
     */
    protected ValueContext createValueContext() {
        if (field instanceof Component) {
            return new ValueContext((Component) field, field);
        }
        return new ValueContext(null, field, findLocale());
    }

    /**
     * Finds an appropriate locale to be used in conversion and validation.
     *
     * @return the found locale, not null
     */
    protected Locale findLocale() {
        Locale l = null;
        if (field instanceof Component && ((Component) field).getUI().isPresent()){
            l = ((Component) field).getUI().get().getLocale();
        }
        if (l == null && UI.getCurrent() != null) {
            l = UI.getCurrent().getLocale();
        }
        if (l == null) {
            l = Locale.getDefault();
        }
        return l;
    }

    public Optional<String> getProperty() {
        return Optional.ofNullable(property);
    }

    public boolean hasValidationError() {
        return validationError != null;
    }

    public boolean hasConversionError() {
        return conversionError != null;
    }

    public boolean hasError() {
        return hasValidationError() || hasConversionError();
    }

    @Override
    public BindingValidationStatus<TARGET> validate() {
        return validate(true);
    }

    protected void setConversionError(String errorMessage) {
        Objects.requireNonNull(errorMessage);
        conversionError = errorMessage;
    }

    protected void clearConversionError() {
        conversionError = null;
    }

    public void clearValidationError() {
        validationError = null;
    }

    public Optional<String> getValidationError() {
        return Optional.ofNullable(validationError);
    }

    public void setValidationError(String errorMessage) {
        Objects.requireNonNull(errorMessage);
        validationError = errorMessage;
    }

    public Optional<String> getError() {
        if (conversionError != null) {
            return Optional.of(conversionError);
        } else {
            return Optional.ofNullable(validationError);
        }
    }

    // Since 8.4
    //@Override
    public ValueProvider<BEAN, TARGET> getGetter() {
        return getter;
    }

    // Since 8.2
    //@Override
    //@SuppressWarnings("deprecation")
    public BindingValidationStatus<TARGET> validate(boolean fireEvent) {
        Objects.requireNonNull(binder,
                "This Binding is no longer attached to a Binder");
        Result<TARGET> res = null;
        Binder.Binding<?, TARGET> binding = null;
        BindingValidationStatus<TARGET> status = new BindingValidationStatus<>(res, binding);
        if (fireEvent) {
            getBinder().getValidationStatusHandler()
                    .statusChange(new BasicBinderValidationStatus<>(getBinder(),
                            Collections.<BasicBinderValidationStatus<?>>singletonList(status),
                            Collections.emptyList()));
            getBinder().fireStatusChangeEvent();
        }
        return status;
    }

    private BasicBinder<BEAN> getBinder() {
        return binder;
    }





    /**
     * Returns the field value run through all converters and validators,
     * but doesn't pass the {@link BindingValidationStatus} to any status
     * handler.
     *
     * @return the result of the conversion
     */
    private Result<TARGET> doConversion() {
        return execute(() -> {
            FIELDVALUE fieldValue = field.getValue();
            return converterValidatorChain.convertToModel(fieldValue,
                    createValueContext());
        });
    }

    private <T> T execute(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (BindingException exception) {
            throw exception;
        } catch (Exception exception) {
            if (binder == null) {
                throw new IllegalStateException(
                        "This binding is already unbound", exception);
            }
            BindingExceptionHandler handler = binder
                    .getBindingExceptionHandler();
            Optional<BindingException> bindingException = handler
                    .handleException(field, exception);
            if (bindingException.isPresent()) {
                BindingException toThrow = bindingException.get();
                toThrow.fillInStackTrace();
                throw toThrow;
            } else {
                throw exception;
            }
        }
    }

    // Since 8.2
    //@Override
    public BindingValidationStatusHandler getValidationStatusHandler() {
        return statusHandler;
    }

    // Since 8.2
    //@Override
    public void unbind() {
        registration.remove();
    }

    // Since 8.2
    //@Override
    public void read(BEAN bean) {
        if (setter == null || field.isReadOnly()) {
            return;
        }
        Result<TARGET> result = converterValidatorChain.convertToModel(field.getValue(), createValueContext());
        result.ifError(e -> setConversionError(e));
        result.ifOk(e -> {
            clearConversionError();
            setter.accept(bean, e);
        });
    }

    // Since 8.4
    //@Override
    public Setter<BEAN, TARGET> getSetter() {
        return setter;
    }

    @Override
    public void setAsRequiredEnabled(boolean asRequiredEnabled) {
        if (!asRequiredSet) {
            throw new IllegalStateException(
                    "Unable to toggle asRequired validation since "
                            + "asRequired has not been set.");
        }
        if (asRequiredEnabled != isAsRequiredEnabled()) {
            field.setRequiredIndicatorVisible(asRequiredEnabled);
            validate();
        }
    }

    @Override
    public boolean isAsRequiredEnabled() {
        return field.isRequiredIndicatorVisible();
    }

    @Override
    public void setValidatorsDisabled(boolean validatorsDisabled) {
        this.validatorsDisabled = validatorsDisabled;
    }

    @Override
    public boolean isValidatorsDisabled() {
        return validatorsDisabled;
    }

    @Override
    public void setConvertBackToPresentation(boolean convertBackToPresentation) {
        this.convertBackToPresentation = convertBackToPresentation;
    }

    @Override
    public boolean isConvertBackToPresentation() {
        return convertBackToPresentation;
    }

    // Since 8.4
    //@Override
    public boolean isReadOnly() {
        return (setter == null || field.isReadOnly());
    }

    public void setReadOnly(boolean readOnly) {
        field.setReadOnly(setter == null || readOnly);
    }

}
