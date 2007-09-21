/*
 * Copyright 2005-2007 The Kuali Foundation.
 * 
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
package edu.iu.uis.eden.routetemplate.dao;

import java.util.List;

import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.springmodules.orm.ojb.support.PersistenceBrokerDaoSupport;

import edu.iu.uis.eden.routetemplate.RuleAttribute;

public class RuleAttributeDAOOjbImpl extends PersistenceBrokerDaoSupport implements RuleAttributeDAO {

	private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(RuleAttributeDAOOjbImpl.class);

	public void save(RuleAttribute ruleAttribute) {
		this.getPersistenceBrokerTemplate().store(ruleAttribute);
	}

	public void delete(Long ruleAttributeId) {
		this.getPersistenceBrokerTemplate().delete(findByRuleAttributeId(ruleAttributeId));
	}

	public RuleAttribute findByRuleAttributeId(Long ruleAttributeId) {
		RuleAttribute ruleAttribute = new RuleAttribute();
		ruleAttribute.setRuleAttributeId(ruleAttributeId);
		return (RuleAttribute) this.getPersistenceBrokerTemplate().getObjectByQuery(new QueryByCriteria(ruleAttribute));
	}

	public List findByRuleAttribute(RuleAttribute ruleAttribute) {
		Criteria crit = new Criteria();
		if (ruleAttribute.getName() != null) {
			crit.addSql("UPPER(RULE_ATTRIB_NM) like '" + ruleAttribute.getName().toUpperCase() + "'");
		}
		if (ruleAttribute.getClassName() != null) {
			crit.addSql("UPPER(RULE_ATTRIB_CLS_NM) like '" + ruleAttribute.getClassName().toUpperCase() + "'");
		}
		if (ruleAttribute.getType() != null) {
			crit.addSql("UPPER(RULE_ATTRIB_TYP) like '" + ruleAttribute.getType().toUpperCase() + "'");
		}
		return (List) this.getPersistenceBrokerTemplate().getCollectionByQuery(new QueryByCriteria(RuleAttribute.class, crit));

	}

	public List getAllRuleAttributes() {
		return (List) this.getPersistenceBrokerTemplate().getCollectionByQuery(new QueryByCriteria(RuleAttribute.class));
	}

	public RuleAttribute findByName(String name) {
		LOG.debug("findByName name=" + name);
		Criteria crit = new Criteria();
		crit.addEqualTo("name", name);
		return (RuleAttribute) this.getPersistenceBrokerTemplate().getObjectByQuery(new QueryByCriteria(RuleAttribute.class, crit));
	}

	public RuleAttribute findByClassName(String classname) {
		Criteria crit = new Criteria();
		crit.addEqualTo("className", classname);
		return (RuleAttribute) this.getPersistenceBrokerTemplate().getObjectByQuery(new QueryByCriteria(RuleAttribute.class, crit));
	}

}