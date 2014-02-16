/**
 * Copyright 2005-2014 The Kuali Foundation
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
package org.kuali.rice.krad.uif.layout;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kuali.rice.krad.datadictionary.parse.BeanTag;
import org.kuali.rice.krad.datadictionary.parse.BeanTagAttribute;
import org.kuali.rice.krad.datadictionary.parse.BeanTags;
import org.kuali.rice.krad.uif.UifPropertyPaths;
import org.kuali.rice.krad.uif.component.Component;
import org.kuali.rice.krad.uif.component.DataBinding;
import org.kuali.rice.krad.uif.component.KeepExpression;
import org.kuali.rice.krad.uif.container.CollectionGroup;
import org.kuali.rice.krad.uif.container.Container;
import org.kuali.rice.krad.uif.container.Group;
import org.kuali.rice.krad.uif.element.Action;
import org.kuali.rice.krad.uif.element.Message;
import org.kuali.rice.krad.uif.field.Field;
import org.kuali.rice.krad.uif.field.FieldGroup;
import org.kuali.rice.krad.uif.lifecycle.ViewLifecycle;
import org.kuali.rice.krad.uif.lifecycle.ViewLifecycleUtils;
import org.kuali.rice.krad.uif.util.ComponentUtils;
import org.kuali.rice.krad.uif.util.LifecycleElement;
import org.kuali.rice.krad.uif.util.ObjectPropertyUtils;
import org.kuali.rice.krad.uif.widget.Pager;
import org.kuali.rice.krad.web.form.UifFormBase;

/**
 * Layout manager that works with {@code CollectionGroup} containers and
 * renders the collection lines in a vertical row
 *
 * <p>
 * For each line of the collection, a {@code Group} instance is created.
 * The group header contains a label for the line (summary information), the
 * group fields are the collection line fields, and the group footer contains
 * the line actions. All the groups are rendered using the
 * {@code BoxLayoutManager} with vertical orientation.
 * </p>
 *
 * <p>
 * Modify the lineGroupPrototype to change header/footer styles or any other
 * customization for the line groups
 * </p>
 *
 * @author Kuali Rice Team (rice.collab@kuali.org)
 */
@BeanTags({@BeanTag(name = "stackedCollectionLayout-bean", parent = "Uif-StackedCollectionLayoutBase"),
        @BeanTag(name = "stackedCollectionLayout-withGridItems-bean",
                parent = "Uif-StackedCollectionLayout-WithGridItems"),
        @BeanTag(name = "stackedCollectionLayout-withBoxItems-bean",
                parent = "Uif-StackedCollectionLayout-WithBoxItems"),
        @BeanTag(name = "stackedCollectionLayout-list-bean", parent = "Uif-StackedCollectionLayout-List")})
public class StackedLayoutManagerBase extends LayoutManagerBase implements StackedLayoutManager {
    private static final long serialVersionUID = 4602368505430238846L;

    @KeepExpression
    private String summaryTitle;
    private List<String> summaryFields;

    private Group addLineGroup;
    private Group lineGroupPrototype;
    private FieldGroup subCollectionFieldGroupPrototype;
    private Field selectFieldPrototype;
    private Group wrapperGroup;
    private Pager pagerWidget;

    private List<Group> stackedGroups;

    private boolean actionsInLineGroup;

    public StackedLayoutManagerBase() {
        super();

        summaryFields = new ArrayList<String>();
        stackedGroups = new ArrayList<Group>();
    }

    /**
     * The following actions are performed:
     *
     * <ul>
     * <li>Initializes the prototypes</li>
     * </ul>
     *
     * {@inheritDoc}
     */
    @Override
    public void performInitialization(Object model) {
        super.performInitialization(model);

        stackedGroups = new ArrayList<Group>();
    }

