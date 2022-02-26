package org.springframework.transaction.interceptor;

import org.springframework.transaction.TransactionDefinition;

public class NoTransactionDefinition implements TransactionDefinition {
	public int getPropagationBehavior() {
		return TransactionDefinition.PROPAGATION_SUPPORTS;
	}

	public int getIsolationLevel() {
		return TransactionDefinition.ISOLATION_DEFAULT;
	}

	public int getTimeout() {
		return 0;
	}

	public boolean isReadOnly() {
		return false;
	}

	public String getName() {
		return "readonly";
	}

}
