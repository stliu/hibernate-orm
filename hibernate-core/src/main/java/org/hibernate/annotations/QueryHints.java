/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.annotations;

import javax.persistence.*;

/**
 * Consolidation of hints available to Hibernate JPA queries.  Mainly used to define features available on
 * Hibernate queries that have no corollary in JPA queries.
 */
@javax.persistence.NamedQuery( name = "", query = "", hints = @QueryHint( name = QueryHints.CACHE_MODE, value = "sdfds"))
public class QueryHints {
	/**
	 * Disallow instantiation.
	 */
	private QueryHints() {
	}

	/**
	 * The cache mode to use.
	 *
	 * @see org.hibernate.Query#setCacheMode
	 * @see org.hibernate.SQLQuery#setCacheMode
	 */
	public static final String CACHE_MODE = "org.hibernate.cacheMode";

	/**
	 * The cache region to use.
	 *
	 * @see org.hibernate.Query#setCacheRegion
	 * @see org.hibernate.SQLQuery#setCacheRegion
	 */
	public static final String CACHE_REGION = "org.hibernate.cacheRegion";

	/**
	 * Are the query results cacheable?
	 *
	 * @see org.hibernate.Query#setCacheable
	 * @see org.hibernate.SQLQuery#setCacheable
	 */
	public static final String CACHEABLE = "org.hibernate.cacheable";

	/**
	 * Is the query callable?  Note: only valid for named native sql queries.
	 */
	public static final String CALLABLE = "org.hibernate.callable";

	/**
	 * Defines a comment to be applied to the SQL sent to the database.
	 *
	 * @see org.hibernate.Query#setComment
	 * @see org.hibernate.SQLQuery#setComment
	 */
	public static final String COMMENT = "org.hibernate.comment";

	/**
	 * Defines the JDBC fetch size to use.
	 *
	 * @see org.hibernate.Query#setFetchSize
	 * @see org.hibernate.SQLQuery#setFetchSize
	 */
	public static final String FETCH_SIZE = "org.hibernate.fetchSize";

	/**
	 * The flush mode to associate with the execution of the query.
	 *
	 * @see org.hibernate.Query#setFlushMode
	 * @see org.hibernate.SQLQuery#setFlushMode
	 * @see org.hibernate.Session#setFlushMode
	 */
	public static final String FLUSH_MODE = "org.hibernate.flushMode";

	/**
	 * Should entities returned from the query be set in read only mode?
	 *
	 * @see org.hibernate.Query#setReadOnly
	 * @see org.hibernate.SQLQuery#setReadOnly
	 * @see org.hibernate.Session#setReadOnly
	 */
	public static final String READ_ONLY = "org.hibernate.readOnly";

	/**
	 * Apply a Hibernate query timeout, which is defined in <b>seconds</b>.
	 *
	 * @see org.hibernate.Query#setTimeout
	 * @see org.hibernate.SQLQuery#setTimeout
	 */
	public static final String TIMEOUT_HIBERNATE = "org.hibernate.timeout";

	/**
	 * Apply a JPA query timeout, which is defined in <b>milliseconds</b>.
	 */
	public static final String TIMEOUT_JPA = "javax.persistence.query.timeout";

	/**
	 * Available to apply lock mode to a native SQL query since JPA requires that
	 * {@link javax.persistence.Query#setLockMode} throw an IllegalStateException if called for a native query.
	 * <p/>
	 * Accepts a {@link javax.persistence.LockModeType} or a {@link org.hibernate.LockMode}
	 */
	public static final String NATIVE_LOCKMODE = "org.hibernate.lockMode";
	
	/**
	 * Hint providing an EntityGraph.  With JPQL/HQL, the sole functionality is attribute nodes are treated as
	 * FetchType.EAGER.  Laziness is not affected.
	 */
	public static final String FETCHGRAPH = "javax.persistence.fetchgraph";
	
	/**
	 * Hint providing an EntityGraph.  With JPQL/HQL, the sole functionality is attribute nodes are treated as
	 * FetchType.EAGER.  Laziness is not affected.
	 */
	public static final String LOADGRAPH = "javax.persistence.loadgraph";

}