    /**
     * The following actions are performed:
     *
     * <ul>
     * <li>If wrapper group is specified, places the stacked groups into the wrapper</li>
     * </ul>
     *
     * {@inheritDoc}
     */
    @Override
    public void performApplyModel(Object model, LifecycleElement component) {
        super.performApplyModel(model, component);

        if (wrapperGroup != null) {
            wrapperGroup.setItems(stackedGroups);
        }
    }

    /**
     * Calculates the values that must be pased into the pagerWidget if using paging
     *
     * {@inheritDoc}
     */
    @Override
    public void performFinalize(Object model, LifecycleElement element) {
        super.performFinalize(model, element);
        
        Component parent = ViewLifecycle.getPhase().getParent();

        // Calculate the number of pages for the pager widget if we are using server paging
        if (parent instanceof CollectionGroup
                && ((CollectionGroup) parent).isUseServerPaging()
                && this.getPagerWidget() != null) {
            CollectionGroup collectionGroup = (CollectionGroup) parent;

            // Set the appropriate page, total pages, and link script into the Pager
            CollectionLayoutUtils.setupPagerWidget(pagerWidget, collectionGroup, model);
        }
    }

    /**
     * Builds a {@code Group} instance for a collection line. The group is
     * built by first creating a copy of the configured prototype. Then the
     * header for the group is created using the configured summary fields on
     * the {@code CollectionGroup}. The line fields passed in are set as
     * the items for the group, and finally the actions are placed into the
     * group footer
     *
     * @see CollectionLayoutManager#buildLine(org.kuali.rice.krad.uif.view.View,
     *      Object, org.kuali.rice.krad.uif.container.CollectionGroup,
     *      java.util.List, java.util.List, String, java.util.List,
     *      String, Object, int)
     */
    public void buildLine(Object model, CollectionGroup collectionGroup, List<Field> lineFields,
            List<FieldGroup> subCollectionFields, String bindingPath, List<? extends Component> actions, String idSuffix,
            Object currentLine, int lineIndex) {
        boolean isAddLine = lineIndex == -1;

        // construct new group
        Group lineGroup = null;
        if (isAddLine) {
            stackedGroups = new ArrayList<Group>();

            if (addLineGroup == null) {
                lineGroup = ComponentUtils.copy(lineGroupPrototype, idSuffix);
            } else {
                lineGroup = ComponentUtils.copy(getAddLineGroup(), idSuffix);
                lineGroup.addStyleClass(collectionGroup.getAddItemCssClass());
            }

            if (collectionGroup.isAddViaLightBox()) {
                String actionScript = "showLightboxComponent('" + lineGroup.getId() + "');";
                if (StringUtils.isNotBlank(collectionGroup.getAddViaLightBoxAction().getActionScript())) {
                    actionScript = collectionGroup.getAddViaLightBoxAction().getActionScript() + actionScript;
                }
                collectionGroup.getAddViaLightBoxAction().setActionScript(actionScript);
                lineGroup.setStyle("display: none");
            }
        } else {
            lineGroup = ComponentUtils.copy(lineGroupPrototype, idSuffix);
        }

        if (((UifFormBase) model).isAddedCollectionItem(currentLine)) {
            lineGroup.addStyleClass(collectionGroup.getNewItemsCssClass());
        }

        // any actions that are attached to the group prototype (like the header) need to get action parameters
        // and context set for the collection line
        List<Action> lineGroupActions = ViewLifecycleUtils.getElementsOfTypeDeep(lineGroup, Action.class);
        if (lineGroupActions != null) {
            collectionGroup.getCollectionGroupBuilder().initializeActions(lineGroupActions, collectionGroup, lineIndex);
            ComponentUtils.updateContextsForLine(lineGroupActions, currentLine, lineIndex, idSuffix);
        }

        ComponentUtils.updateContextForLine(lineGroup, currentLine, lineIndex, idSuffix);

        // build header for the group
        if (isAddLine) {
            if (lineGroup.getHeader() != null) {
                Message headerMessage = ComponentUtils.copy(collectionGroup.getAddLineLabel());
                lineGroup.getHeader().setRichHeaderMessage(headerMessage);
            }
        } else {
            // get the collection for this group from the model
            List<Object> modelCollection = ObjectPropertyUtils.getPropertyValue(model,
                    ((DataBinding) collectionGroup).getBindingInfo().getBindingPath());

            String headerText = buildLineHeaderText(modelCollection.get(lineIndex), lineGroup);

            // don't set header if text is blank (could already be set by other means)
            if (StringUtils.isNotBlank(headerText) && lineGroup.getHeader() != null) {
                lineGroup.getHeader().setHeaderText(headerText);
            }
        }

        // stack all fields (including sub-collections) for the group
        List<Component> groupFields = new ArrayList<Component>();
        groupFields.addAll(lineFields);
        groupFields.addAll(subCollectionFields);

        // set line actions on group footer
        if (collectionGroup.isRenderLineActions() && !collectionGroup.isReadOnly() && (lineGroup.getFooter() != null)) {
            // add the actions to the line group if isActionsInLineGroup flag is true
            if (isActionsInLineGroup()) {
                groupFields.addAll(actions);
                lineGroup.setRenderFooter(false);
            } else {
                lineGroup.getFooter().setItems(actions);
            }
        }

        lineGroup.setItems(groupFields);

        stackedGroups.add(lineGroup);
    }

