/*
 * Copyright 2005-2009 The Kuali Foundation
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
package org.kuali.rice.kew.dto;

/**
 * Signal to the PostProcessor that the routeHeader is about to be processed
 * 
 * @author Kuali Rice Team (kuali-rice@googlegroups.com)
 */
public class BeforeProcessEventDTO extends DocumentEventDTO {

	private static final long serialVersionUID = 1462372563938714818L;
	
	private Long nodeInstanceId;

    public BeforeProcessEventDTO() {
        super(BEFORE_PROCESS);
    }

    public Long getNodeInstanceId() {
        return this.nodeInstanceId;
    }

    public void setNodeInstanceId(Long nodeInstanceId) {
        this.nodeInstanceId = nodeInstanceId;
    }
    
}
