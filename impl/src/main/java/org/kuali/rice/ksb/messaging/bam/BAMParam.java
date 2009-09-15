/*
 * Copyright 2005-2007 The Kuali Foundation
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
package org.kuali.rice.ksb.messaging.bam;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import org.apache.commons.lang.StringUtils;
import org.kuali.rice.core.jpa.annotations.Sequence;
import org.kuali.rice.core.util.OrmUtils;
import org.kuali.rice.ksb.service.KSBServiceLocator;

/**
 * A parameter of a method invocation recorded by the BAM.
 *
 * @author Kuali Rice Team (kuali-rice@googlegroups.com)
 */
@Entity
@Table(name="KRSB_BAM_PARM_T")
@Sequence(name="KRSB_BAM_PARM_S", property="bamParamId")
public class BAMParam {

	@Id
	@Column(name="BAM_PARM_ID")
	private Long bamParamId;
	@ManyToOne(fetch=FetchType.EAGER, cascade={CascadeType.PERSIST})
	@JoinColumn(name="BAM_ID")
	private BAMTargetEntry bamTargetEntry;
	@Column(name="PARM")
	private String param;
	
	@PrePersist
    public void beforeInsert(){
	    OrmUtils.populateAutoIncValue(this, KSBServiceLocator.getRegistryEntityManagerFactory().createEntityManager());
    }
	
	public BAMTargetEntry getBamTargetEntry() {
		return this.bamTargetEntry;
	}
	public void setBamTargetEntry(BAMTargetEntry bamTargetEntry) {
		this.bamTargetEntry = bamTargetEntry;
	}
	public Long getBamParamId() {
		return this.bamParamId;
	}
	public void setBamParamId(Long bamParamId) {
		this.bamParamId = bamParamId;
	}
	public String getParam() {
		return this.param;
	}
	public void setParam(String paramToString) {
		if (StringUtils.isEmpty(paramToString)) {
			paramToString = "<null>";//oracle blows null constraint on empty strings
		}
		this.param = paramToString;
	}
}
