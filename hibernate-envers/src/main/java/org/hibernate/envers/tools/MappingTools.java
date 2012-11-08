package org.hibernate.envers.tools;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class MappingTools {	
	/**
	 * @param componentName Name of the component, that is, name of the property in the entity that references the
	 * component.
	 * @return A prefix for properties in the given component.
	 */
	public static String createComponentPrefix(String componentName) {
		return componentName + "_";
	}

    /**
     * @param referencePropertyName The name of the property that holds the relation to the entity.
     * @return A prefix which should be used to prefix an id mapper for the related entity.
     */
    public static String createToOneRelationPrefix(String referencePropertyName) {
        return referencePropertyName + "_";
    }

    public static String getReferencedEntityName(Value value) {
        if (value instanceof ToOne) {
            return ((ToOne) value).getReferencedEntityName();
        } else if (value instanceof OneToMany) {
            return ((OneToMany) value).getReferencedEntityName();
        } else if (value instanceof Collection) {
            return getReferencedEntityName(((Collection) value).getElement());
        }

        return null;
    }

	public static String getReferencedEntityName(AttributeBinding attributeBinding) {
		if ( attributeBinding instanceof ManyToOneAttributeBinding ) {
			// TODO: is the above type correct? What about one-to-one mappings?
			return ( (ManyToOneAttributeBinding) attributeBinding ).getReferencedEntityName();
		}
//		TODO: OneToMany mapping?
//		else if ( attributeBinding instanceof  ) {
//		}
		else if ( attributeBinding instanceof CompositeAttributeBinding ) {
//			TODO: What to return here?
			return null;
		}
		return null;
	}
}
