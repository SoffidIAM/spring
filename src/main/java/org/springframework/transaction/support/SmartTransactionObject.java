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

package org.springframework.transaction.support;

/**
 * Interface to be implemented by transaction objects that are able to
 * return an internal rollback-only marker, typically from a another
 * transaction that has participated and marked it as rollback-only.
 *
 * <p>Autodetected by DefaultTransactionStatus, to always return a
 * current rollbackOnly flag even if not resulting from the current
 * TransactionStatus.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see DefaultTransactionStatus#isRollbackOnly
 */
public interface SmartTransactionObject {

	/**
	 * Return whether the transaction is internally marked as rollback-only.
	 * Can, for example, check the JTA UserTransaction.
	 * @see jakarta.transaction.UserTransaction#getStatus
	 * @see jakarta.transaction.Status#STATUS_MARKED_ROLLBACK
	 */
	boolean isRollbackOnly();

}
