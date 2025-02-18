/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ejb.support;

import jakarta.ejb.CreateException;
import jakarta.ejb.EJBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenient superclass for stateless session beans (SLSBs), minimizing
 * the work involved in implementing an SLSB and preventing common errors.
 * <b>Note that SLSBs are the most useful kind of EJB.</b>
 *
 * <p>As the ejbActivate() and ejbPassivate() methods cannot be invoked
 * on SLSBs, these methods are implemented to throw an exception and should
 * not be overriden by subclasses. (Unfortunately the EJB specification
 * forbids enforcing this by making EJB lifecycle methods final.)
 *
 * <p>There should be no need to override the setSessionContext() or
 * ejbCreate() lifecycle methods.
 *
 * <p>Subclasses are left to implement the onEjbCreate() method to do
 * whatever initialization they wish to do after their BeanFactory has
 * already been loaded, and is available from the getBeanFactory() method.
 *
 * <p>This class provides the no-argument ejbCreate() method required
 * by the EJB specification, but not the SessionBean interface,
 * eliminating a common cause of EJB deployment failure.
 *
 * @author Rod Johnson
 */
public abstract class AbstractStatelessSessionBean extends AbstractSessionBean {

	protected final Log logger = LogFactory.getLog(getClass());


	/**
	 * This implementation loads the BeanFactory. A BeansException thrown by
	 * loadBeanFactory will simply get propagated, as it is a runtime exception.
	 * <p>Don't override it (although it can't be made final): code your own
	 * initialization in onEjbCreate(), which is called when the BeanFactory
	 * is available.
	 * <p>Unfortunately we can't load the BeanFactory in setSessionContext(),
	 * as resource manager access isn't permitted there - but the BeanFactory
	 * may require it.
	 */
	public void createBean() {
		loadBeanFactory();
		onEjbCreate();
	}
	
	/**
	 * Subclasses must implement this method to do any initialization
	 * they would otherwise have done in an ejbCreate() method. In contrast
	 * to ejbCreate, the BeanFactory will have been loaded here.
	 * <p>The same restrictions apply to the work of this method as
	 * to an ejbCreate() method.
	 * @throws CreateException
	 */
	protected abstract void onEjbCreate() ;


	/**
	 * @see jakarta.ejb.SessionBean#ejbActivate(). This method always throws an exception, as
	 * it should not be invoked by the EJB container.
	 */
	public void ejbActivate() throws EJBException {
		throw new IllegalStateException("ejbActivate must not be invoked on a stateless session bean");
	}

	/**
	 * @see jakarta.ejb.SessionBean#ejbPassivate(). This method always throws an exception, as
	 * it should not be invoked by the EJB container.
	 */
	public void ejbPassivate() throws EJBException {
		throw new IllegalStateException("ejbPassivate must not be invoked on a stateless session bean");
	}

}
