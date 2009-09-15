/*
 * Copyright 2005-2008 The Kuali Foundation
 * 
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
package org.kuali.rice.kew.user;

import org.kuali.rice.kew.identity.PrincipalId;

/**
 * A unique user Id which is generated by the application.
 *
 * @deprecated use {@link PrincipalId} instead
 *
 * @author Kuali Rice Team (kuali-rice@googlegroups.com)
 */
public class WorkflowUserId implements UserId {

	private static final long serialVersionUID = -5551723348738932404L;

	private String workflowId;

    public WorkflowUserId(String workflowId) {
        setWorkflowId(workflowId);
    }

    public WorkflowUserId() {
    }

    public String getId() {
        return getWorkflowId();
    }
    
    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = (workflowId == null ? null : workflowId.trim());
    }

    /**
     * Returns true if this userId has an empty value. Empty userIds can't be used as keys in a Hash, among other things.
     * 
     * @return true if this instance doesn't have a value
     */
    public boolean isEmpty() {
    	return (workflowId == null || workflowId.trim().length() == 0);
    }

    /**
     * If you make this class non-final, you must rewrite equals to work for subclasses.
     */
    public boolean equals(Object obj) {
        if (obj != null && (obj instanceof WorkflowUserId)) {
            WorkflowUserId w = (WorkflowUserId) obj;

            if (getWorkflowId() == null) {
                return false;
            }
            return workflowId.equals(w.getWorkflowId());
        }

        return false;
    }

    public int hashCode() {
        return workflowId == null ? 0 : workflowId.hashCode();
    }

    public String toString() {
        if (workflowId == null) {
            return "workflowId: null";
        }
        return "workflowId: " + workflowId;
    }
}
