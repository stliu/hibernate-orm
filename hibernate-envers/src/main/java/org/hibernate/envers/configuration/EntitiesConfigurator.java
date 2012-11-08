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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.jboss.jandex.IndexView;

import org.hibernate.MappingException;
import org.hibernate.envers.configuration.metadata.AuditEntityNameRegister;
import org.hibernate.envers.configuration.metadata.AuditMetadataGenerator;
import org.hibernate.envers.configuration.metadata.EntityXmlMappingData;
import org.hibernate.envers.configuration.metadata.reader.AnnotationsMetadataReader;
import org.hibernate.envers.configuration.metadata.reader.ClassAuditingData;
import org.hibernate.envers.entities.EntitiesConfigurations;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.tools.StringTools;
import org.hibernate.envers.tools.Tools;
import org.hibernate.envers.tools.graph.GraphTopologicalSort;
import org.hibernate.jaxb.spi.JaxbRoot;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.jaxb.spi.hbm.JaxbKeyManyToOneElement;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.EntityBinding;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class EntitiesConfigurator {
    public EntitiesConfigurations configure(CoreConfiguration coreConfiguration, GlobalConfiguration globalCfg,
											AuditEntitiesConfiguration verEntCfg, AuditStrategy auditStrategy,
											JaxbRoot revisionInfoMapping, JaxbKeyManyToOneElement revisionInfoRelationMapping) {
        // Creating a name register to capture all audit entity names created.
        AuditEntityNameRegister auditEntityNameRegister = new AuditEntityNameRegister();

        // Sorting the persistent class topologically - superclass always before subclass
		Iterator<EntityBinding> entityBindings = GraphTopologicalSort.sort(
				new PersistentClassGraphDefiner( coreConfiguration.getMetadata() )
		).iterator();

        ClassesAuditingData classesAuditingData = new ClassesAuditingData();
        Map<EntityBinding, ClassAuditingData> jaxbMappings = new HashMap<EntityBinding, ClassAuditingData>();

        // Reading metadata from annotations
        while (entityBindings.hasNext()) {
            EntityBinding entityBinding = entityBindings.next();

            // Collecting information from annotations on the persistent class pc
			AnnotationsMetadataReader annotationsMetadataReader = new AnnotationsMetadataReader(
					globalCfg, coreConfiguration,
					coreConfiguration.locateClassInfo( entityBinding.getClassReference().getName() ),
					entityBinding
			);
            ClassAuditingData auditData = annotationsMetadataReader.getAuditData();

			classesAuditingData.addClassAuditingData( entityBinding, auditData );
        }

        // Now that all information is read we can update the calculated fields.
        classesAuditingData.updateCalculatedFields();

//       TODO: AuditMetadataGenerator auditMetaGen = new AuditMetadataGenerator(metadata, globalCfg, verEntCfg, auditStrategy,
//                revisionInfoRelationMapping, auditEntityNameRegister);

        // First pass
        for (Map.Entry<EntityBinding, ClassAuditingData> pcDatasEntry : classesAuditingData.getAllClassAuditedData()) {
			EntityBinding entityBinding = pcDatasEntry.getKey();
            ClassAuditingData auditData = pcDatasEntry.getValue();

            EntityXmlMappingData xmlMappingData = new EntityXmlMappingData();
            if (auditData.isAudited()) {
                if (!StringTools.isEmpty(auditData.getAuditTable().value())) {
                    verEntCfg.addCustomAuditTableName(entityBinding.getEntity().getName(), auditData.getAuditTable().value());
                }

//                TODO: auditMetaGen.generateFirstPass(pc, auditData, xmlMappingData, true);
			} else {
//				TODO: auditMetaGen.generateFirstPass(pc, auditData, xmlMappingData, false);
			}

//            TODO: xmlMappings.put(pc, xmlMappingData);
        }

        // Second pass
        for (Map.Entry<EntityBinding, ClassAuditingData> pcDatasEntry : classesAuditingData.getAllClassAuditedData()) {
            EntityXmlMappingData xmlMappingData = null; // TODO: xmlMappings.get(pcDatasEntry.getKey());

            if (pcDatasEntry.getValue().isAudited()) {
//                TODO: auditMetaGen.generateSecondPass(pcDatasEntry.getKey(), pcDatasEntry.getValue(), xmlMappingData);
//                try {
//                    TODO: cfg.addDocument(writer.write(xmlMappingData.getMainXmlMapping()));

                    for (Document additionalMapping : xmlMappingData.getAdditionalXmlMappings()) {
//                        TODO: cfg.addDocument(writer.write(additionalMapping));
                    }
//                } catch (DocumentException e) {
//                    throw new MappingException(e);
//                }
            }
        }

        // Only if there are any versioned classes
//       TODO: if (auditMetaGen.getEntitiesConfigurations().size() > 0) {
//            try {
//                if (revisionInfoMapping !=  null) {
//                    cfg.addDocument(writer.write(revisionInfoXmlMapping));
//                }
//            } catch (DocumentException e) {
//                throw new MappingException(e);
//            }
//        }
//
//		return new EntitiesConfigurations(auditMetaGen.getEntitiesConfigurations(),
//				auditMetaGen.getNotAuditedEntitiesConfigurations());
		return null;
    }
}
