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

package org.springframework.transaction.jta;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.springframework.util.Assert;

/**
 * Adapter for a JTA UserTransaction handle, taking a JTA TransactionManager
 * reference and creating a JTA UserTransaction handle for it.
 *
 * <p>The JTA UserTransaction interface is an exact subset of the JTA
 * TransactionManager interface. Unfortunately, it does not serve as
 * super-interface of TransactionManager, though, which requires an
 * adapter such as this class to be used when intending to talk to
 * a TransactionManager handle through the UserTransaction interface.
 *
 * <p>Used internally by Spring's JtaTransactionManager for certain scenarios.
 * Not intended for direct use in application code.
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 */
public class UserTransactionAdapter implements UserTransaction {

	private final TransactionManager transactionManager;


	/**
	 * Create a new UserTransactionAdapter.
	 * @param transactionManager the JTA TransactionManager
	 */
	public UserTransactionAdapter(TransactionManager transactionManager) {
		Assert.notNull(transactionManager, "TransactionManager is required");
		this.transactionManager = transactionManager;
	}

	/**
	 * Return the JTA TransactionManager that this adapter delegates to.
	 */
	public TransactionManager getTransactionManager() {
		return transactionManager;
	}


	public void begin() throws NotSupportedException, SystemException {
		this.transactionManager.begin();
	}

	public void commit()
			throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
			SecurityException, IllegalStateException, SystemException {
		this.transactionManager.commit();
	}

	public int getStatus() throws SystemException {
		return this.transactionManager.getStatus();
	}

	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		this.transactionManager.rollback();
	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		this.transactionManager.setRollbackOnly();
	}

	public void setTransactionTimeout(int timeout) throws SystemException {
		this.transactionManager.setTransactionTimeout(timeout);
	}

}
