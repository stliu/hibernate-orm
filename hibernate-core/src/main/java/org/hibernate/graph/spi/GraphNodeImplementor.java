package org.hibernate.graph.spi;

import java.util.List;
import javax.persistence.AttributeNode;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public interface GraphNodeImplementor {
	List<AttributeNodeImplementor<?>> attributeImplementorNodes();
	List<AttributeNode<?>> attributeNodes();
}