    /**
     * Builds the header text for the collection line
     *
     * <p>
     * Header text is built up by first the collection label, either specified
     * on the collection definition or retrieved from the dictionary. Then for
     * each summary field defined, the value from the model is retrieved and
     * added to the header.
     * </p>
     *
     * <p>
     * Note the {@link #getSummaryTitle()} field may have expressions defined, in which cause it will be copied to the
     * property expressions map to set the title for the line group (which will have the item context variable set)
     * </p>
     *
     * @param line Collection line containing data
     * @param lineGroup Group instance for rendering the line and whose title should be built
     * @return header text for line
     */
    protected String buildLineHeaderText(Object line, Group lineGroup) {
        // check for expression on summary title
        if (ViewLifecycle.getExpressionEvaluator().containsElPlaceholder(summaryTitle)) {
            lineGroup.getPropertyExpressions().put(UifPropertyPaths.HEADER_TEXT, summaryTitle);
            return null;
        }

        // build up line summary from declared field values and fixed title
        String summaryFieldString = "";
        for (String summaryField : summaryFields) {
            Object summaryFieldValue = ObjectPropertyUtils.getPropertyValue(line, summaryField);
            if (StringUtils.isNotBlank(summaryFieldString)) {
                summaryFieldString += " - ";
            }

            if (summaryFieldValue != null) {
                summaryFieldString += summaryFieldValue;
            } else {
                summaryFieldString += "Null";
            }
        }

        String headerText = summaryTitle;
        if (StringUtils.isNotBlank(summaryFieldString)) {
            headerText += " ( " + summaryFieldString + " )";
        }

        return headerText;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends Container> getSupportedContainer() {
        return CollectionGroup.class;
    }

    /**
     * Text to appears in the header for each collection lines Group. Used in
     * conjunction with {@link #getSummaryFields()} to build up the final header
     * text
     *
     * @return summary title text
     */
    @Override
    @BeanTagAttribute(name = "summaryTitle")
    public String getSummaryTitle() {
        return this.summaryTitle;
    }

    /**
     * Setter for the summary title text
     *
     * @param summaryTitle
     */
    public void setSummaryTitle(String summaryTitle) {
        this.summaryTitle = summaryTitle;
    }

    /**
     * List of attribute names from the collection line class that should be
     * used to build the line summary. To build the summary the value for each
     * attribute is retrieved from the line instance. All the values are then
     * placed together with a separator.
     *
     * @return summary field names
     * @see #buildLineHeaderText(Object, org.kuali.rice.krad.uif.container.Group)
     */
    @BeanTagAttribute(name = "summaryFields", type = BeanTagAttribute.AttributeType.LISTVALUE)
    public List<String> getSummaryFields() {
        return this.summaryFields;
    }

    /**
     * Setter for the summary field name list
     *
     * @param summaryFields
     */
    public void setSummaryFields(List<String> summaryFields) {
        this.summaryFields = summaryFields;
    }

    /**
     * Group instance that will be used for the add line
     *
     * <p>
     * Add line fields and actions configured on the
     * {@code CollectionGroup} will be set onto the add line group (if add
     * line is enabled). If the add line group is not configured, a new instance
     * of the line group prototype will be used for the add line.
     * </p>
     *
     * @return add line group instance
     * @see #getAddLineGroup()
     */
    @BeanTagAttribute(name = "addLineGroup", type = BeanTagAttribute.AttributeType.SINGLEBEAN)
    public Group getAddLineGroup() {
        return this.addLineGroup;
    }

    /**
     * Setter for the add line group
     *
     * @param addLineGroup
     */
    public void setAddLineGroup(Group addLineGroup) {
        this.addLineGroup = addLineGroup;
    }

    /**
     * Group instance that is used as a prototype for creating the collection
     * line groups. For each line a copy of the prototype is made and then
     * adjusted as necessary
     *
     * @return Group instance to use as prototype
     */
    @BeanTagAttribute(name = "lineGroupPrototype", type = BeanTagAttribute.AttributeType.SINGLEBEAN)
    public Group getLineGroupPrototype() {
        return this.lineGroupPrototype;
    }

    /**
     * Setter for the line group prototype
     *
     * @param lineGroupPrototype
     */
    public void setLineGroupPrototype(Group lineGroupPrototype) {
        this.lineGroupPrototype = lineGroupPrototype;
    }

    /**
     * {@inheritDoc}
     */
    @BeanTagAttribute(name = "subCollectionFieldGroupPrototype", type = BeanTagAttribute.AttributeType.SINGLEBEAN)
    public FieldGroup getSubCollectionFieldGroupPrototype() {
        return this.subCollectionFieldGroupPrototype;
    }

    /**
     * Setter for the sub-collection field group prototype
     *
     * @param subCollectionFieldGroupPrototype
     */
    public void setSubCollectionFieldGroupPrototype(FieldGroup subCollectionFieldGroupPrototype) {
        this.subCollectionFieldGroupPrototype = subCollectionFieldGroupPrototype;
    }

    /**
     * Field instance that serves as a prototype for creating the select field on each line when
     * {@link org.kuali.rice.krad.uif.container.CollectionGroup#isIncludeLineSelectionField()} is true
     *
     * <p>
     * This prototype can be used to set the control used for the select field (generally will be a checkbox control)
     * in addition to styling and other setting. The binding path will be formed with using the
     * {@link org.kuali.rice.krad.uif.container.CollectionGroup#getLineSelectPropertyName()} or if not set the
     * framework
     * will use {@link org.kuali.rice.krad.web.form.UifFormBase#getSelectedCollectionLines()}
     * </p>
     *
     * @return select field prototype instance
     */
    @BeanTagAttribute(name = "selectFieldPrototype", type = BeanTagAttribute.AttributeType.SINGLEBEAN)
    public Field getSelectFieldPrototype() {
        return selectFieldPrototype;
    }

    /**
     * Setter for the prototype instance for select fields
     *
     * @param selectFieldPrototype
     */
    public void setSelectFieldPrototype(Field selectFieldPrototype) {
        this.selectFieldPrototype = selectFieldPrototype;
    }

    /**
     * Group that will 'wrap' the generated collection lines so that they have a different layout from the general
     * stacked layout
     *
     * <p>
     * By default (when the wrapper group is null), each collection line will become a group and the groups are
     * rendered one after another. If the wrapper group is configured, the generated groups will be inserted as the
     * items for the wrapper group, and the layout manager configured for the wrapper group will determine how they
     * are rendered. For example, the layout manager could be a grid layout configured for three columns, which would
     * layout the first three lines horizontally then break to a new row.
     * </p>
     *
     * @return Group instance whose items list should be populated with the generated groups, or null to use the
     *         default layout
     */
    @BeanTagAttribute(name = "wrapperGroup", type = BeanTagAttribute.AttributeType.SINGLEBEAN)
    public Group getWrapperGroup() {
        return wrapperGroup;
    }

    /**
     * Setter for the wrapper group that will receive the generated line groups
     *
     * @param wrapperGroup
     */
    public void setWrapperGroup(Group wrapperGroup) {
        this.wrapperGroup = wrapperGroup;
    }

    /**
     * The pagerWidget used for paging when the StackedLayout is using paging
     *
     * @return the pagerWidget
     */
    public Pager getPagerWidget() {
        return pagerWidget;
    }

    /**
     * Set the pagerWidget used for paging StackedLayouts
     *
     * @param pagerWidget
     */
    public void setPagerWidget(Pager pagerWidget) {
        this.pagerWidget = pagerWidget;
    }

    /**
     * Final {@code List} of Groups to render for the collection
     *
     * @return collection groups
     */
    @BeanTagAttribute(name = "stackedGroups", type = BeanTagAttribute.AttributeType.LISTBEAN)
    public List<Group> getStackedGroups() {
        return this.stackedGroups;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStackedGroups(List<Group> stackedGroups) {
        this.stackedGroups = stackedGroups;
    }

    /**
     * Flag that indicates whether actions will be added in the same group as the line items instead of in the
     * footer of the line group
     *
     * @return boolean
     */
    public boolean isActionsInLineGroup() {
        return actionsInLineGroup;
    }

    /**
     * Set flag to add actions in the same group as the line items
     *
     * @param actionsInLineGroup
     */
    public void setActionsInLineGroup(boolean actionsInLineGroup) {
        this.actionsInLineGroup = actionsInLineGroup;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T> void copyProperties(T layoutManager) {
        super.copyProperties(layoutManager);

        StackedLayoutManager stackedLayoutManagerCopy = (StackedLayoutManager) layoutManager;

        stackedLayoutManagerCopy.setSummaryTitle(this.summaryTitle);

        if (summaryFields != null) {
            stackedLayoutManagerCopy.setSummaryFields(new ArrayList<String>(summaryFields));
        }

        if (this.addLineGroup != null) {
            stackedLayoutManagerCopy.setAddLineGroup((Group) this.addLineGroup.copy());
        }

        if (this.lineGroupPrototype != null) {
            stackedLayoutManagerCopy.setLineGroupPrototype((Group) this.lineGroupPrototype.copy());
        }

        if (this.wrapperGroup != null) {
            stackedLayoutManagerCopy.setWrapperGroup((Group) this.wrapperGroup.copy());
        }

        if (this.subCollectionFieldGroupPrototype != null) {
            stackedLayoutManagerCopy.setSubCollectionFieldGroupPrototype(
                    (FieldGroup) this.subCollectionFieldGroupPrototype.copy());
        }

        if (this.selectFieldPrototype != null) {
            stackedLayoutManagerCopy.setSelectFieldPrototype((Field) this.selectFieldPrototype.copy());
        }

        if (this.stackedGroups != null) {
            List<Group> stackedGroupsCopy = ComponentUtils.copy(stackedGroups);
            stackedLayoutManagerCopy.setStackedGroups(stackedGroupsCopy);
        }

        Pager pager = this.getPagerWidget();
        if (pager != null) {
            stackedLayoutManagerCopy.setPagerWidget(pager.<Pager> copy());
        }

        stackedLayoutManagerCopy.setActionsInLineGroup(this.isActionsInLineGroup());
    }
}