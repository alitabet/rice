/**
 * Copyright 2005-2012 The Kuali Foundation
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
package org.kuali.rice.krad.datadictionary.validation.constraint;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code WhenConstraint} is a child of a {@link CaseConstraint}
 *
 * <p>It provides a specific additional constraint that should be processed when
 * the condition itself is true.</p>
 * 
 * <p>So a case constraint on country, might have a when constraint with value='USA', and another with value='Canada'. Each of these
 * {@code WhenConstraint}'s would define a constraint of their own that would only be processed when the country was USA, or when the country
 * was Canada.</p>
 *
 * <p>A {@code WhenConstraint} either specifies an attribute path whose value it then provides or a constraint.
 * The parent @{CaseConstraint} is defined on the field on which the constraints are desired to take effect.</p>
 * 
 * @author Kuali Rice Team (rice.collab@kuali.org)
 * @since 1.1
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class WhenConstraint implements Constraint {
	protected List<Object> values;
	protected String valuePath;
	protected Constraint constraint;

    /**
     * gets the list of values defined for this constraint
     *
     * @return a list of values for which to activate the associated constraint
     */
	public List<Object> getValues() {
		return values;
	}

    /**
     * setter for values
     *
     * @param values - the values to set
     */
	public void setValues(List<Object> values) {
        this.values = values;
    }

    /**
     * gets the value defined for this constraint
     *
     * @param value - a values for which to activate the associated constraint
     */
    public void setValue(Object value) {	    
	    values = new ArrayList<Object>();
	    values.add(value);
	}

    /**
     * gets a path that can retrieve an attributes value
     *
     * @return a string representation of specifically which attribute (at some depth) is being accessed
     */
	public String getValuePath() {
		return valuePath;
	}

    /**
     * setter for the value path
     *
     * @param valuePath - the value path to set
     */
	public void setValuePath(String valuePath) {
		this.valuePath = valuePath;
	}

    /**
     * gets the constraint to apply to the field when the {@code WhenConstraint} value/values match
     *
     * @return
     */
	public Constraint getConstraint() {
		return constraint;
	}

    /**
     * setter for constraint
     *
     * @param constraint - the constraint to set
     */
	public void setConstraint(Constraint constraint) {
		this.constraint = constraint;
	}
}
