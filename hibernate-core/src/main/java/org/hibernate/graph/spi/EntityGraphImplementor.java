package org.hibernate.graph.spi;

import javax.persistence.EntityGraph;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public interface EntityGraphImplementor<T> extends EntityGraph<T> , GraphNodeImplementor{
}
