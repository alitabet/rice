/**
 * Copyright 2005-2013 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl2.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.rice.krad.uif.container;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.kuali.rice.krad.datadictionary.parse.BeanTag;
import org.kuali.rice.krad.datadictionary.parse.BeanTagAttribute;
import org.kuali.rice.krad.datadictionary.parse.BeanTags;
import org.kuali.rice.krad.datadictionary.validator.ValidationTrace;
import org.kuali.rice.krad.datadictionary.validator.Validator;
import org.kuali.rice.krad.uif.component.Component;
import org.kuali.rice.krad.uif.component.DataBinding;
import org.kuali.rice.krad.uif.component.DelayedCopy;
import org.kuali.rice.krad.uif.field.Field;
import org.kuali.rice.krad.uif.field.FieldGroup;
import org.kuali.rice.krad.uif.util.ComponentUtils;
import org.kuali.rice.krad.uif.util.LifecycleAwareList;
import org.kuali.rice.krad.uif.widget.Disclosure;
import org.kuali.rice.krad.uif.widget.Scrollpane;

/**
 * Container that holds a list of <code>Field</code> or other <code>Group</code>
 * instances
 *
 * <p>
 * Groups can exist at different levels of the <code>View</code>, providing
 * conceptual groupings such as the page, section, and group. In addition, other
 * group types can be created to add behavior like collection support
 * </p>
 *
 * <p>
 * <code>Group</code> implementation has properties for defaulting the binding
 * information (such as the parent object path and a binding prefix) for the
 * fields it contains. During the phase these properties (if given) are set on
 * the fields contained in the <code>Group</code> that implement
 * <code>DataBinding</code>, unless they have already been set on the field.
 * </p>
 *
 * @author Kuali Rice Team (rice.collab@kuali.org)
 */
