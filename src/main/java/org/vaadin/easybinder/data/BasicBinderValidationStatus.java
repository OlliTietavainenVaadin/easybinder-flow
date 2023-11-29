/*
 * Copyright 2017 Lars Sønderby Jessen
 *
 * Mostly based on code copied from Vaadin Framework (BinderValidationStatus)
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

import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.function.SerializablePredicate;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Binder validation status change. Represents the outcome of binder level
 * validation. Use
 * {@link BasicBinder#setValidationStatusHandler(BinderValidationStatusHandler)}
 * to handle form level validation status changes.
 *
 * @param <BEAN> the bean type of the binder
 * @see BinderValidationStatusHandler
 * @see BasicBinder#setValidationStatusHandler(BinderValidationStatusHandler)
 * @see BasicBinder#validate()
 * @see BindingValidationStatus
 */
@SuppressWarnings("serial")
public class BasicBinderValidationStatus<BEAN> implements Serializable {

    private final transient BasicBinder<BEAN> binder;
    private final List<BasicBinderValidationStatus<?>> bindingStatuses;
    private final List<ValidationResult> binderStatuses;

    /**
     * Creates a new binder validation status for the given binder and validation
     * results.
     *
     * @param source          the source binder
     * @param bindingStatuses the validation results for the fields
     * @param binderStatuses  the validation results for binder level validation
     */
    public BasicBinderValidationStatus(BasicBinder<BEAN> source, List<BasicBinderValidationStatus<?>> bindingStatuses,
                                       List<ValidationResult> binderStatuses) {
        Objects.requireNonNull(binderStatuses, "binding statuses cannot be null");
        Objects.requireNonNull(binderStatuses, "binder statuses cannot be null");
        this.binder = source;
        this.bindingStatuses = Collections.unmodifiableList(bindingStatuses);
        this.binderStatuses = Collections.unmodifiableList(binderStatuses);
    }


    /**
     * Gets whether validation for the binder passed or not.
     *
     * @return {@code true} if validation has passed, {@code false} if not
     */
    public boolean isOk() {
        return !hasErrors();
    }

    /**
     * Gets whether the validation for the binder failed or not.
     *
     * @return {@code true} if validation failed, {@code false} if validation passed
     */
    public boolean hasErrors() {
        return binderStatuses.stream().filter(ValidationResult::isError).findAny().isPresent()
                || bindingStatuses.stream().filter(BasicBinderValidationStatus::hasErrors).findAny().isPresent();
    }

    /**
     * Gets the source binder of the status.
     *
     * @return the source binder
     */
    public BasicBinder<BEAN> getBinder() {
        return binder;
    }

    /**
     * Gets both field and bean level validation errors.
     *
     * @return a list of all validation errors
     */
    public List<ValidationResult> getValidationErrors() {
        List<ValidationResult> errors = getFieldValidationErrors().stream().map(s -> s.getResult().get())
                .collect(Collectors.toList());
        errors.addAll(getBeanValidationErrors());
        return errors;
    }

    /**
     * Gets the field level validation statuses.
     * <p>
     * The field level validators have been added with
     * {@link BindingBuilder#withValidator(Validator)}.
     *
     * @return the field validation statuses
     */
    public List<BindingValidationStatus<?>> getFieldValidationStatuses() {
        return bindingStatuses;
    }

    /**
     * Gets the bean level validation results.
     *
     * @return the bean level validation results
     */
    public List<ValidationResult> getBeanValidationResults() {
        return binderStatuses;
    }

    /**
     * Gets the failed field level validation statuses.
     * <p>
     * The field level validators have been added with
     * {@link BindingBuilder#withValidator(Validator)}.
     *
     * @return a list of failed field level validation statuses
     */
    public List<BindingValidationStatus<?>> getFieldValidationErrors() {
        return bindingStatuses.stream().filter(BindingValidationStatus::isError).collect(Collectors.toList());
    }

    /**
     * Gets the failed bean level validation results.
     *
     * @return a list of failed bean level validation results
     */
    public List<ValidationResult> getBeanValidationErrors() {
        return binderStatuses.stream().filter(ValidationResult::isError).collect(Collectors.toList());
    }

    /**
     * Notifies all validation status handlers in bindings.
     *
     * @see #notifyBindingValidationStatusHandlers(SerializablePredicate)
     */
    public void notifyBindingValidationStatusHandlers() {
        notifyBindingValidationStatusHandlers(t -> true);
    }

    /**
     * Notifies validation status handlers for bindings that pass given filter. The
     * filter should return {@code true} for each {@link BindingValidationStatus}
     * that should be delegated to the status handler in the binding.
     *
     * @param filter the filter to select bindings to run status handling for
     * @see #notifyBindingValidationStatusHandlers()
     */
    @SuppressWarnings("unchecked")
    public void notifyBindingValidationStatusHandlers(SerializablePredicate<BindingValidationStatus<?>> filter) {
        bindingStatuses.stream().filter(filter)
                .forEach(s -> ((EasyBinding<BEAN, ?, ?>) s.getBinding()).getValidationStatusHandler()
                        .statusChange(s));
    }
}
