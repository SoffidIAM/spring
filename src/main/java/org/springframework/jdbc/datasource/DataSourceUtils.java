/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
 
/**
 * Helper class that provides static methods to obtain JDBC Connections from
 * a DataSource, and to close Connections if necessary. Has special support for
 * Spring-managed connections, e.g. for use with DataSourceTransactionManager.
 *
 * <p>Used internally by JdbcTemplate, JDBC operation objects and the JDBC
 * DataSourceTransactionManager. Can also be used directly in application code.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #getConnection
 * @see #releaseConnection
 * @see DataSourceTransactionManager
 * @see org.springframework.jdbc.core.JdbcTemplate
 * @see org.springframework.jdbc.object
 */
public abstract class DataSourceUtils {

	/**
	 * Order value for TransactionSynchronization objects that clean up
	 * JDBC Connections.
	 */
	public static final int CONNECTION_SYNCHRONIZATION_ORDER = 1000;

	private static final Log logger = LogFactory.getLog(DataSourceUtils.class);


	/**
	 * Get a Connection from the given DataSource. Changes any SQL exception into
	 * the Spring hierarchy of unchecked generic data access exceptions, simplifying
	 * calling code and making any exception that is thrown more meaningful.
	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
	 * when using DataSourceTransactionManager. Will bind a Connection to the thread
	 * if transaction synchronization is active (e.g. if in a JTA transaction).
	 * @param dataSource DataSource to get Connection from
	 * @return a JDBC Connection from the given DataSource
	 * @throws org.springframework.jdbc.CannotGetJdbcConnectionException
	 * if the attempt to get a Connection failed
	 * @see org.springframework.transaction.support.TransactionSynchronizationManager
	 * @see DataSourceTransactionManager
	 */
	public static Connection getConnection(DataSource dataSource) throws CannotGetJdbcConnectionException {
		try {
			return doGetConnection(dataSource);
		}
		catch (SQLException ex) {
			throw new CannotGetJdbcConnectionException("Could not get JDBC Connection", ex);
		}
	}

	/**
	 * Actually get a JDBC Connection for the given DataSource.
	 * Same as <code>getConnection</code>, but throwing the original SQLException.
	 * <p>Is aware of a corresponding Connection bound to the current thread, for example
	 * when using DataSourceTransactionManager. Will bind a Connection to the thread
	 * if transaction synchronization is active (e.g. if in a JTA transaction).
	 * <p>Directly accessed by TransactionAwareDataSourceProxy.
	 * @param dataSource DataSource to get Connection from
	 * @return a JDBC Connection from the given DataSource
	 * @throws SQLException if thrown by JDBC methods
	 * @see #getConnection(DataSource)
	 * @see TransactionAwareDataSourceProxy
	 */
	public static Connection doGetConnection(DataSource dataSource) throws SQLException {
		Assert.notNull(dataSource, "No DataSource specified");

		ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
		if (conHolder != null && (conHolder.hasConnection() || conHolder.isSynchronizedWithTransaction())) {
			conHolder.requested();
			if (!conHolder.hasConnection()) {
				logger.debug("Fetching resumed JDBC Connection from DataSource");
				conHolder.setConnection(dataSource.getConnection());
			}
			return conHolder.getConnection();
		}
		// Else we either got no holder or an empty thread-bound holder here.

		logger.debug("Fetching JDBC Connection from DataSource");
		Connection con = dataSource.getConnection();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for JDBC Connection");
			// Use same Connection for further JDBC actions within the transaction.
			// Thread-bound object will get removed by synchronization at transaction completion.
			ConnectionHolder holderToUse = conHolder;
			if (holderToUse == null) {
				holderToUse = new ConnectionHolder(con);
			}
			else {
				holderToUse.setConnection(con);
			}
			holderToUse.requested();
			TransactionSynchronizationManager.registerSynchronization(
					new ConnectionSynchronization(holderToUse, dataSource));
			holderToUse.setSynchronizedWithTransaction(true);
			if (holderToUse != conHolder) {
				TransactionSynchronizationManager.bindResource(dataSource, holderToUse);
			}
		}

