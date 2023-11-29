/*
 * Copyright 2017 Lars Sønderby Jessen
 *
 * Partly based on code copied from Vaadin Framework (Binder)
 * Copyright 2000-2016 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.vaadin.easybinder.data;

import com.vaadin.flow.component.*;
import com.vaadin.flow.data.binder.*;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("serial")
public class BasicBinder<BEAN> {

    protected BEAN bean;
    protected HasText statusLabel;
    protected List<EasyBinding<BEAN, ?, ?>> bindings = new LinkedList<>();
    protected Map<String, EasyBinding<BEAN, ?, ?>> propertyToBindingMap = new HashMap<>();
    protected Set<ConstraintViolation<BEAN>> constraintViolations;
    protected boolean hasChanges = false;
    protected Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    protected Class<?>[] groups = new Class<?>[0];
    protected BasicBinderValidationStatusHandler<BEAN> statusHandler;
    protected BasicBinderValidationStatus<BEAN> status;
    private HashMap<Class<?>, List<SerializableConsumer<?>>> listeners = new HashMap<>();

    public BasicBinder() {
        validate();
    }

    public BEAN getBean() {
        return bean;
    }

    public void setBean(BEAN bean) {
        this.bean = null;

        if (bean != null) {
            bindings.forEach(e -> e.beanToField(bean));
        }

        this.bean = bean;
        validate();
        fireStatusChangeEvent();
        hasChanges = false;
    }

    public void removeBean() {
        setBean(null);
    }

    public Class<?>[] getValidationGroups() {
        return groups.clone();
    }

    public void setValidationGroups(Class<?>... groups) {
        this.groups = groups;
        validate();
    }

    public void clearValidationGroups() {
        groups = new Class<?>[0];
        validate();
    }

    public boolean isValid() {
        return constraintViolations.isEmpty();
    }

    public <FIELDVALUE, TARGET> EasyBinding<BEAN, FIELDVALUE, FIELDVALUE> bind(HasValue<?, FIELDVALUE> field,
                                                                               ValueProvider<BEAN, FIELDVALUE> getter, Setter<BEAN, FIELDVALUE> setter, String property) {
        return bind(field, getter, setter, property, Converter.identity());
    }

    public <FIELDVALUE, TARGET> EasyBinding<BEAN, FIELDVALUE, TARGET> bind(HasValue<?, FIELDVALUE> field,
                                                                           ValueProvider<BEAN, TARGET> getter, Setter<BEAN, TARGET> setter, String property,
                                                                           Converter<FIELDVALUE, TARGET> converter) {

        Objects.requireNonNull(field);
        Objects.requireNonNull(getter);
        Objects.requireNonNull(converter);

        // Register as binding
        EasyBinding<BEAN, FIELDVALUE, TARGET> binding = new EasyBinding<BEAN, FIELDVALUE, TARGET>(this, field, getter,
                setter, property, converter);

        // TODO: remove from binding
        /*
         * binding.registration = field.addValueChangeListener(e -> { if (getBean() !=
         * null) { if(fieldToBean(binding)) { fireValueChangeEvent(e); } } });
         */

        bindings.add(binding);

        // Add property to validation error map
        if (property != null) {
            propertyToBindingMap.put(property, binding);
        }

        if (getBean() != null) {
            if (fieldToBean(binding)) {
                // TODO: should this be fired?
                // fireValueChangeEvent(e);
            }
        } else {
            fireStatusChangeEvent();
        }

        return binding;
    }

    public void removeAllBindings() {
        while (!bindings.isEmpty()) {
            EasyBinding<BEAN, ?, ?> binding = bindings.remove(0);
            binding.getProperty().ifPresent(e -> propertyToBindingMap.remove(e));
            binding.unbind();
        }
    }

    public void removeBinding(HasValue<?, ?> field) {
        bindings.stream().filter(e -> e.getField().equals(field)).findFirst().ifPresent(e -> clearBinding(e));
        validate();
    }

    public <FIELDVALUE, TARGET> void removeBinding(EasyBinding<BEAN, FIELDVALUE, TARGET> binding) {
        clearBinding(binding);
        validate();
    }

    public void removeBinding(String propertyValue) {
        Objects.requireNonNull(propertyValue);
        Optional.ofNullable(propertyToBindingMap.get(propertyValue)).ifPresent(e -> removeBinding(e));
        validate();
    }

    protected <FIELDVALUE, TARGET> void clearBinding(EasyBinding<BEAN, FIELDVALUE, TARGET> binding) {
        if (bindings.remove(binding)) {
            binding.unbind();
        }
        binding.getProperty().ifPresent(e -> propertyToBindingMap.remove(e));
    }

    public Stream<HasValue<?, ?>> getFields() {
        return bindings.stream().map(e -> e.getField());
    }

    protected void handleConstraintViolations(ConstraintViolation<BEAN> v,
                                              Function<ConstraintViolation<BEAN>, String> f) {
        String property = v.getPropertyPath().toString();
        if (property.isEmpty()) {
            // Bean level validation error
        } else {
            // Field validation error
            Optional.ofNullable(propertyToBindingMap.get(property)).ifPresent(e -> e.setValidationError(f.apply(v)));
        }
    }

    protected void validate() {
        // Clear all validation errors
        propertyToBindingMap.values().stream().forEach(e -> e.clearValidationError());

        // Validate and set validation errors
        if (getBean() != null) {
            constraintViolations = validator.validate(getBean(), groups);
            constraintViolations.stream().forEach(e -> handleConstraintViolations(e, f -> f.getMessage()));
        } else {
            constraintViolations = new HashSet<ConstraintViolation<BEAN>>();
        }

        List<BindingValidationStatus<?>> binRes =
                getBindings().stream().map(e -> e.validate(false)).collect(Collectors.toList());

        List<ValidationResult> valRes =
                constraintViolations.stream()
                        .filter(e -> e.getPropertyPath().toString().isEmpty())
                        .map(e -> ValidationResult.error(e.getMessage()))
                        .collect(Collectors.toList());

        status = new BasicBinderValidationStatus<BEAN>(this, binRes, valRes);

        getValidationStatusHandler().statusChange(status);
    }

    /**
     * Gets the status label or an empty optional if none has been set.
     *
     * @return the optional status label
     * @see #setStatusLabel(Label)
     */
    public Optional<HasText> getStatusLabel() {
        return Optional.ofNullable(statusLabel);
    }

    public void setStatusLabel(HasText statusLabel) {
        this.statusLabel = statusLabel;
    }

    public Optional<HasValue<?, ?>> getFieldForProperty(String propertyName) {
        return Optional.ofNullable(propertyToBindingMap.get(propertyName)).map(e -> e.getField());
    }

    /**
     * Adds field value change listener to all the fields in the binder.
     * <p>
     * Added listener is notified every time whenever any bound field value is
     * changed. The same functionality can be achieved by adding a
     * {@link ValueChangeListener} to all fields in the {@link Binder}.
     * <p>
     * The listener is added to all fields regardless of whether the method is
     * invoked before or after field is bound.
     *
     * @param listener a field value change listener
     * @return a registration for the listener
     * @see ValueChangeEvent
     * @see ValueChangeListener
     */
    public Registration addValueChangeListener(HasValue.ValueChangeListener<?> listener) {
        return addListener(HasValue.ValueChangeEvent.class, listener::valueChanged);
    }

    /**
     * Adds a listener to the binder.
     *
     * @param eventType the type of the event
     * @param method    the consumer method of the listener
     * @param <T>       the event type
     * @return a registration for the listener
     */
    private <T> Registration addListener(Class<?> eventType, SerializableConsumer<T> method) {
            List<SerializableConsumer<?>> list = listeners
                    .computeIfAbsent(eventType, key -> new ArrayList<>());
            return Registration.addAndRemove(list, method);
    }

    /**
     * Adds status change listener to the binder.
     * <p>
     * The {@link Binder} status is changed whenever any of the following happens:
     * <ul>
     * <li>if it's bound and any of its bound field or select has been changed
     * <li>{@link #setBean(Object)} is called
     * <li>{@link #removeBean()} is called
     * <li>{@link #bind(HasValue, ValueProvider, Setter, String)} is called
     * </ul>
     *
     * @param listener status change listener to add, not null
     * @return a registration for the listener
     * @see #setBean(Object)
     * @see #removeBean()
     */
    public Registration addStatusChangeListener(BinderStatusChangeListener listener) {
        return addListener(BinderStatusChangeEvent.class, listener::statusChange);
    }

    public boolean getHasChanges() {
        return hasChanges;
    }

    protected <V> void fireValueChangeEvent(HasValue.ValueChangeEvent<V> event) {
        hasChanges = true;
        fireEvent(event);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void fireEvent(Object event) {
        new HashMap<>(listeners).entrySet().stream().filter(
                        entry -> entry.getKey().isAssignableFrom(event.getClass()))
                .forEach(entry -> {
                    for (Consumer consumer : new ArrayList<>(
                            entry.getValue())) {
                        consumer.accept(event);
                    }
                });
    }

    protected void fireStatusChangeEvent() {
        boolean hasConversionErrors = bindings.stream().anyMatch(e -> e.hasConversionError());
        fireEvent(new BinderStatusChangeEvent(this, hasConversionErrors, !constraintViolations.isEmpty()));
    }

    public Optional<EasyBinding<BEAN, ?, ?>> getBinding(String propertyName) {
        Objects.requireNonNull(propertyName);
        return Optional.ofNullable(propertyToBindingMap.get(propertyName));
    }

    public void setReadonly(boolean readOnly) {
        bindings.stream().forEach(e -> e.setReadOnly(readOnly));
    }

    protected boolean fieldToBean(EasyBinding<BEAN, ?, ?> binding) {
        Optional<String> currentError = binding.getError();

        binding.read(getBean());
        if (!binding.hasConversionError()) {
            Optional<String> currentValidationError = binding.getValidationError();
            validate();
            if (!currentValidationError.equals(binding.getValidationError())) {
                // TODO: only fire if global change
                fireStatusChangeEvent();
            }
        }

        if (!currentError.equals(binding.getError())) {
            binding.validate(true);
        }
        fireStatusChangeEvent();

        return !binding.hasConversionError();
    }

    public List<EasyBinding<BEAN, ?, ?>> getBindings() {
        return Collections.unmodifiableList(bindings);
    }

    public Set<ConstraintViolation<BEAN>> getConstraintViolations() {
        return constraintViolations;
    }

    /**
     * Gets the status handler of this form.
     * <p>
     * If none has been set with
     * {@link #setValidationStatusHandler(BinderValidationStatusHandler)}, the
     * default implementation is returned.
     *
     * @return the status handler used, never <code>null</code>
     * @see #setValidationStatusHandler(BinderValidationStatusHandler)
     */
    public BasicBinderValidationStatusHandler<BEAN> getValidationStatusHandler() {
        return Optional.ofNullable(statusHandler).orElse(this::handleBinderValidationStatus);
    }

    /**
     * Sets the status handler to track form status changes.
     * <p>
     * Setting this handler will override the default behavior, which is to let
     * fields show their validation status messages and show binder level
     * validation errors or OK status in the label set with
     * {@link #setStatusLabel(Label)}.
     * <p>
     *
     * @param statusHandler the status handler to set, not <code>null</code>
     * @throws NullPointerException for <code>null</code> status handler
     * @see #setStatusLabel(Label)
     * @see BindingBuilder#withValidationStatusHandler(BindingValidationStatusHandler)
     */
    public void setValidationStatusHandler(BasicBinderValidationStatusHandler<BEAN> statusHandler) {
        Objects.requireNonNull(statusHandler,
                "Cannot set a null " + BasicBinderValidationStatusHandler.class.getSimpleName());
        this.statusHandler = statusHandler;
    }

    /**
     * The default binder level status handler.
     * <p>
     * Passes all field related results to the Binding status handlers. All
     * other status changes are displayed in the status label, if one has been
     * set with {@link #setStatusLabel(Label)}.
     *
     * @param binderStatus status of validation results from binding and/or bean level
     *                     validators
     */
    protected void handleBinderValidationStatus(BasicBinderValidationStatus<BEAN> binderStatus) {
        // let field events go to binding status handlers
        binderStatus.notifyBindingValidationStatusHandlers();

        // show first possible error or OK status in the label if set
        if (getStatusLabel().isPresent()) {
            String statusMessage = binderStatus.getBeanValidationErrors().stream().findFirst()
                    .map(ValidationResult::getErrorMessage).orElse("");
            getStatusLabel().get().setText(statusMessage);
        }
    }

    public BasicBinderValidationStatus<BEAN> getValidationStatus() {
        return status;
    }

    public static class EasyBinding<BEAN, FIELDVALUE, TARGET> implements Binder.Binding<BEAN, TARGET> {
        protected final HasValue<?, FIELDVALUE> field;
        protected final ValueProvider<BEAN, TARGET> getter;
        protected final Setter<BEAN, TARGET> setter;
        protected final String property;

        protected final Converter<FIELDVALUE, TARGET> converterValidatorChain;
        protected Registration registration;

        protected String conversionError = null;
        protected String validationError = null;

        protected BindingValidationStatusHandler statusHandler = s -> {
            HasValue<?, ?> field = s.getField();
            if (s.getMessage().isPresent()) {
                if (field instanceof Component) {
                    ComponentUtil.setData((Component) field, "error", s.getMessage().get());
                    ;
                }
            } else {
                if (field instanceof Component) {
                    ComponentUtil.setData((Component) field, "error", null);
                }
            }
        };
        private boolean asRequiredSet;
        private boolean validatorsDisabled;
        private boolean convertBackToPresentation = true;

        public EasyBinding(BasicBinder<BEAN> binder, HasValue<?, FIELDVALUE> field, ValueProvider<BEAN, TARGET> getter,
                           Setter<BEAN, TARGET> setter, String property,
                           Converter<FIELDVALUE, TARGET> converterValidatorChain) {
            this.field = field;
            this.getter = getter;
            this.setter = setter;
            this.property = property;
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
            if (field instanceof Component && ((Component) field).getUI().isPresent()) {
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
            Result<TARGET> result = hasError() ? Result.error("") : Result.ok(null);
            BindingValidationStatus<TARGET> status = new BindingValidationStatus<>(result, this);

            if (fireEvent) {
                getValidationStatusHandler().statusChange(status);
            }

            return status;
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
        public boolean isAsRequiredEnabled() {
            return field.isRequiredIndicatorVisible();
        }

        @Override
        public void setAsRequiredEnabled(boolean asRequiredEnabled) {
            if (asRequiredEnabled != isAsRequiredEnabled()) {
                field.setRequiredIndicatorVisible(asRequiredEnabled);
                validate();
            }
        }

        @Override
        public boolean isValidatorsDisabled() {
            return validatorsDisabled;
        }

        @Override
        public void setValidatorsDisabled(boolean validatorsDisabled) {
            this.validatorsDisabled = validatorsDisabled;
        }

        @Override
        public boolean isConvertBackToPresentation() {
            return convertBackToPresentation;
        }

        @Override
        public void setConvertBackToPresentation(boolean convertBackToPresentation) {
            this.convertBackToPresentation = convertBackToPresentation;
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
}
