/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.configuration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.cfg.Configuration;
import org.hibernate.envers.tools.Tools;
import org.hibernate.envers.tools.graph.GraphDefiner;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.EntityBinding;

/**
 * Defines a graph, where the vertexes are all persistent classes, and there is an edge from
 * p.c. A to p.c. B iff A is a superclass of B.
 * @author Adam Warski (adam at warski dot org)
 */
public class PersistentClassGraphDefiner implements GraphDefiner<EntityBinding, String> {
	private MetadataImplementor metadata;

	public PersistentClassGraphDefiner(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	public String getRepresentation(EntityBinding entityBinding) {
		return entityBinding.getEntity().getName();
	}

	public EntityBinding getValue(String entityName) {
		return metadata.getEntityBinding( entityName );
	}

	private void addNeighbours(List<EntityBinding> neighbours, Iterator<EntityBinding> subclassIterator) {
		while ( subclassIterator.hasNext() ) {
			EntityBinding subclass = subclassIterator.next();
			neighbours.add( subclass );
			addNeighbours( neighbours, subclass.getDirectSubEntityBindings().iterator() );
		}
	}

	public List<EntityBinding> getNeighbours(EntityBinding entityBinding) {
		List<EntityBinding> neighbours = new ArrayList<EntityBinding>();

		addNeighbours( neighbours, entityBinding.getDirectSubEntityBindings().iterator() );

		return neighbours;
	}

	public List<EntityBinding> getValues() {
		return Tools.iteratorToList( metadata.getEntityBindings().iterator() );
	}
}