@BeanTags({@BeanTag(name = "group-bean", parent = "Uif-GroupBase"),
        @BeanTag(name = "boxGroupBase-bean", parent = "Uif-BoxGroupBase"),
        @BeanTag(name = "verticalBoxGroup-bean", parent = "Uif-VerticalBoxGroup"),
        @BeanTag(name = "verticalBoxSection-bean", parent = "Uif-VerticalBoxSection"),
        @BeanTag(name = "verticalBoxSubSection-bean", parent = "Uif-VerticalBoxSubSection"),
        @BeanTag(name = "disclosure-verticalBoxSection-bean", parent = "Uif-Disclosure-VerticalBoxSection"),
        @BeanTag(name = "disclosure-verticalBoxSubSection-bean", parent = "Uif-Disclosure-VerticalBoxSubSection"),
        @BeanTag(name = "horizontalBoxGroup-bean", parent = "Uif-HorizontalBoxGroup"),
        @BeanTag(name = "horizontalBoxSection-bean", parent = "Uif-HorizontalBoxSection"),
        @BeanTag(name = "horizontalBoxSubSection-bean", parent = "Uif-HorizontalBoxSubSection"),
        @BeanTag(name = "disclosure-horizontalBoxSection-bean", parent = "Uif-Disclosure-HorizontalBoxSection"),
        @BeanTag(name = "disclosure-horizontalBoxSubSection-bean", parent = "Uif-Disclosure-HorizontalBoxSubSection"),
        @BeanTag(name = "gridGroup-bean", parent = "Uif-GridGroup"),
        @BeanTag(name = "gridSection-bean", parent = "Uif-GridSection"),
        @BeanTag(name = "gridSubSection-bean", parent = "Uif-GridSubSection"),
        @BeanTag(name = "disclosure-gridSection-bean", parent = "Uif-Disclosure-GridSection"),
        @BeanTag(name = "cssGridGroup-bean", parent = "Uif-CssGridGroup"),
        @BeanTag(name = "cssGridSection-bean", parent = "Uif-CssGridSection"),
        @BeanTag(name = "cssGridSubSection-bean", parent = "Uif-CssGridSubSection"),
        @BeanTag(name = "cssGridSection-1FieldLabelColumn-bean", parent = "Uif-CssGridSection-1FieldLabelColumn"),
        @BeanTag(name = "cssGridSection-2FieldLabelColumn-bean", parent = "Uif-CssGridSection-2FieldLabelColumn"),
        @BeanTag(name = "cssGridSection-3FieldLabelColumn-bean", parent = "Uif-CssGridSection-3FieldLabelColumn"),
        @BeanTag(name = "cssGridSubSection-1FieldLabelColumn-bean", parent = "Uif-CssGridSubSection-1FieldLabelColumn"),
        @BeanTag(name = "cssGridSubSection-2FieldLabelColumn-bean", parent = "Uif-CssGridSubSection-2FieldLabelColumn"),
        @BeanTag(name = "cssGridSubSection-3FieldLabelColumn-bean", parent = "Uif-CssGridSubSection-3FieldLabelColumn"),
        @BeanTag(name = "listGroup-bean", parent = "Uif-ListGroup"),
        @BeanTag(name = "listSection-bean", parent = "Uif-ListSection"),
        @BeanTag(name = "listSubSection-bean", parent = "Uif-ListSubSection"),
        @BeanTag(name = "disclosure-listSection-bean", parent = "Uif-Disclosure-ListSection"),
        @BeanTag(name = "disclosure-listSubSection-bean", parent = "Uif-Disclosure-ListSubSection"),
        @BeanTag(name = "collectionGridItem-bean", parent = "Uif-CollectionGridItem"),
        @BeanTag(name = "collectionVerticalBoxItem-bean", parent = "Uif-CollectionVerticalBoxItem"),
        @BeanTag(name = "collectionHorizontalBoxItem-bean", parent = "Uif-CollectionHorizontalBoxItem"),
        @BeanTag(name = "headerUpperGroup-bean", parent = "Uif-HeaderUpperGroup"),
        @BeanTag(name = "headerRightGroup-bean", parent = "Uif-HeaderRightGroup"),
        @BeanTag(name = "headerLowerGroup-bean", parent = "Uif-HeaderLowerGroup"),
        @BeanTag(name = "footer-bean", parent = "Uif-FooterBase"),
        @BeanTag(name = "formFooter-bean", parent = "Uif-FormFooter"),
        @BeanTag(name = "actionsGroup-bean", parent = "Uif-ActionsGroup"),
        @BeanTag(name = "disclosureActionsGroup-bean", parent = "Uif-DisclosureActionsGroup"),
        @BeanTag(name = "disclosureActions-reqMessageGroup-bean", parent = "Uif-DisclosureActions-ReqMessageGroup"),
        @BeanTag(name = "inactiveItemsActionsGroup-bean", parent = "Uif-InactiveItemsActionsGroup"),
        @BeanTag(name = "documentInfoGroup-bean", parent = "Uif-DocumentInfoGroup"),
        @BeanTag(name = "documentOverviewSection-bean", parent = "Uif-DocumentOverviewSection"),
        @BeanTag(name = "documentAdHocRecipientsSection-bean", parent = "Uif-DocumentAdHocRecipientsSection"),
        @BeanTag(name = "documentRouteLogSection-bean", parent = "Uif-DocumentRouteLogSection"),
        @BeanTag(name = "documentPageFooter-bean", parent = "Uif-DocumentPageFooter"),
        @BeanTag(name = "incidentDetailGroup-bean", parent = "Uif-IncidentDetailGroup"),
        @BeanTag(name = "incidentStackTraceGroup-bean", parent = "Uif-IncidentStackTraceGroup"),
        @BeanTag(name = "incidentReportFooter-bean", parent = "Uif-IncidentReportFooter"),
        @BeanTag(name = "initiatedDocumentFooter-bean", parent = "InitiatedDocumentFooter"),
        @BeanTag(name = "inquiryFooter-bean", parent = "Uif-InquiryFooter"),
        @BeanTag(name = "lookupCriteriaGroup-bean", parent = "Uif-LookupCriteriaGroup"),
        @BeanTag(name = "lookupPageHeaderGroup-bean", parent = "Uif-LookupPageHeaderGroup"),
        @BeanTag(name = "lookupCriteriaFooter-bean", parent = "Uif-LookupCriteriaFooter"),
        @BeanTag(name = "lookupResultsFooter-bean", parent = "Uif-LookupResultsFooter"),
        @BeanTag(name = "maintenanceGridGroup-bean", parent = "Uif-MaintenanceGridGroup"),
        @BeanTag(name = "maintenanceHorizontalBoxGroup-bean", parent = "Uif-MaintenanceHorizontalBoxGroup"),
        @BeanTag(name = "maintenanceVerticalBoxGroup-bean", parent = "Uif-MaintenanceVerticalBoxGroup"),
        @BeanTag(name = "maintenanceGridSection-bean", parent = "Uif-MaintenanceGridSection"),
        @BeanTag(name = "maintenanceGridSubSection-bean", parent = "Uif-MaintenanceGridSubSection"),
        @BeanTag(name = "maintenanceHorizontalBoxSection-bean", parent = "Uif-MaintenanceHorizontalBoxSection"),
        @BeanTag(name = "maintenanceVerticalBoxSection-bean", parent = "Uif-MaintenanceVerticalBoxSection"),
        @BeanTag(name = "maintenanceHorizontalBoxSubSection-bean", parent = "Uif-MaintenanceHorizontalBoxSubSection"),
        @BeanTag(name = "maintenanceVerticalBoxSubSection-bean", parent = "Uif-MaintenanceVerticalBoxSubSection")})
