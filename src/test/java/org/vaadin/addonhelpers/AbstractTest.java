package org.vaadin.addonhelpers;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public abstract class AbstractTest extends Div {

    protected VerticalLayout content;

    public AbstractTest() {
        content = new VerticalLayout();
        add(content);
    }

    protected void setup() {
        Component map = getTestComponent();
        setContentSize(content);
        content.add(map);
        content.setFlexGrow(1,map);
    }

    /**
     * Sets the size of the content. Override to change from
     * {@link Sizeable#setSizeFull()}
     *
     * @param content
     */
    public void setContentSize(HasSize content) {
        content.setSizeFull();
    }

    public abstract Component getTestComponent();


}
