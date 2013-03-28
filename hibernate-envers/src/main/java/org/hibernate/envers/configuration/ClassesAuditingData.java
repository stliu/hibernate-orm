package org.hibernate.envers.configuration;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.MappingException;
import org.hibernate.envers.configuration.metadata.reader.ClassAuditingData;
import org.hibernate.envers.configuration.metadata.reader.PropertyAuditingData;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.tools.MappingTools;
import org.hibernate.metamodel.spi.binding.EntityBinding;

/**
 * A helper class holding auditing meta-data for all persistent classes.
 * @author Adam Warski (adam at warski dot org)
 */
public class ClassesAuditingData {
    public static final EnversMessageLogger LOG = Logger.getMessageLogger(EnversMessageLogger.class, ClassesAuditingData.class.getName());

    private final Map<String, ClassAuditingData> entityNameToAuditingData = new HashMap<String, ClassAuditingData>();
    private final Map<EntityBinding, ClassAuditingData> persistentClassToAuditingData = new LinkedHashMap<EntityBinding, ClassAuditingData>();

    /**
     * Stores information about auditing meta-data for the given class.
     * @param entityBinding Persistent class.
     * @param cad Auditing meta-data for the given class.
     */
    public void addClassAuditingData(EntityBinding entityBinding, ClassAuditingData cad) {
        entityNameToAuditingData.put(entityBinding.getEntity().getName(), cad);
        persistentClassToAuditingData.put(entityBinding, cad);
    }

    /**
     * @return A collection of all auditing meta-data for persistent classes.
     */
    public Collection<Map.Entry<EntityBinding, ClassAuditingData>> getAllClassAuditedData() {
        return persistentClassToAuditingData.entrySet();
    }

    /**
     * @param entityName Name of the entity.
     * @return Auditing meta-data for the given entity.
     */
    public ClassAuditingData getClassAuditingData(String entityName) {
        return entityNameToAuditingData.get(entityName);
    }

    /**
     * After all meta-data is read, updates calculated fields. This includes:
     * <ul>
     * <li>setting {@code forceInsertable} to {@code true} for properties specified by {@code @AuditMappedBy}</li>
     * </ul>
     */
    public void updateCalculatedFields() {
        for (Map.Entry<EntityBinding, ClassAuditingData> classAuditingDataEntry : persistentClassToAuditingData.entrySet()) {
			EntityBinding entityBinding = classAuditingDataEntry.getKey();
            ClassAuditingData classAuditingData = classAuditingDataEntry.getValue();
            for (String propertyName : classAuditingData.getPropertyNames()) {
                PropertyAuditingData propertyAuditingData = classAuditingData.getPropertyAuditingData(propertyName);
                // If a property had the @AuditMappedBy annotation, setting the referenced fields to be always insertable.
                if (propertyAuditingData.getAuditMappedBy() != null) {
//                   TODO: String referencedEntityName = MappingTools.getReferencedEntityName(pc.getProperty(propertyName).getValue());
					final String referencedEntityName = MappingTools.getReferencedEntityName( entityBinding.locateAttributeBinding( propertyName ) );

                    ClassAuditingData referencedClassAuditingData = entityNameToAuditingData.get(referencedEntityName);

                    forcePropertyInsertable(referencedClassAuditingData, propertyAuditingData.getAuditMappedBy(),
							entityBinding.getEntity().getName(), referencedEntityName);

                    forcePropertyInsertable(referencedClassAuditingData, propertyAuditingData.getPositionMappedBy(),
							entityBinding.getEntity().getName(), referencedEntityName);
                }
            }
        }
    }

    private void forcePropertyInsertable(ClassAuditingData classAuditingData, String propertyName,
                                         String entityName, String referencedEntityName) {
        if (propertyName != null) {
            if (classAuditingData.getPropertyAuditingData(propertyName) == null) {
                throw new MappingException("@AuditMappedBy points to a property that doesn't exist: " +
                    referencedEntityName + "." + propertyName);
            }

            LOG.debugf("Non-insertable property %s.%s will be made insertable because a matching @AuditMappedBy was found in the %s entity",
                       referencedEntityName,
                       propertyName,
                       entityName);

            classAuditingData
                    .getPropertyAuditingData(propertyName)
                    .setForceInsertable(true);
        }
    }
}
