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

import java.util.List;
import java.util.Map;
import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;

import org.hibernate.jpa.graph.internal.AbstractGraphNode;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.graph.internal.SubgraphImpl;
import org.hibernate.jpa.graph.spi.AttributeNodeImplementor;

/**
 * @author Strong Liu
 */
public class EntityGraphWalker {
	private final EntityGraphVisitationStrategy strategy;

	public EntityGraphWalker(
			EntityGraphVisitationStrategy strategy) {
		this.strategy = strategy;
	}


	public static <T> void visit(EntityGraphVisitationStrategy strategy, EntityGraphImpl<T> entityGraph) {
		strategy.start();
		try {
			new EntityGraphWalker( strategy ).visitEntity( entityGraph );
		}
		finally {
			strategy.finish();
		}

	}

	private <T> void visitEntity(final EntityGraphImpl<T> entityGraph) {
		strategy.startingEntity( entityGraph );
		visitAttributeNodes( entityGraph );
		strategy.finishingEntity( entityGraph );


	}

	private <T> void visitAttributeNodes(AbstractGraphNode<T> graphNode) {
		List<AttributeNodeImplementor<?>> nodeList = graphNode.attributeImplementorNodes();
		for ( AttributeNode node : nodeList ) {
			visitAttributeNode( (AttributeNodeImplementor) node );
		}

	}

	private <T> void visitAttributeNode(AttributeNodeImplementor<T> attributeNode) {
		strategy.startingAttributeNode( attributeNode );
		Map<Class, Subgraph> subgraphMap = attributeNode.getSubgraphs();
		for ( Map.Entry<Class, Subgraph> entry : subgraphMap.entrySet() ) {
			visitSubgraph( entry.getKey(), (SubgraphImpl) entry.getValue() );
		}
		subgraphMap = attributeNode.getKeySubgraphs();
		for ( Map.Entry<Class, Subgraph> entry : subgraphMap.entrySet() ) {
			visitKeySubgraph( entry.getKey(), (SubgraphImpl) entry.getValue() );
		}
		strategy.finishingAttributeNode( attributeNode );

	}

	private <T> void visitSubgraph(Class clazz, SubgraphImpl<T> subgraph) {
		strategy.startingSubGraph( clazz, subgraph );
		visitAttributeNodes( subgraph );
		strategy.finishingSubGraph( clazz, subgraph );

	}

	private <T> void visitKeySubgraph(Class clazz, SubgraphImpl<T> subgraph) {
		strategy.startingKeySubGraph( clazz, subgraph );
		visitAttributeNodes( subgraph );
		strategy.finishingKeySubGraph( clazz, subgraph );
	}
}
