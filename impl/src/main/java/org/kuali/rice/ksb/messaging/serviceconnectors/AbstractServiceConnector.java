/*
 * Copyright 2007 The Kuali Foundation
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
package org.kuali.rice.ksb.messaging.serviceconnectors;

import org.kuali.rice.core.security.credentials.CredentialsSource;
import org.kuali.rice.ksb.messaging.BusClientFailureProxy;
import org.kuali.rice.ksb.messaging.ServiceInfo;
import org.kuali.rice.ksb.messaging.bam.BAMClientProxy;
import org.springframework.util.Assert;


/**
 * Abstract implementation of the ServiceConnector that holds the ServiceInfo
 * and the CredentialsSource as well as providing convenience proxy methods.
 * 
 * @author Kuali Rice Team (kuali-rice@googlegroups.com)
 * @since 0.9
 * 
 */
public abstract class AbstractServiceConnector implements ServiceConnector {

	/**
	 * Maintains the information about the service.  This should never be null.
	 */
	private ServiceInfo serviceInfo;

	/**
	 * Maintains the credentials needed by the service.  This may be null.
	 */
	private CredentialsSource credentialsSource;

	public AbstractServiceConnector(final ServiceInfo serviceInfo) {
		Assert.notNull(serviceInfo, "serviceInfo cannot be null");
		this.serviceInfo = serviceInfo;
	}

	public ServiceInfo getServiceInfo() {
		return this.serviceInfo;
	}

	public void setServiceInfo(final ServiceInfo serviceInfo) {
		this.serviceInfo = serviceInfo;
	}

	public void setCredentialsSource(final CredentialsSource credentialsSource) {
		this.credentialsSource = credentialsSource;
	}

	protected CredentialsSource getCredentialsSource() {
		return this.credentialsSource;
	}

	protected Object getServiceProxyWithFailureMode(final Object service,
			final ServiceInfo serviceInfo) {
		Object bamWrappedClientProxy = BAMClientProxy
				.wrap(service, serviceInfo);
		return BusClientFailureProxy.wrap(bamWrappedClientProxy, serviceInfo);
	}
}
