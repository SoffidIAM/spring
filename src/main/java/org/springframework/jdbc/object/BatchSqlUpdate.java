/*
 * Copyright 2002-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.jdbc.object;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;

/**
 * SqlUpdate subclass that performs batch update operations. Encapsulates
 * queuing up records to be updated, and adds them as a single batch once
 * <code>flush</code> is called or the given batch size has been met.
 *
 * <p>Note that this class is a <b>non-thread-safe object</b>, in contrast
 * to all other JDBC operations objects in this package. You need to create
 * a new instance of it for each use, or call <code>reset</code> before
 * reuse within the same thread.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.1
 * @see #flush
 * @see #reset
 */
public class BatchSqlUpdate extends SqlUpdate {

	/**
	 * Default number of inserts to accumulate before commiting a batch (5000).
	 */
	public static int DEFAULT_BATCH_SIZE = 5000;


	private int batchSize = DEFAULT_BATCH_SIZE;

	private boolean trackRowsAffected = true;

	private final LinkedList parameterQueue = new LinkedList();

	private final List rowsAffected = new ArrayList();


	/**
	 * Constructor to allow use as a JavaBean. DataSource and SQL
	 * must be supplied before compilation and use.
	 * @see #setDataSource
	 * @see #setSql
	 */
	public BatchSqlUpdate() {
		super();
	}

	/**
	 * Construct an update object with a given DataSource and SQL.
	 * @param ds DataSource to use to obtain connections
	 * @param sql SQL statement to execute
	 */
	public BatchSqlUpdate(DataSource ds, String sql) {
		super(ds, sql);
	}

	/**
	 * Construct an update object with a given DataSource, SQL
	 * and anonymous parameters.
	 * @param ds DataSource to use to obtain connections
	 * @param sql SQL statement to execute
	 * @param types SQL types of the parameters, as defined in the
	 * <code>java.sql.Types</code> class
	 * @see java.sql.Types
	 */
	public BatchSqlUpdate(DataSource ds, String sql, int[] types) {
		super(ds, sql, types);
	}

	/**
	 * Construct an update object with a given DataSource, SQL,
	 * anonymous parameters and specifying the maximum number of rows
	 * that may be affected.
	 * @param ds DataSource to use to obtain connections
	 * @param sql SQL statement to execute
	 * @param types SQL types of the parameters, as defined in the
	 * <code>java.sql.Types</code> class
	 * @param batchSize the number of statements that will trigger
	 * an automatic intermediate flush
	 * @see java.sql.Types
	 */
	public BatchSqlUpdate(DataSource ds, String sql, int[] types, int batchSize) {
		super(ds, sql, types);
		setBatchSize(batchSize);
	}


	/**
	 * Set the number of statements that will trigger an automatic intermediate
	 * flush. <code>update</code> calls or the given statement parameters will
	 * be queued until the batch size is met, at which point it will empty the
	 * queue and execute the batch.
	 * <p>You can also flush already queued statements with an explicit
	 * <code>flush</code> call. Note that you need to this after queueing
	 * all parameters to guarantee that all statements have been flushed.
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * Set whether to track the rows affected by batch updates performed
	 * by this operation object.
	 * <p>Default is "true". Turn this off to save the memory needed for
	 * the list of row counts.
	 * @see #getRowsAffected()
	 */
	public void setTrackRowsAffected(boolean trackRowsAffected) {
		this.trackRowsAffected = trackRowsAffected;
	}

	/**
	 * BatchSqlUpdate does not support BLOB or CLOB parameters.
	 */
	protected boolean supportsLobParameters() {
		return false;
	}


	/**
	 * Overridden version of <code>update</code> that adds the given statement
	 * parameters to the queue rather than executing them immediately.
	 * All other <code>update</code> methods of the SqlUpdate base class go
	 * through this method and will thus behave similarly.
	 * <p>You need to call <code>flush</code> to actually execute the batch.
	 * If the specified batch size is reached, an implicit flush will happen;
	 * you still need to finally call <code>flush</code> to flush all statements.
	 * @param args array of object arguments
	 * @return the number of rows affected by the update (always -1,
	 * meaning "not applicable", as the statement is not actually
	 * executed by this method)
	 * @see #flush
	 */
	public int update(Object[] args) throws DataAccessException {
		validateParameters(args);
		this.parameterQueue.add(args.clone());

		if (this.parameterQueue.size() == this.batchSize) {
			if (logger.isDebugEnabled()) {
				logger.debug("Triggering auto-flush because queue reached batch size of " + this.batchSize);
			}
			flush();
		}

		return -1;
	}

	/**
	 * Trigger any queued update operations to be added as a final batch.
	 * @return an array of the number of rows affected by each statement
	 */
	public int[] flush() {
		if (this.parameterQueue.isEmpty()) {
			return new int[0];
		}
		
		int[] rowsAffected = getJdbcTemplate().batchUpdate(
				getSql(),
				new BatchPreparedStatementSetter() {
					public int getBatchSize() {
						return parameterQueue.size();
					}
					public void setValues(PreparedStatement ps, int index) throws SQLException {
						Object[] params = (Object[]) parameterQueue.removeFirst();
						newPreparedStatementSetter(params).setValues(ps);
					}
				});

		if (this.trackRowsAffected) {
			for (int i = 0; i < rowsAffected.length; i++) {
				this.rowsAffected.add(new Integer(rowsAffected[i]));
			}
		}
		for (int i = 0; i < rowsAffected.length; i++) {
			checkRowsAffected(rowsAffected[i]);
		}
		return rowsAffected;
	}

	/**
	 * Return the current number of statements or statement parameters
	 * in the queue.
	 */
	public int getQueueCount() {
		return this.parameterQueue.size();
	}

	/**
	 * Return the number of already executed statements.
	 */
	public int getExecutionCount() {
		return this.rowsAffected.size();
	}

	/**
	 * Return the number of affected rows for all already executed statements.
	 * Accumulates all of <code>flush</code>'s return values until
	 * <code>reset</code> is invoked.
	 * @return an array of the number of rows affected by each statement
	 * @see #reset
	 */
	public int[] getRowsAffected() {
		int[] result = new int[this.rowsAffected.size()];
		int i = 0;
		for (Iterator it = this.rowsAffected.iterator(); it.hasNext(); i++) {
			Integer rowCount = (Integer) it.next();
			result[i] = rowCount.intValue();
		}
		return result;
	}

	/**
	 * Reset the statement parameter queue, the rows affected cache,
	 * and the execution count.
	 */
	public void reset() {
		this.parameterQueue.clear();
		this.rowsAffected.clear();
	}

}