		return con;
	}

	/**
	 * Prepare the given Connection with the given transaction semantics.
	 * @param con the Connection to prepare
	 * @param definition the transaction definition to apply
	 * @return the previous isolation level, if any
	 * @throws SQLException if thrown by JDBC methods
	 * @see #resetConnectionAfterTransaction
	 */
	public static Integer prepareConnectionForTransaction(Connection con, TransactionDefinition definition)
			throws SQLException {

		Assert.notNull(con, "No Connection specified");

		// Set read-only flag.
		if (definition != null && definition.isReadOnly()) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Setting JDBC Connection [" + con + "] read-only");
				}
				con.setReadOnly(true);
			}
			catch (Throwable ex) {
				// SQLException or UnsupportedOperationException
				// -> ignore, it's just a hint anyway.
				logger.debug("Could not set JDBC Connection read-only", ex);
			}
		}

		// Apply specific isolation level, if any.
		Integer previousIsolationLevel = null;
		if (definition != null && definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			if (logger.isDebugEnabled()) {
				logger.debug("Changing isolation level of JDBC Connection [" + con + "] to " +
						definition.getIsolationLevel());
			}
			previousIsolationLevel = new Integer(con.getTransactionIsolation());
			con.setTransactionIsolation(definition.getIsolationLevel());
		}

		return previousIsolationLevel;
	}

	/**
	 * Reset the given Connection after a transaction,
	 * regarding read-only flag and isolation level.
	 * @param con the Connection to reset
	 * @param previousIsolationLevel the isolation level to restore, if any
	 * @see #prepareConnectionForTransaction
	 */
	public static void resetConnectionAfterTransaction(Connection con, Integer previousIsolationLevel) {
		Assert.notNull(con, "No Connection specified");
		try {
			// Reset transaction isolation to previous value, if changed for the transaction.
			if (previousIsolationLevel != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Resetting isolation level of JDBC Connection [" +
							con + "] to " + previousIsolationLevel);
				}
				con.setTransactionIsolation(previousIsolationLevel.intValue());
			}

			// Reset read-only flag.
			if (con.isReadOnly()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Resetting read-only flag of JDBC Connection [" + con + "]");
				}
				con.setReadOnly(false);
			}
		}
		catch (Throwable ex) {
			logger.debug("Could not reset JDBC Connection after transaction", ex);
		}
	}

	/**
	 * Apply the current transaction timeout, if any,
	 * to the given JDBC Statement object.
	 * @param stmt the JDBC Statement object
	 * @param dataSource DataSource that the Connection came from
	 * @see java.sql.Statement#setQueryTimeout
	 */
	public static void applyTransactionTimeout(Statement stmt, DataSource dataSource) throws SQLException {
		Assert.notNull(stmt, "No statement specified");
		ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
		if (holder != null && holder.hasTimeout()) {
			stmt.setQueryTimeout(holder.getTimeToLiveInSeconds());
		}
	}

	/**
	 * Close the given Connection if necessary, i.e. if it is not bound to the thread
	 * and it is not created by a SmartDataSource returning shouldClose=false.
	 * @deprecated in favor of releaseConnection
	 * @see #releaseConnection
	 */
	public static void closeConnectionIfNecessary(Connection con, DataSource dataSource) {
		releaseConnection(con, dataSource);
	}

	/**
	 * Close the given Connection, created via the given DataSource,
	 * if it is not managed externally (i.e. not bound to the thread).
	 * Will never close a Connection from a SmartDataSource returning shouldClose=false.
	 * @param con Connection to close if necessary
	 * (if this is null, the call will be ignored)
	 * @param dataSource DataSource that the Connection came from (can be <code>null</code>)
	 * @see SmartDataSource#shouldClose
	 */
	public static void releaseConnection(Connection con, DataSource dataSource) {
		try {
			doReleaseConnection(con, dataSource);
		}
		catch (SQLException ex) {
			logger.error("Could not close JDBC Connection", ex);
		}
		catch (RuntimeException ex) {
			logger.error("Unexpected exception on closing JDBC Connection", ex);
		}
	}

	/**
	 * Actually release a JDBC Connection for the given DataSource.
	 * Same as <code>releaseConnection</code>, but throwing the original SQLException.
	 * <p>Directly accessed by TransactionAwareDataSourceProxy.
	 * @param con Connection to close if necessary
	 * (if this is null, the call will be ignored)
	 * @param dataSource DataSource that the Connection came from (can be <code>null</code>)
	 * @throws SQLException if thrown by JDBC methods
	 * @see #releaseConnection
	 * @see TransactionAwareDataSourceProxy
	 */
	public static void doReleaseConnection(Connection con, DataSource dataSource) throws SQLException {
		if (con == null) {
			return;
		}

		if (dataSource != null) {
			ConnectionHolder conHolder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);
			if (conHolder != null && conHolder.hasConnection() && connectionEquals(conHolder.getConnection(), con)) {
				// It's the transactional Connection: Don't close it.
				conHolder.released();
				return;
			}
		}

		// Leave the Connection open only if the DataSource is our
		// special data source, and it wants the Connection left open.
		if (!(dataSource instanceof SmartDataSource) || ((SmartDataSource) dataSource).shouldClose(con)) {
			logger.debug("Returning JDBC Connection to DataSource");
			con.close();
		}
	}

	/**
	 * Return whether the given two Connections are equal, asking the target
	 * Connection in case of a proxy. Used to detect equality even if the
	 * user passed in a raw target Connection while the held one is a proxy.
	 * @param heldCon the held Connection (potentially a proxy)
	 * @param passedInCon the Connection passed-in by the user
	 * (potentially a target Connection without proxy)
	 * @see #getTargetConnection
	 */
	private static boolean connectionEquals(Connection heldCon, Connection passedInCon) {
		// Explicitly check for identity too: for Connection handles that do not implement
		// "equals" properly, such as the ones Commons DBCP exposes).
		return (heldCon == passedInCon || heldCon.equals(passedInCon) ||
				getTargetConnection(heldCon).equals(passedInCon));
	}

	/**
	 * Return the innermost target Connection of the given Connection. If the given
	 * Connection is a proxy, it will be unwrapped until a non-proxy Connection is
	 * found. Else, the passed-in Connection will be returned as-is.
	 * @param con the Connection proxy to unwrap
	 * @return the innermost target Connection, or the passed-in one if no proxy
	 * @see ConnectionProxy#getTargetConnection
	 */
	public static Connection getTargetConnection(Connection con) {
		Connection conToUse = con;
		while (conToUse instanceof ConnectionProxy) {
			conToUse = ((ConnectionProxy) conToUse).getTargetConnection();
		}
		return conToUse;
	}


	/**
	 * Callback for resource cleanup at the end of a non-native JDBC transaction
	 * (e.g. when participating in a JtaTransactionManager transaction).
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	private static class ConnectionSynchronization extends TransactionSynchronizationAdapter {

		private final ConnectionHolder connectionHolder;

		private final DataSource dataSource;

		private boolean holderActive = true;

		public ConnectionSynchronization(ConnectionHolder connectionHolder, DataSource dataSource) {
			this.connectionHolder = connectionHolder;
			this.dataSource = dataSource;
		}

		public int getOrder() {
			return CONNECTION_SYNCHRONIZATION_ORDER;
		}

		public void suspend() {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResource(this.dataSource);
				if (this.connectionHolder.hasConnection() && !this.connectionHolder.isOpen()) {
					// Release Connection on suspend if the application doesn't keep
					// a handle to it anymore. We will fetch a fresh Connection if the
					// application accesses the ConnectionHolder again after resume,
					// assuming that it will participate in the same transaction.
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
					this.connectionHolder.setConnection(null);
				}
			}
		}

		public void resume() {
			if (this.holderActive) {
				TransactionSynchronizationManager.bindResource(this.dataSource, this.connectionHolder);
			}
		}

		public void beforeCompletion() {
			// Release Connection early if the holder is not open anymore
			// (i.e. not used by another resource like a Hibernate Session
			// that has its own cleanup via transaction synchronization),
			// to avoid issues with strict JTA implementations that expect
			// the close call before transaction completion.
			if (!this.connectionHolder.isOpen()) {
				TransactionSynchronizationManager.unbindResource(this.dataSource);
				this.holderActive = false;
				if (this.connectionHolder.hasConnection()) {
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
				}
			}
		}

		public void afterCompletion(int status) {
			// If we haven't closed the Connection in beforeCompletion,
			// close it now. The holder might have been used for other
			// cleanup in the meantime, for example by a Hibernate Session.
			if (this.holderActive) {
				// The thread-bound ConnectionHolder might not be available anymore,
				// since afterCompletion might get called from a different thread.
				if (TransactionSynchronizationManager.hasResource(this.dataSource)) {
					TransactionSynchronizationManager.unbindResource(this.dataSource);
				}
				this.holderActive = false;
				if (this.connectionHolder.hasConnection()) {
					releaseConnection(this.connectionHolder.getConnection(), this.dataSource);
					// Reset the ConnectionHolder - it might remain bound to the thread.
					this.connectionHolder.setConnection(null);
				}
				this.connectionHolder.reset();
			}
		}
	}

}
