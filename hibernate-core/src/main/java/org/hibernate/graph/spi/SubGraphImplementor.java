package org.hibernate.graph.spi;

import javax.persistence.Subgraph;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public interface SubGraphImplementor<T> extends Subgraph<T>, GraphNodeImplementor {
}
