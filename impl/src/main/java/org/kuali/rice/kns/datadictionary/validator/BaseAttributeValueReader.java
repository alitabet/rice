/*
 * Copyright 2011 The Kuali Foundation
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.rice.kns.datadictionary.validator;

import java.util.List;

import org.kuali.rice.core.jdbc.SqlBuilder;
import org.kuali.rice.kns.datadictionary.DataDictionaryEntry;
import org.kuali.rice.kns.datadictionary.exception.AttributeValidationException;
import org.kuali.rice.kns.service.KNSServiceLocator;
import org.kuali.rice.kns.util.DataTypeUtil;

/**
 * 
 * @author James Renfro, University of Washington 
 */
public abstract class BaseAttributeValueReader implements AttributeValueReader {

	protected String entryName;
	protected String attributeName;
	
	@Override
	public List<String> getCleanSearchableValues(String attributeKey) throws AttributeValidationException {
		Class<?> attributeType = getType(attributeKey);		
		Object rawValue = getValue(attributeKey);
		
		String attributeInValue = rawValue != null ? rawValue.toString() : "";
		String attributeDataType = DataTypeUtil.determineDataType(attributeType);
		return SqlBuilder.getCleanedSearchableValues(attributeInValue, attributeDataType);
	}
	
	@Override
	public DataDictionaryEntry getEntry(String entryName) {
		return KNSServiceLocator.getDataDictionaryService().getDataDictionary().getDictionaryObjectEntry(entryName);
	}

	/**
	 * @return the currentName
	 */
	@Override
	public String getAttributeName() {
		return this.attributeName;
	}

	/**
	 * @param currentName the currentName to set
	 */
	@Override
	public void setAttributeName(String currentName) {
		this.attributeName = currentName;
	}

	/**
	 * @return the entryName
	 */
	public String getEntryName() {
		return this.entryName;
	}

	/**
	 * @param entryName the entryName to set
	 */
	public void setEntryName(String entryName) {
		this.entryName = entryName;
	}
	
}
