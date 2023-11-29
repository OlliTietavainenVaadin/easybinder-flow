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

import com.googlecode.gentyref.GenericTypeReflector;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.*;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.provider.HasDataView;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.ReflectTools;
import org.vaadin.easybinder.data.converters.NullConverter;
import org.vaadin.easybinder.data.converters.NullConverterPrimitiveTarget;

import javax.validation.constraints.Min;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ReflectionBinder<BEAN> extends BasicBinder<BEAN> implements HasGenericType<BEAN> {
    private static RequiredFieldConfigurator min = (annotation, bindingBuilder) -> annotation.annotationType().equals(Min.class)
            && ((Min) annotation).value() > 0;
    protected Class<BEAN> clazz;
    protected PropertySet<BEAN> propertySet;
    protected Map<String, EasyBinding<BEAN, ?, ?>> boundProperties = new HashMap<String, EasyBinding<BEAN, ?, ?>>();
    protected ConverterRegistry converterRegistry = ConverterRegistry.getInstance();
    protected Logger log = Logger.getLogger(getClass().getName());

    protected RequiredFieldConfigurator requiredConfigurator = min.chain(RequiredFieldConfigurator.DEFAULT);

    public ReflectionBinder(Class<BEAN> clazz) {
        this.clazz = clazz;
        propertySet = BeanPropertySet.get(clazz);
    }

    public ReflectionBinder(Class<BEAN> clazz, ConverterRegistry converterRegistry) {
        this(clazz);
        this.converterRegistry = converterRegistry;
    }

    public <PRESENTATION, MODEL> EasyBinding<BEAN, PRESENTATION, MODEL> bind(HasValue<?, PRESENTATION> field,
                                                                             String propertyName) {

        boolean readOnly = false;

        Objects.requireNonNull(propertyName, "Property name cannot be null");
        // checkUnbound();

        PropertyDefinition<BEAN, ?> definition = propertySet.getProperty(propertyName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not resolve property name " + propertyName + " from " + propertySet));

        Optional<Class<PRESENTATION>> presentationTypeClass = getPresentationTypeForField(field);

        // PropertyDefinition does not return primitive type, so need to get it by field name
        Optional<Field> modelField = getDeclaredFieldByName(definition.getPropertyHolderType(), getTopLevelName(definition));
        Class<?> modelTypeClass = modelField.get().getType();

        Converter<PRESENTATION, ?> converter = null;
        if (presentationTypeClass.isPresent()) {
            converter = createConverter(presentationTypeClass.get(), modelTypeClass, field.getEmptyValue());
            if (converter == null) {
                if (presentationTypeClass.get().equals(String.class)) {
                    log.log(Level.INFO,
                            "Unable to find converter between presentationType=<{0}> and modelType=<{1}> for property=<{2}>, using read-only toString() converter",
                            new Object[]{presentationTypeClass.get(), modelTypeClass, propertyName});
                    converter = createToStringConverter();
                    readOnly = true;
                } else {
                    log.log(Level.SEVERE,
                            "Unable to find converter between presentationType=<{0}> and modelType=<{1}> for property=<{2}>. Please register a converter.",
                            new Object[]{presentationTypeClass.get(), modelTypeClass, propertyName});
                    throw new RuntimeException("No valid converter found for property=" + propertyName);
                }
            }
        } else {
            log.log(Level.WARNING,
                    "Unable to determine presentation type of field due to type-erasure. Fields requiring generic type arguments should either implement HasGenericType, be wrapped by EGTypeComponentAdapter or be subclassed to ensure type can be recovered. Using default assignment converter for modelType=<{0}>, property=<{1}>",
                    new Object[]{modelTypeClass, propertyName});

            converter = createCastConverter(modelTypeClass);
        }

        return bind(field, propertyName, converter, readOnly);
    }

    public <PRESENTATION, MODEL> EasyBinding<BEAN, PRESENTATION, MODEL> bind(HasValue<?, PRESENTATION> field,
                                                                             String propertyName, Converter<PRESENTATION, ?> converter) {
        return bind(field, propertyName, converter, false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <PRESENTATION, MODEL> EasyBinding<BEAN, PRESENTATION, MODEL> bind(HasValue<?, PRESENTATION> field,
                                                                             String propertyName, Converter<PRESENTATION, ?> converter, boolean readOnly) {
        Objects.requireNonNull(converter);
        Objects.requireNonNull(propertyName, "Property name cannot be null");
        // checkUnbound();

        PropertyDefinition<BEAN, ?> definition = propertySet.getProperty(propertyName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not resolve property name " + propertyName + " from " + propertySet));

        ValueProvider<BEAN, ?> getter = definition.getGetter();
        Setter<BEAN, ?> setter = readOnly ? null : definition.getSetter().orElse(null);

        EasyBinding<BEAN, PRESENTATION, MODEL> binding = bind(field, (ValueProvider) getter, (Setter) setter,
                propertyName, (Converter) converter);

        boundProperties.put(propertyName, binding);

        Optional<Field> modelField = getDeclaredFieldByName(definition.getPropertyHolderType(), getTopLevelName(definition));
        if (Arrays.asList(modelField.get().getAnnotations()).stream().anyMatch(new SerializablePredicate<Annotation>() {
            @Override
            public boolean test(Annotation annotation) {
                return requiredConfigurator.test(annotation, null);
            }
        })) {
            field.setRequiredIndicatorVisible(true);
        }

        return binding;
    }

    @SuppressWarnings("unchecked")
    protected <PRESENTATION, MODEL> Converter<PRESENTATION, MODEL> createConverter(Class<PRESENTATION> presentationType,
                                                                                   Class<MODEL> modelType, PRESENTATION emptyValue) {
        Objects.requireNonNull(presentationType);
        Objects.requireNonNull(modelType);

        Converter<PRESENTATION, MODEL> converter = converterRegistry.getConverter(presentationType, modelType);
        if (converter != null) {
            log.log(Level.INFO, "Converter for {0}->{1} found by lookup", new Object[]{presentationType, modelType});
        } else if (ReflectTools.convertPrimitiveType(presentationType)
                .equals(ReflectTools.convertPrimitiveType(modelType))) {
            if (modelType.isPrimitive()) {
                converter = (Converter<PRESENTATION, MODEL>) new NullConverterPrimitiveTarget<PRESENTATION>();
                log.log(Level.INFO, "Converter for primitive {0}->{1} found by identity",
                        new Object[]{presentationType, modelType});
            } else {
                converter = (Converter<PRESENTATION, MODEL>) new NullConverter<PRESENTATION>(emptyValue);
                log.log(Level.INFO, "Converter for non-primitive {0}->{1} found by identity",
                        new Object[]{presentationType, modelType});
            }
        }
        return converter;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <PRESENTATION, MODEL> Converter<PRESENTATION, MODEL> createCastConverter(Class<MODEL> propertyType) {
        Class<?> propertyTypeNonPrimitive = ReflectTools.convertPrimitiveType(propertyType);
        Converter converter = new Converter() {
            @Override
            public Result convertToModel(Object value, ValueContext context) {
                try {
                    Object cast = propertyTypeNonPrimitive.cast(value);
                } catch (Exception e) {
                    return Result.error(e.getMessage());
                }
                return Result.ok(value);
            }

            @Override
            public Object convertToPresentation(Object value, ValueContext context) {
                return value;
            }
        };
        return converter;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <PRESENTATION, MODEL> Converter<PRESENTATION, MODEL> createToStringConverter() {
        return (Converter) Converter.from(null, fieldValue -> fieldValue == null ? "" : fieldValue.toString());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <PRESENTATION> Optional<Class<PRESENTATION>> getPresentationTypeForField(HasValue<?, PRESENTATION> field) {
        // Unfortunately HasValue in Vaadin does not define a getType() method.

        // Try to find the field type using reflection. This will work for any fields
        // except fields with generic types.
        Type valueType = GenericTypeReflector.getTypeParameter(field.getClass(), HasValue.class.getTypeParameters()[0]);
        if (valueType != null) {
            if (valueType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) valueType;
                return Optional.of((Class<PRESENTATION>) pType.getRawType());
            }
            return Optional.of((Class<PRESENTATION>) valueType);
        }

        // Not possible to find using reflection (due to type erasure).
        // If field is an instance of HasGenericType
        if (field instanceof HasGenericType) {
            HasGenericType<PRESENTATION> type = (HasGenericType<PRESENTATION>) field;
            return Optional.of(type.getGenericType());
        }

        // The road to success is paved with dirty hacks....

        // If the field has a non null empty value we can fetch the type from this
        PRESENTATION emptyValue = field.getEmptyValue();
        if (emptyValue != null) {
            return Optional.of((Class<PRESENTATION>) emptyValue.getClass());
        }

        // If the field has a current value we can fetch the type from this
        PRESENTATION currentValue = field.getValue();
        if (currentValue != null) {
            return Optional.of((Class<PRESENTATION>) currentValue.getClass());
        }

        // If the field has items we can fetch the type from the first item
        if (field instanceof HasDataView) {
            HasDataView<PRESENTATION, ?, ?> hasDataView = (HasDataView<PRESENTATION, ?, ?>) field;
            PRESENTATION item = hasDataView.getGenericDataView().getItem(0);
            return Optional.of((Class<PRESENTATION>) item.getClass());

        }

        return Optional.empty();
    }

    protected Optional<Field> getDeclaredFieldByName(Class<?> searchClass, String name) {
        while (searchClass != null) {
            try {
                return Optional.of(searchClass.getDeclaredField(name));
            } catch (NoSuchFieldException | SecurityException e) {
                // No such field, try superclass
                searchClass = searchClass.getSuperclass();
            }
        }
        return Optional.empty();
    }

    /**
     * Gets field required indicator configuration logic.
     *
     * @return required indicator configurator, may be {@code null}
     * @see #setRequiredConfigurator(RequiredFieldConfigurator)
     */
    public RequiredFieldConfigurator getRequiredConfigurator() {
        return requiredConfigurator;
    }

    /**
     * Sets a logic which allows to configure require indicator via
     * {@link HasValue#setRequiredIndicatorVisible(boolean)} based on property
     * descriptor.
     * <p>
     * Required indicator configuration will not be used at all if
     * {@code configurator} is null.
     * <p>
     * By default the {@link RequiredFieldConfigurator#DEFAULT} configurator is
     * used.
     *
     * @param configurator required indicator configurator, may be {@code null}
     */
    public void setRequiredConfigurator(RequiredFieldConfigurator configurator) {
        requiredConfigurator = configurator;
    }

    @Override
    public Class<BEAN> getGenericType() {
        return clazz;
    }

    /*
    Workaround for broken backwards compatibility in Vaadin 8.3:
    The behavior of NestedBeanPropertyDefinition.getName() was changed in 8.3 instead a new method getTopLevelName() was introduced that
    provides the old "getName()" functionality. However since the getTopLevelName() was not introduced before 8.3 we have to provide out own
    implementation that works with all 8.x series of Vaadin
     */
    String getTopLevelName(PropertyDefinition<BEAN, ?> definition) {
        String propertyDefinitionName = definition.getName();
        if (definition instanceof BeanPropertySet.NestedBeanPropertyDefinition) {
            BeanPropertySet.NestedBeanPropertyDefinition<BEAN, ?> nbpd = (BeanPropertySet.NestedBeanPropertyDefinition<BEAN, ?>) definition;
            PropertyDefinition<BEAN, ?> parent = nbpd.getParent();
            if (parent != null) {
                String prefix = parent.getName() + ".";
                if (propertyDefinitionName.startsWith(prefix)) {
                    propertyDefinitionName = propertyDefinitionName.substring(prefix.length());
                }
            }
        }
        return propertyDefinitionName;
    }

}
