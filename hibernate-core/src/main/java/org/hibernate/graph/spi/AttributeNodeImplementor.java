package org.hibernate.graph.spi;

import javax.persistence.AttributeNode;
import javax.persistence.metamodel.Attribute;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public interface AttributeNodeImplementor<T> extends AttributeNode<T> {
	public Attribute<?,T> getAttribute();
	public AttributeNodeImplementor<T> makeImmutableCopy();
}
