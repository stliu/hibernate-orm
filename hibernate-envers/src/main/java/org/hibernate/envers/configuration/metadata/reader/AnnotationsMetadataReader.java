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
package org.hibernate.envers.configuration.metadata.reader;
import java.lang.annotation.Annotation;
import java.util.Iterator;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.ModificationStore;
import org.hibernate.envers.SecondaryAuditTable;
import org.hibernate.envers.SecondaryAuditTables;
import org.hibernate.envers.configuration.CoreConfiguration;
import org.hibernate.envers.configuration.GlobalConfiguration;
import org.hibernate.envers.event.EnversDotNames;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.EntityBinding;

/**
 * A helper class to read versioning meta-data from annotations on a persistent class.
 * @author Adam Warski (adam at warski dot org)
 * @author Sebastian Komander
 */
public final class AnnotationsMetadataReader {
	private final GlobalConfiguration globalCfg;
	private final ClassInfo classInfo;
	private final EntityBinding entityBinding;
	private final CoreConfiguration coreConfiguration;
	private final ClassAuditingData auditData;

	public AnnotationsMetadataReader(GlobalConfiguration globalCfg, CoreConfiguration coreConfiguration,
									 ClassInfo classInfo, EntityBinding entityBinding) {
		this.globalCfg = globalCfg;
		this.classInfo = classInfo;
		this.entityBinding = entityBinding;
		this.coreConfiguration = coreConfiguration;
		this.auditData = new ClassAuditingData();
	}

	private ModificationStore getDefaultAudited(ClassInfo classInfo) {
		final AnnotationInstance audited = JandexHelper.getSingleAnnotation( classInfo, EnversDotNames.AUDITED );
		if ( audited != null ) {
			return JandexHelper.getValue( audited, "modStore", ModificationStore.class );
		}
		return null;
	}

	private void addAuditTable(ClassInfo classInfo) {
		final AnnotationInstance auditTable = JandexHelper.getSingleAnnotation( classInfo, EnversDotNames.AUDIT_TABLE );
		if ( auditTable != null ) {
			auditData.setAuditTable( coreConfiguration.createAnnotationProxy( auditTable, AuditTable.class ) );
		}
		else {
			auditData.setAuditTable( getDefaultAuditTable() );
		}
	}

	private void addAuditSecondaryTables(ClassInfo classInfo) {
		// Getting information on secondary tables
		final AnnotationInstance secondaryAuditTable1 = JandexHelper.getSingleAnnotation( classInfo, EnversDotNames.SECONDARY_AUDIT_TABLE );
		if ( secondaryAuditTable1 != null ) {
			auditData.getSecondaryTableDictionary().put(
					JandexHelper.getValue( secondaryAuditTable1, "secondaryTableName", String.class ),
					JandexHelper.getValue( secondaryAuditTable1, "secondaryAuditTableName", String.class )
			);
		}

		final AnnotationInstance secondaryAuditTables = JandexHelper.getSingleAnnotation( classInfo, EnversDotNames.SECONDARY_AUDIT_TABLES );
		if ( secondaryAuditTables != null ) {
			for ( AnnotationInstance secondaryAuditTable2 : JandexHelper.getValue( secondaryAuditTables, "value", AnnotationInstance[].class ) ) {
				auditData.getSecondaryTableDictionary().put(
						JandexHelper.getValue( secondaryAuditTable2, "secondaryTableName", String.class ),
						JandexHelper.getValue( secondaryAuditTable2, "secondaryAuditTableName", String.class )
				);
			}
		}
	}

	public ClassAuditingData getAuditData() {
		if ( entityBinding.getClassReference() == null ) {
			// TODO: What is the case here? Test by throwing exception.
			return auditData;
		}

		ModificationStore defaultStore = getDefaultAudited( classInfo );
		auditData.setDefaultAudited( defaultStore != null );

		new AuditedPropertiesReader( coreConfiguration, globalCfg, entityBinding, classInfo, auditData, defaultStore, "" ).read();

		addAuditTable( classInfo );
		addAuditSecondaryTables( classInfo );

		return auditData;
	}

	private AuditTable defaultAuditTable = new AuditTable() {
		public String value() { return ""; }
		public String schema() { return ""; }
		public String catalog() { return ""; }
		public Class<? extends Annotation> annotationType() { return this.getClass(); }
	};

	private AuditTable getDefaultAuditTable() {
		return defaultAuditTable;
	}
}