public class GroupBase extends ContainerBase implements Group {
    private static final long serialVersionUID = 7953641325356535509L;

    private String fieldBindByNamePrefix;
    private String fieldBindingObjectPath;

    @DelayedCopy
    private Disclosure disclosure;
    private Scrollpane scrollpane;

    private List<? extends Component> items;

    /**
     * Default Constructor
     */
    public GroupBase() {
        items = Collections.emptyList();
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#performInitialization(java.lang.Object)
     */
    @Override
    public void performInitialization(Object model) {
        super.performInitialization(model);

        for (Component component : getItems()) {
            // append group's field bind by name prefix (if set) to each
            // attribute field's binding prefix
            if (component instanceof DataBinding) {
                DataBinding dataBinding = (DataBinding) component;

                if (StringUtils.isNotBlank(getFieldBindByNamePrefix())) {
                    String bindByNamePrefixToSet = getFieldBindByNamePrefix();

                    if (StringUtils.isNotBlank(dataBinding.getBindingInfo().getBindByNamePrefix())) {
                        bindByNamePrefixToSet += "." + dataBinding.getBindingInfo().getBindByNamePrefix();
                    }
                    dataBinding.getBindingInfo().setBindByNamePrefix(bindByNamePrefixToSet);
                }

                if (StringUtils.isNotBlank(fieldBindingObjectPath) && StringUtils.isBlank(
                        dataBinding.getBindingInfo().getBindingObjectPath())) {
                    dataBinding.getBindingInfo().setBindingObjectPath(fieldBindingObjectPath);
                }
            }
            // set on FieldGroup's group to recursively set AttributeFields
            else if (component instanceof FieldGroup) {
                FieldGroup fieldGroup = (FieldGroup) component;

                if (fieldGroup.getGroup() != null) {
                    if (StringUtils.isBlank(fieldGroup.getGroup().getFieldBindByNamePrefix())) {
                        fieldGroup.getGroup().setFieldBindByNamePrefix(fieldBindByNamePrefix);
                    }
                    if (StringUtils.isBlank(fieldGroup.getGroup().getFieldBindingObjectPath())) {
                        fieldGroup.getGroup().setFieldBindingObjectPath(fieldBindingObjectPath);
                    }
                }
            } else if (component instanceof Group) {
                Group subGroup = (Group) component;
                if (StringUtils.isNotBlank(getFieldBindByNamePrefix())) {
                    if (StringUtils.isNotBlank(subGroup.getFieldBindByNamePrefix())) {
                        subGroup.setFieldBindByNamePrefix(
                                getFieldBindByNamePrefix() + "." + subGroup.getFieldBindByNamePrefix());
                    } else {
                        subGroup.setFieldBindByNamePrefix(getFieldBindByNamePrefix());
                    }
                }
                if (StringUtils.isNotBlank(getFieldBindingObjectPath())) {
                    if (StringUtils.isNotBlank(subGroup.getFieldBindingObjectPath())) {
                        subGroup.setFieldBindingObjectPath(
                                getFieldBindingObjectPath() + "." + subGroup.getFieldBindingObjectPath());
                    } else {
                        subGroup.setFieldBindingObjectPath(getFieldBindingObjectPath());
                    }
                }
            }
        }
        
        if (isReadOnly()) {
            setValidationMessages(null);
            if (getHeader() != null) {
                getHeader().setReadOnly(true);
            }
            if (getFooter() != null) {
                getFooter().setReadOnly(true);
            }
            
            List<? extends Component> items = getItems();
            if (items != null) {
                for (Component item : items) {
                    if (item != null) {
                        item.setReadOnly(true);
                    }
                }
            }
        }
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#getSupportedComponents()
     */
    @Override
    public Set<Class<? extends Component>> getSupportedComponents() {
        Set<Class<? extends Component>> supportedComponents = new HashSet<Class<? extends Component>>();
        supportedComponents.add(Field.class);
        supportedComponents.add(Group.class);

        return supportedComponents;
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#getComponentTypeName()
     */
    @Override
    public String getComponentTypeName() {
        return "group";
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#getFieldBindByNamePrefix()
     */
    @Override
    @BeanTagAttribute(name = "fieldBindByNamePrefix")
    public String getFieldBindByNamePrefix() {
        return this.fieldBindByNamePrefix;
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#setFieldBindByNamePrefix(java.lang.String)
     */
    @Override
    public void setFieldBindByNamePrefix(String fieldBindByNamePrefix) {
        this.fieldBindByNamePrefix = fieldBindByNamePrefix;
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#getFieldBindingObjectPath()
     */
    @Override
    @BeanTagAttribute(name = "fieldBindingObjectPath")
    public String getFieldBindingObjectPath() {
        return this.fieldBindingObjectPath;
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#setFieldBindingObjectPath(java.lang.String)
     */
    @Override
    public void setFieldBindingObjectPath(String fieldBindingObjectPath) {
        this.fieldBindingObjectPath = fieldBindingObjectPath;
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#getDisclosure()
     */
    @Override
    @BeanTagAttribute(name = "Disclosure", type = BeanTagAttribute.AttributeType.SINGLEBEAN)
    public Disclosure getDisclosure() {
        return this.disclosure;
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#setDisclosure(org.kuali.rice.krad.uif.widget.Disclosure)
     */
    @Override
    public void setDisclosure(Disclosure disclosure) {
        this.disclosure = disclosure;
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#getScrollpane()
     */
    @Override
    @BeanTagAttribute(name = "scrollpane", type = BeanTagAttribute.AttributeType.SINGLEBEAN)
    public Scrollpane getScrollpane() {
        return this.scrollpane;
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#setScrollpane(org.kuali.rice.krad.uif.widget.Scrollpane)
     */
    @Override
    public void setScrollpane(Scrollpane scrollpane) {
        this.scrollpane = scrollpane;
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#getItems()
     */
    @BeanTagAttribute(name = "items", type = BeanTagAttribute.AttributeType.LISTBEAN)
    public List<? extends Component> getItems() {
        if (items == Collections.EMPTY_LIST && isMutable(true)) {
            items = new LifecycleAwareList<Component>(this);
        }

        return this.items;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setItems(List<? extends Component> items) {
        if (items == null) {
            this.items = Collections.emptyList();
        } else if (items.contains(this)) {
            throw new IllegalArgumentException("Attempted to add group to itself");
        } else {
            this.items = new LifecycleAwareList<Component>(this, (List<Component>) items);
        }
    }

    /**
     * @see org.kuali.rice.krad.datadictionary.DictionaryBeanBase#copyProperties(Object)
     */
    @Override
    protected <T> void copyProperties(T component) {
        super.copyProperties(component);

        GroupBase groupCopy = (GroupBase) component;

        groupCopy.setFieldBindByNamePrefix(this.fieldBindByNamePrefix);
        groupCopy.setFieldBindingObjectPath(this.fieldBindingObjectPath);

        if (this.disclosure != null) {
            groupCopy.setDisclosure((Disclosure) this.disclosure.copy());
        }

        if (this.scrollpane != null) {
            groupCopy.setScrollpane((Scrollpane) this.scrollpane.copy());
        }

        if (this.items != null) {
            List<Component> itemsCopy = ComponentUtils.copy(new ArrayList<Component>(items));
            groupCopy.setItems(itemsCopy);
        }
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#completeValidation(org.kuali.rice.krad.datadictionary.validator.ValidationTrace)
     */
    @Override
    public void completeValidation(ValidationTrace tracer) {
        tracer.addBean(this);

        // Checks that no invalid items are present
        for (int i = 0; i < getItems().size(); i++) {
            if (getItems().get(i).getClass() == PageGroup.class || getItems().get(i).getClass()
                    == NavigationGroup.class) {
                String currentValues[] = {"item(" + i + ").class =" + getItems().get(i).getClass()};
                tracer.createError("Items in Group cannot be PageGroup or NaviagtionGroup", currentValues);
            }
        }

        // Checks that the layout manager is set
        if (getLayoutManager() == null) {
            if (Validator.checkExpressions(this, "layoutManager")) {
                String currentValues[] = {"layoutManager = " + getLayoutManager()};
                tracer.createError("LayoutManager must be set", currentValues);
            }
        }

        super.completeValidation(tracer.getCopy());
    }

    /**
     * This overridden method ...
     * 
     * @see org.kuali.rice.krad.uif.container.Group2#isRenderLoading()
     */
    @Override
    public boolean isRenderLoading() {
        return disclosure != null && disclosure.isAjaxRetrievalWhenOpened() && (!disclosure.isRender() || !disclosure
                .isDefaultOpen());
    }

}