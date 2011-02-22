/*
 * Copyright 2007 The Kuali Foundation
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
package org.kuali.rice.kns.service;

import org.junit.Test;
import org.kuali.rice.core.api.parameter.Parameter;
import org.kuali.rice.core.framework.services.CoreFrameworkServiceLocator;
import org.kuali.rice.core.impl.parameter.ParameterBo;
import org.kuali.rice.core.impl.parameter.ParameterId;
import org.kuali.rice.core.impl.parameter.ParameterTypeBo;
import org.kuali.rice.core.jpa.metadata.MetadataManager;
import org.kuali.rice.kns.bo.StateImpl;
import org.kuali.rice.kns.bo.StateImplId;
import org.kuali.rice.kns.test.document.bo.Account;
import org.kuali.rice.kns.test.document.bo.AccountExtension;
import org.kuali.rice.shareddata.api.country.Country;
import org.kuali.test.KNSTestCase;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for MetadataManager methods
 * 
 * @author Kuali Rice Team (rice.collab@kuali.org)
 *
 */
public class MetadataManagerTest extends KNSTestCase {
	/**
	 * Tests that MetadataManager can convert a primary key Map to a key object correctly
	 */
	@Test
	public void testPKMapToObject() {
		Map<String, Object> pkMap = new HashMap<String, Object>();
		Object pkValue = MetadataManager.convertPrimaryKeyMapToObject(Country.class, pkMap);
		assertNull("An empty map should return a null key", pkValue);
		
		pkMap.put("postalCountryCode", "AN");
		pkValue = MetadataManager.convertPrimaryKeyMapToObject(Country.class, pkMap);
		assertEquals("Single pkValue should be of class String", String.class, pkValue.getClass());
		assertEquals("Single pkValue should be \"AN\"", "AN", pkValue);
		
		pkMap.put("postalCountryName", "ANDORRA");
		boolean exceptionThrown = false;
		try {
			pkValue = MetadataManager.convertPrimaryKeyMapToObject(Country.class, pkMap);
		} catch (IllegalArgumentException iae) {
			exceptionThrown = true;
		}
		assertTrue("Multiple keys did not lead to exception", exceptionThrown);
		
		pkMap.clear();
		
		pkMap.put("postalCountryCode", "US");
		pkMap.put("postalStateCode", "WV");
		pkValue = MetadataManager.convertPrimaryKeyMapToObject(StateImpl.class, pkMap);
		org.junit.Assert.assertEquals("Composite pkValue for State should have class of StateId", StateImplId.class, pkValue.getClass());
		StateImplId stateId = (StateImplId)pkValue;
		assertEquals("postalCountryCode was not correctly set", "US", stateId.getPostalCountryCode());
		assertEquals("postalStateCode was not correctly set", "WV", stateId.getPostalStateCode());
		
		pkMap.put("postalStateName", "WEST VIRGINIA");
		exceptionThrown = false;
		try {
			pkValue = MetadataManager.convertPrimaryKeyMapToObject(StateImpl.class, pkMap);
		} catch (Exception e) {
			exceptionThrown = true;
		}
		assertTrue("Non primary key field caused exception", exceptionThrown);
	}
	
	/**
	 * Tests that MetadataManager.getPersistableBusinessObjectPrimaryKeyObject correctly pulls primary keys
	 * from a BO
	 */
	@Test
	public void testPKObjectForEntity() {
		ParameterTypeBo parameterType = KNSServiceLocator.getBusinessObjectService().findBySinglePrimaryKey(ParameterTypeBo.class, "CONFG");
		assertNotNull("ParameterType should not be null", parameterType);
		
		Object pkValue = MetadataManager.getPersistableBusinessObjectPrimaryKeyObject(parameterType);
		assertEquals("Single pkValue should be of class String", String.class, pkValue.getClass());
		assertEquals("Single pkValue should be \"CONFG\"", "CONFG", pkValue);
		
		Parameter parameter = CoreFrameworkServiceLocator.getClientParameterService().getParameter("KR-NS", "Lookup", "MULTIPLE_VALUE_RESULTS_PER_PAGE");
		assertNotNull("State should not be null", parameter);
		
		pkValue = MetadataManager.getPersistableBusinessObjectPrimaryKeyObject(ParameterBo.from(parameter));
		org.junit.Assert.assertEquals("Composite pkValue for Parameter should have class of ParameterId", ParameterId.class, pkValue.getClass());
		ParameterId parameterId = (ParameterId)pkValue;
		assertEquals("namespace code was not correctly set", "KR-NS", parameterId.getNamespaceCode());
		assertEquals("parameter detail type code was not correctly set", "Lookup", parameterId.getComponentCode());
		assertEquals("parameter name was not correctly set", "MULTIPLE_VALUE_RESULTS_PER_PAGE", parameterId.getName());
		assertEquals("parameterApplicationNamespaceCode was not correctly set", "KUALI", parameterId.getApplicationCode());
	}
	
	/**
	 * Tests that MetadataManager.getPersistableBusinessObjectPrimaryKeyObjectWithValuesForExtension works
	 */
	@Test
	public void testPKObjectForExtension() {
		final Account account = KNSServiceLocator.getBusinessObjectService().findBySinglePrimaryKey(Account.class, "a1");
		assertNotNull("Account should not be null", account);
		final AccountExtension accountExtension = (AccountExtension)account.getExtension();
		
		final Object pkValue = MetadataManager.getPersistableBusinessObjectPrimaryKeyObjectWithValuesForExtension(account, accountExtension);
		assertEquals("Single pkValue should be of class String", String.class, pkValue.getClass());
		assertEquals("Single pkValue should be \"a1\"", "a1", pkValue);
	}
}
