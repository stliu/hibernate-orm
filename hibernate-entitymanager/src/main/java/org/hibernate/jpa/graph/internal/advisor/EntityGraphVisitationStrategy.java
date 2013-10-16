/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

package org.hibernate.jpa.graph.internal.advisor;

import javax.persistence.EntityManagerFactory;

import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.graph.internal.SubgraphImpl;
import org.hibernate.jpa.graph.spi.AttributeNodeImplementor;

/**
 * @author Strong Liu
 */
public abstract class EntityGraphVisitationStrategy {
	private FetchProfile fetchProfile;
	private HibernateEntityManagerFactory emf;

	public FetchProfile buildFetchProfile() {
		return fetchProfile;
	}

	public void finish() {
	}

	public void start() {
	}


	public <T> void startingEntity(EntityGraphImpl<T> entityGraph) {
		emf = entityGraph.entityManagerFactory();
		fetchProfile = new FetchProfile( entityGraph.getName() );


	}

	public <T> void finishingEntity(EntityGraphImpl<T> entityGraph) {
	}

	public <T> void startingAttributeNode(final AttributeNodeImplementor<T> attributeNode) {


	}

	public <T> void finishingAttributeNode(final AttributeNodeImplementor<T> attributeNode) {

	}

	public <T> void finishingKeySubGraph(final Class clazz, final SubgraphImpl<T> subgraph) {

	}

	public <T> void startingKeySubGraph(final Class clazz, final SubgraphImpl<T> subgraph) {

	}

	public <T> void finishingSubGraph(final Class clazz, final SubgraphImpl<T> subgraph) {

	}

	public <T> void startingSubGraph(final Class clazz, final SubgraphImpl<T> subgraph) {

	}

	protected HibernateEntityManagerFactory emf() {
		return emf;
	}
}
