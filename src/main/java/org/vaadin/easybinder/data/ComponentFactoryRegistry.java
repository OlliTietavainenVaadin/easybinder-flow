/*
 * Copyright 2017 Lars Sønderby Jessen
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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.util.SharedUtil;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.vaadin.easybinder.ui.EComboBox;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;


public class
ComponentFactoryRegistry {
    private static ComponentFactoryRegistry instance;

    protected Logger log = Logger.getLogger(getClass().getName());

    protected Map<Class<?>, List<Pair<Predicate<Field>, Function<Field, Component>>>> builders = new HashMap<>();

    protected ComponentFactoryRegistry() {
        addBuildPattern(String.class, e -> true, e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(Integer.class, e -> true, e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(int.class, e -> true, e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(Long.class, e -> true, e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(long.class, e -> true, e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(Float.class, e -> true, e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(float.class, e -> true, e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(Double.class, e -> true, e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(double.class, e -> true, e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(BigInteger.class, e -> true,
                e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(BigDecimal.class, e -> true,
                e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(Character.class, e -> true,
                e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(char.class, e -> true, e -> new TextField(SharedUtil.camelCaseToHumanFriendly(e.getName())));

        addBuildPattern(Date.class,
                e -> Arrays.asList(e.getAnnotations()).stream().filter(f -> f instanceof Temporal)
                        .map(f -> (Temporal) f).filter(f -> f.value() == TemporalType.TIMESTAMP).findAny().isPresent(),
                e -> new DateTimePicker(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(Date.class,
                e -> !Arrays.asList(e.getAnnotations()).stream().filter(f -> f instanceof Temporal)
                        .map(f -> (Temporal) f).filter(f -> f.value() == TemporalType.TIMESTAMP).findAny().isPresent(),
                e -> new DatePicker(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(LocalDate.class, e -> true,
                e -> new DatePicker(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(LocalDateTime.class, e -> true,
                e -> new DateTimePicker(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(Boolean.class, e -> true, e -> new Checkbox(SharedUtil.camelCaseToHumanFriendly(e.getName())));
        addBuildPattern(boolean.class, e -> true, e -> new Checkbox(SharedUtil.camelCaseToHumanFriendly(e.getName())));

        addBuildPattern(Enum.class, e -> true, e -> {
            Class<?> clazz = e.getGenericType() != null ? (Class<?>) e.getGenericType() : e.getType();
            @SuppressWarnings({"rawtypes", "unchecked"})
            Component c = new EComboBox(clazz, SharedUtil.camelCaseToHumanFriendly(e.getName()),
                    Arrays.asList(e.getType().getEnumConstants()));
            return c;
        });

        addBuildPattern(Collection.class, e -> String.class.equals((Class<?>) GenericTypeReflector
                .getTypeParameter(e.getGenericType(), Collection.class.getTypeParameters()[0])), e -> {
            Grid<String> g = new Grid<String>();
            g.addColumn(f -> f);
            return g;
        });
        addBuildPattern(ArrayList.class, e -> String.class.equals((Class<?>) GenericTypeReflector
                .getTypeParameter(e.getGenericType(), ArrayList.class.getTypeParameters()[0])), e -> {
            Grid<String> g = new Grid<String>();
            g.addColumn(f -> f);
            return g;
        });
        addBuildPattern(List.class, e -> String.class.equals((Class<?>) GenericTypeReflector
                .getTypeParameter(e.getGenericType(), List.class.getTypeParameters()[0])), e -> {
            Grid<String> g = new Grid<String>();
            g.addColumn(f -> f);
            return g;
        });

        addBuildPattern(Map.class, e -> true, e -> {
            Grid<Map.Entry<?, ?>> g = new Grid<>();
            g.addColumn(f -> f.getKey());
            g.addColumn(f -> f.getValue());
            return g;
        });
    }

    public static ComponentFactoryRegistry getInstance() {
        if (instance == null) {
            instance = new ComponentFactoryRegistry();
        }

        return instance;
    }

    public void addBuildPattern(Class<?> propertyType, Predicate<Field> matcher,
                                Function<Field, Component> componentFactory) {
        List<Pair<Predicate<Field>, Function<Field, Component>>> lst = builders.get(propertyType);
        if (lst == null) {
            lst = new LinkedList<>();
            builders.put(propertyType, lst);
        }
        lst.add(new Pair<>(matcher, componentFactory));
    }

    public Optional<Component> createComponent(Field field) {
        Class<?> classToTest = field.getType();
        while (classToTest != null) {
            List<Pair<Predicate<Field>, Function<Field, Component>>> candidates = builders.get(classToTest);
            if (candidates != null) {
                Optional<Pair<Predicate<Field>, Function<Field, Component>>> match = candidates.stream()
                        .filter(e -> e.getFirst().test(field)).findFirst();
                if (match.isPresent()) {
                    log.log(Level.INFO, "Fould build rule for field=<{0}> using type={1}",
                            new Object[]{field, classToTest});
                    return Optional.of(match.get().getSecond().apply(field));
                }
            }

            log.log(Level.INFO, "No build rule for field=<{0}> with type=<{1}>", new Object[]{field, classToTest});
            classToTest = classToTest.getSuperclass();
        }

        log.log(Level.INFO, "No match for field=<{1}> with type=<{2}>, generating a text field",
                new Object[]{field, field.getType()});
        return Optional.of(new TextField(SharedUtil.camelCaseToHumanFriendly(field.getName())));
    }
}
