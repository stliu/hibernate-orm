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

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.configuration.metadata.AuditTableData;
import org.hibernate.envers.configuration.metadata.MetadataTools;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.event.EnversDotNames;
import org.hibernate.envers.revisioninfo.DefaultRevisionInfoGenerator;
import org.hibernate.envers.revisioninfo.DefaultTrackingModifiedEntitiesRevisionInfoGenerator;
import org.hibernate.envers.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.tools.MutableBoolean;
import org.hibernate.envers.tools.Tools;
import org.hibernate.jaxb.spi.JaxbRoot;
import org.hibernate.jaxb.spi.hbm.JaxbClassElement;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbElementElement;
import org.hibernate.jaxb.spi.hbm.JaxbFetchAttributeWithSubselect;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.jaxb.spi.hbm.JaxbIdElement;
import org.hibernate.jaxb.spi.hbm.JaxbKeyElement;
import org.hibernate.jaxb.spi.hbm.JaxbKeyManyToOneElement;
import org.hibernate.jaxb.spi.hbm.JaxbLazyAttributeWithExtra;
import org.hibernate.jaxb.spi.hbm.JaxbPropertyElement;
import org.hibernate.jaxb.spi.hbm.JaxbSetElement;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.SetBinding;
import org.hibernate.type.DateType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class RevisionInfoConfiguration {
    private EntityBinding revisionInfoEntityBinding;
	private ClassInfo revisionInfoEntityClassInfo;
	private Class revisionInfoEntityClass;
	private String revisionInfoEntityName;
    private PropertyData revisionInfoIdData;
    private PropertyData revisionInfoTimestampData;
    private PropertyData modifiedEntityNamesData;
    private Type revisionInfoTimestampType;
    private GlobalConfiguration globalCfg;

    private String revisionPropType;
    private String revisionPropSqlType;

	public RevisionInfoConfiguration(GlobalConfiguration globalCfg) {
        this.globalCfg = globalCfg;
		// TODO: Switch to SequenceIdRevisionEntity.
        if (globalCfg.isUseRevisionEntityWithNativeId()) {
			revisionInfoEntityName = "org.hibernate.envers.DefaultRevisionEntity";
        } else {
			revisionInfoEntityName = "org.hibernate.envers.enhanced.SequenceIdRevisionEntity";
        }
        revisionInfoIdData = new PropertyData("id", "id", "field", null);
        revisionInfoTimestampData = new PropertyData("timestamp", "timestamp", "field", null);
        modifiedEntityNamesData = new PropertyData("modifiedEntityNames", "modifiedEntityNames", "field", null);
        revisionInfoTimestampType = new LongType();

        revisionPropType = "integer";
    }

	private JaxbRoot generateDefaultRevisionInfoMapping() {
		JaxbHibernateMapping revisionInfoMapping = new JaxbHibernateMapping();
		revisionInfoMapping.setAutoImport( false );
		// Schema and catalog are specified on class level.
//		revisionInfoMapping.setSchema( globalCfg.getDefaultSchemaName() );
//		revisionInfoMapping.setCatalog( globalCfg.getDefaultCatalogName() );

		final JaxbClassElement entity = MetadataTools.createEntity(
				new AuditTableData( null, null, globalCfg.getDefaultSchemaName(), globalCfg.getDefaultCatalogName() ),
				null
		);
		entity.setName( revisionInfoEntityName );
		entity.setTable( "REVINFO" );

		final JaxbIdElement id = MetadataTools.addNativelyGeneratedId(
				entity, revisionInfoIdData.getName(),
				revisionPropType, globalCfg.isUseRevisionEntityWithNativeId()
		);
		MetadataTools.addColumn( id.getColumn(), "REV", null, null, null, null, null, null, false );

		final JaxbPropertyElement timestamp = MetadataTools.addProperty(
				entity.getProperty(), revisionInfoTimestampData.getName(),
				revisionInfoTimestampType.getName(), true
		);
		MetadataTools.addColumn( timestamp.getColumn(), "REVTSTMP", null, null, null, null, null, null, false );

		if ( globalCfg.isTrackEntitiesChangedInRevisionEnabled() ) {
			generateEntityNamesTrackingTableMapping(
					entity, "modifiedEntityNames",
					globalCfg.getDefaultSchemaName(), globalCfg.getDefaultCatalogName(),
					"REVCHANGES", "REV", "ENTITYNAME", "string"
			);
		}

		revisionInfoMapping.getClazz().add( entity );

		return new JaxbRoot( revisionInfoMapping, null );
	}

    /**
     * TODO: Update JavaDoc.
	 * Generates mapping that represents a set of primitive types.<br />
     * <code>
     * &lt;set name="propertyName" table="joinTableName" schema="joinTableSchema" catalog="joinTableCatalog"
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cascade="persist, delete" lazy="false" fetch="join"&gt;<br />
     * &nbsp;&nbsp;&nbsp;&lt;key column="joinTablePrimaryKeyColumnName" /&gt;<br />
     * &nbsp;&nbsp;&nbsp;&lt;element type="joinTableValueColumnType"&gt;<br />
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;column name="joinTableValueColumnName" /&gt;<br />
     * &nbsp;&nbsp;&nbsp;&lt;/element&gt;<br />
     * &lt;/set&gt;
     * </code>
     */
	private void generateEntityNamesTrackingTableMapping(JaxbClassElement entity, String propertyName,
														 String joinTableSchema, String joinTableCatalog, String joinTableName,
														 String joinTablePrimaryKeyColumnName, String joinTableValueColumnName,
														 String joinTableValueColumnType) {
		final JaxbSetElement set = new JaxbSetElement();
		entity.getSet().add( set );

		set.setName( propertyName );
		set.setTable( joinTableName );
		set.setSchema( joinTableSchema );
		set.setCatalog( joinTableCatalog );
		set.setCascade( "persist, delete" );
		set.setFetch( JaxbFetchAttributeWithSubselect.JOIN );
		set.setLazy( JaxbLazyAttributeWithExtra.FALSE );

		final JaxbKeyElement key = new JaxbKeyElement();
		set.setKey( key );
		key.setColumnAttribute( joinTablePrimaryKeyColumnName );

		final JaxbElementElement element = new JaxbElementElement();
		set.setElement( element );
		element.setTypeAttribute( joinTableValueColumnType );

		final JaxbColumnElement column = new JaxbColumnElement();
		element.getColumn().add( column );
		column.setName( joinTableValueColumnName );
	}

	private JaxbKeyManyToOneElement generateRevisionInfoRelationMapping() {
		final JaxbKeyManyToOneElement revisionRelation = new JaxbKeyManyToOneElement();
		// TODO: rev_rel_mapping.addAttribute("type", revisionPropType);
		revisionRelation.setClazz( revisionInfoEntityName );

		if ( revisionPropSqlType != null ) {
			MetadataTools.addColumn( revisionRelation.getColumn(), "*" , null, null, null, revisionPropSqlType, null, null, false );
		}

		return revisionRelation;
	}

	private void searchForRevisionNumberCfg(IndexView jandexIndex, MutableBoolean revisionNumberFound, MetadataImplementor metadata) {
		for ( AnnotationInstance annotation : jandexIndex.getAnnotations( EnversDotNames.REVISION_NUMBER ) ) {
			AnnotationTarget annotationTarget = annotation.target();
			if ( !( annotationTarget instanceof FieldInfo || annotationTarget instanceof MethodInfo ) ) {
				throw new MappingException( "@RevisionNumber is applicable only to fields or properties." );
			}
			if ( Tools.isFieldOrPropertyOfClass( annotationTarget, revisionInfoEntityClassInfo, jandexIndex ) ) {
				if ( revisionNumberFound.isSet() ) {
					throw new MappingException( "Only one property may be annotated with @RevisionNumber." );
				}

				final String revisionNumberProperty = JandexHelper.getPropertyName( annotationTarget );
				final AttributeBinding revisionNumberAttribute = revisionInfoEntityBinding.locateAttributeBinding(
						revisionNumberProperty
				);
				HibernateTypeDescriptor revisionNumberType = revisionNumberAttribute.getHibernateTypeDescriptor();
				// TODO: Check whether it is required to verify HibernateTypeDescriptor#getJavaTypeName()?
				if ( revisionNumberType.getResolvedTypeMapping() instanceof IntegerType ) {
					revisionInfoIdData = new PropertyData(
							revisionNumberProperty,
							revisionNumberProperty,
							revisionNumberAttribute.getPropertyAccessorName(),
							null
					);
					revisionNumberFound.set();
				}
				else if ( revisionNumberType.getResolvedTypeMapping() instanceof LongType ) {
					revisionInfoIdData = new PropertyData(
							revisionNumberProperty,
							revisionNumberProperty,
							revisionNumberAttribute.getPropertyAccessorName(),
							null
					);
					revisionNumberFound.set();
					// The default is integer.
					revisionPropType = "long";
				}
				else {
					throw new MappingException(
							"Field annotated with @RevisionNumber must be of type int, Integer, long or Long."
					);
				}

				// Getting the @Column definition of the revision number property, to later use that info to
				// generate the same mapping for the relation from an audit table's revision number to the
				// revision entity revision number.
				final AnnotationInstance jpaColumnAnnotation = JandexHelper.getSingleAnnotation(
						JandexHelper.getMemberAnnotations( revisionInfoEntityClassInfo, revisionNumberProperty, metadata.getServiceRegistry()),
						JPADotNames.COLUMN
				);
				if ( jpaColumnAnnotation != null ) {
					revisionPropSqlType = JandexHelper.getValue( jpaColumnAnnotation, "columnDefinition", String.class );
				}
			}
		}
	}

	private void searchForRevisionTimestampCfg(IndexView jandexIndex, MutableBoolean revisionTimestampFound) {
		for ( AnnotationInstance annotation : jandexIndex.getAnnotations( EnversDotNames.REVISION_TIMESTAMP ) ) {
			AnnotationTarget annotationTarget = annotation.target();
			if ( !( annotationTarget instanceof FieldInfo || annotationTarget instanceof MethodInfo ) ) {
				throw new MappingException( "@RevisionTimestamp is applicable only to fields or properties." );
			}
			if ( Tools.isFieldOrPropertyOfClass( annotationTarget, revisionInfoEntityClassInfo, jandexIndex ) ) {
				if ( revisionTimestampFound.isSet() ) {
					throw new MappingException( "Only one property may be annotated with @RevisionTimestamp." );
				}

				final String revisionTimestampProperty = JandexHelper.getPropertyName( annotationTarget );
				final AttributeBinding revisionTimestampAttribute = revisionInfoEntityBinding.locateAttributeBinding(
						revisionTimestampProperty
				);
				HibernateTypeDescriptor revisionTimestampType = revisionTimestampAttribute.getHibernateTypeDescriptor();
				// TODO: Check whether it is required to verify HibernateTypeDescriptor#getJavaTypeName()?
				if ( revisionTimestampType.getResolvedTypeMapping() instanceof LongType || revisionTimestampType.getResolvedTypeMapping() instanceof DateType ) {
					revisionInfoTimestampData = new PropertyData(
							revisionTimestampProperty,
							revisionTimestampProperty,
							revisionTimestampAttribute.getPropertyAccessorName(),
							null
					);
					revisionTimestampFound.set();
				}
				else {
					throw new MappingException(
							"Field annotated with @RevisionTimestamp must be of type long, Long, java.util.Date or java.sql.Date."
					);
				}
			}
		}
	}

	private void searchForModifiedEntityNamesCfg(IndexView jandexIndex, MutableBoolean modifiedEntityNamesFound) {
		for ( AnnotationInstance annotation : jandexIndex.getAnnotations( EnversDotNames.MODIFIED_ENTITY_NAMES ) ) {
			AnnotationTarget annotationTarget = annotation.target();
			if ( !( annotationTarget instanceof FieldInfo || annotationTarget instanceof MethodInfo ) ) {
				throw new MappingException( "@ModifiedEntityNames is applicable only to fields or properties." );
			}
			if ( Tools.isFieldOrPropertyOfClass( annotationTarget, revisionInfoEntityClassInfo, jandexIndex ) ) {
				if ( modifiedEntityNamesFound.isSet() ) {
					throw new MappingException( "Only one property may be annotated with @ModifiedEntityNames." );
				}

				final String modifiedEntityNamesProperty = JandexHelper.getPropertyName( annotationTarget );
				final AttributeBinding modifiedEntityNamesAttribute = revisionInfoEntityBinding.locateAttributeBinding(
						modifiedEntityNamesProperty
				);

				if ( modifiedEntityNamesAttribute instanceof SetBinding ) {
					final SetBinding collectionBinding = (SetBinding) modifiedEntityNamesAttribute;
					final String elementType = collectionBinding.getPluralAttributeElementBinding().getHibernateTypeDescriptor().getJavaTypeName();
					if ( String.class.getName().equals( elementType ) ) {
						modifiedEntityNamesData = new PropertyData(
								modifiedEntityNamesProperty,
								modifiedEntityNamesProperty,
								modifiedEntityNamesAttribute.getPropertyAccessorName(),
								null
						);
						modifiedEntityNamesFound.set();
					}
				}

				if ( !modifiedEntityNamesFound.isSet() ) {
					throw new MappingException(
							"Field annotated with @ModifiedEntityNames must be of Set<String> type."
					);
				}
			}
		}
	}

	private void searchForRevisionInfoCfg(IndexView jandexIndex, MutableBoolean revisionNumberFound,
										  MutableBoolean revisionTimestampFound, MutableBoolean modifiedEntityNamesFound,
										  MetadataImplementor metadata) {
		searchForRevisionNumberCfg( jandexIndex, revisionNumberFound, metadata );
		searchForRevisionTimestampCfg( jandexIndex, revisionTimestampFound );
		searchForModifiedEntityNamesCfg( jandexIndex, modifiedEntityNamesFound );
    }

	// TODO: Change class properties to method arguments. Think of object state.
    public RevisionInfoConfigurationResult configure(CoreConfiguration coreConfiguration) {
		final MetadataImplementor metadata = coreConfiguration.getMetadata();
		final IndexView jandexIndex = coreConfiguration.getJandexIndex();
		final ClassLoaderService classLoaderService = metadata.getServiceRegistry().getService( ClassLoaderService.class );
        RevisionInfoGenerator revisionInfoGenerator = null;

		// Locate @RevisionEntity provided by user and validate its mapping.
		Collection<AnnotationInstance> revisionEntityAnnotations = jandexIndex.getAnnotations( EnversDotNames.REVISION_ENTITY );
		if ( revisionEntityAnnotations.size() > 1 ) {
			throw new MappingException( "Only one entity may be annotated with @RevisionEntity." );
		}
		if ( revisionEntityAnnotations.size() == 1 ) {
			final AnnotationInstance revisionEntityAnnotation = revisionEntityAnnotations.iterator().next();
			revisionInfoEntityClassInfo = (ClassInfo) revisionEntityAnnotation.target();
			revisionInfoEntityClass = classLoaderService.classForName( revisionInfoEntityClassInfo.name().toString() );
			revisionInfoEntityBinding = coreConfiguration.locateEntityBinding( revisionInfoEntityClassInfo );
			revisionInfoEntityName = revisionInfoEntityBinding.getEntity().getName();
			if ( revisionInfoEntityClassInfo.annotations().containsKey( EnversDotNames.AUDITED ) ) {
				throw new MappingException( "An entity annotated with @RevisionEntity cannot be audited." );
			}

			MutableBoolean revisionNumberFound = new MutableBoolean();
			MutableBoolean revisionTimestampFound = new MutableBoolean();
			MutableBoolean modifiedEntityNamesFound = new MutableBoolean();

			searchForRevisionInfoCfg(
					jandexIndex,
					revisionNumberFound,
					revisionTimestampFound,
					modifiedEntityNamesFound,
					metadata
			);

			if ( !revisionNumberFound.isSet() ) {
				throw new MappingException(
						"An entity annotated with @RevisionEntity must encapsulate @RevisionNumber property."
				);
			}

			if ( !revisionTimestampFound.isSet() ) {
				throw new MappingException(
						"An entity annotated with @RevisionEntity must encapsulate @RevisionTimestamp property."
				);
			}

			Class<? extends RevisionListener> revisionListenerClass = getRevisionListenerClass(
					classLoaderService,
					revisionEntityAnnotation
			);
			revisionInfoTimestampType = revisionInfoEntityBinding.locateAttributeBinding( revisionInfoTimestampData.getName() )
					.getHibernateTypeDescriptor()
					.getResolvedTypeMapping();
			if ( globalCfg.isTrackEntitiesChangedInRevisionEnabled()
					|| ( globalCfg.isUseRevisionEntityWithNativeId() && DefaultTrackingModifiedEntitiesRevisionEntity.class.isAssignableFrom( revisionInfoEntityClass ) )
					|| ( !globalCfg.isUseRevisionEntityWithNativeId() && SequenceIdTrackingModifiedEntitiesRevisionEntity.class.isAssignableFrom( revisionInfoEntityClass ) )
					|| modifiedEntityNamesFound.isSet() ) {
				// If tracking modified entities parameter is enabled, custom revision info entity is a subtype
				// of DefaultTrackingModifiedEntitiesRevisionEntity class, or @ModifiedEntityNames annotation is used.
				revisionInfoGenerator = new DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
						revisionInfoEntityBinding.getEntity().getName(), revisionInfoEntityClass,
						revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate(), modifiedEntityNamesData
				);
				globalCfg.setTrackEntitiesChangedInRevisionEnabled( true );
			}
			else {
				revisionInfoGenerator = new DefaultRevisionInfoGenerator(
						revisionInfoEntityBinding.getEntity().getName(), revisionInfoEntityClass,
						revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate()
				);
			}
		}

        // In case of a custom revision info generator, the mapping will be null.
		JaxbRoot revisionInfoMapping = null;

		Class<? extends RevisionListener> revisionListenerClass = getRevisionListenerClass( classLoaderService, null );

		if ( revisionInfoGenerator == null ) {
			if ( globalCfg.isTrackEntitiesChangedInRevisionEnabled() ) {
				revisionInfoEntityClass = globalCfg.isUseRevisionEntityWithNativeId() ? DefaultTrackingModifiedEntitiesRevisionEntity.class
																					  : SequenceIdTrackingModifiedEntitiesRevisionEntity.class;
				revisionInfoEntityName = revisionInfoEntityClass.getName();
				revisionInfoGenerator = new DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
						revisionInfoEntityName, revisionInfoEntityClass,
						revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate(), modifiedEntityNamesData
				);
			}
			else {
				revisionInfoEntityClass = globalCfg.isUseRevisionEntityWithNativeId() ? DefaultRevisionEntity.class
																					  : SequenceIdRevisionEntity.class;
				revisionInfoGenerator = new DefaultRevisionInfoGenerator(
						revisionInfoEntityName, revisionInfoEntityClass,
						revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate()
				);
			}
			revisionInfoMapping = generateDefaultRevisionInfoMapping();
		}

		return new RevisionInfoConfigurationResult(
				revisionInfoGenerator, revisionInfoMapping,
				new RevisionInfoQueryCreator(
						revisionInfoEntityName, revisionInfoIdData.getName(),
						revisionInfoTimestampData.getName(), isTimestampAsDate()
				),
				generateRevisionInfoRelationMapping(),
				new RevisionInfoNumberReader( revisionInfoEntityClass, revisionInfoIdData ),
				globalCfg.isTrackEntitiesChangedInRevisionEnabled() ? new ModifiedEntityNamesReader(
						revisionInfoEntityClass, modifiedEntityNamesData ) : null,
				revisionInfoEntityName, revisionInfoEntityClass, revisionInfoTimestampData
		);
    }

    private boolean isTimestampAsDate() {
    	final String typename = revisionInfoTimestampType.getName();
    	return "date".equals(typename) || "time".equals(typename) || "timestamp".equals(typename);
    }

    /**
	 * Method takes into consideration {@code org.hibernate.envers.revision_listener} parameter and custom
	 * {@link RevisionEntity} annotation.
     * @param classLoaderService Class loading service.
	 * @param revisionEntityAnnotation User defined @RevisionEntity annotation, or {@code null} if none.
     * @return Revision listener.
     */
	private Class<? extends RevisionListener> getRevisionListenerClass(ClassLoaderService classLoaderService,
																	   AnnotationInstance revisionEntityAnnotation) {
		if ( globalCfg.getRevisionListenerClass() != null ) {
			return globalCfg.getRevisionListenerClass();
		}
		if ( revisionEntityAnnotation != null && revisionEntityAnnotation.value() != null ) {
			// User provided revision listener implementation in @RevisionEntity mapping.
			return classLoaderService.classForName( revisionEntityAnnotation.value().asString() );
		}
		return RevisionListener.class;
	}
}

class RevisionInfoConfigurationResult {
    private final RevisionInfoGenerator revisionInfoGenerator;
    private final JaxbRoot revisionInfoMapping;
    private final RevisionInfoQueryCreator revisionInfoQueryCreator;
    private final JaxbKeyManyToOneElement revisionInfoRelationMapping;
    private final RevisionInfoNumberReader revisionInfoNumberReader;
    private final ModifiedEntityNamesReader modifiedEntityNamesReader;
    private final String revisionInfoEntityName;
    private final Class<?> revisionInfoClass;
    private final PropertyData revisionInfoTimestampData;

    RevisionInfoConfigurationResult(RevisionInfoGenerator revisionInfoGenerator,
									JaxbRoot revisionInfoMapping, RevisionInfoQueryCreator revisionInfoQueryCreator,
									JaxbKeyManyToOneElement revisionInfoRelationMapping, RevisionInfoNumberReader revisionInfoNumberReader,
                                    ModifiedEntityNamesReader modifiedEntityNamesReader, String revisionInfoEntityName,
                                    Class<?> revisionInfoClass, PropertyData revisionInfoTimestampData) {
        this.revisionInfoGenerator = revisionInfoGenerator;
        this.revisionInfoMapping = revisionInfoMapping;
        this.revisionInfoQueryCreator = revisionInfoQueryCreator;
        this.revisionInfoRelationMapping = revisionInfoRelationMapping;
        this.revisionInfoNumberReader = revisionInfoNumberReader;
        this.modifiedEntityNamesReader = modifiedEntityNamesReader;
        this.revisionInfoEntityName = revisionInfoEntityName;
        this.revisionInfoClass = revisionInfoClass;
        this.revisionInfoTimestampData = revisionInfoTimestampData;
    }

    public RevisionInfoGenerator getRevisionInfoGenerator() {
        return revisionInfoGenerator;
    }

	public JaxbRoot getRevisionInfoMapping() {
		return revisionInfoMapping;
	}

	public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
        return revisionInfoQueryCreator;
    }

    public JaxbKeyManyToOneElement getRevisionInfoRelationMapping() {
        return revisionInfoRelationMapping;
    }

    public RevisionInfoNumberReader getRevisionInfoNumberReader() {
        return revisionInfoNumberReader;
    }

    public String getRevisionInfoEntityName() {
        return revisionInfoEntityName;
    }

	public Class<?> getRevisionInfoClass() {
		return revisionInfoClass;
	}

	public PropertyData getRevisionInfoTimestampData() {
		return revisionInfoTimestampData;
	}

    public ModifiedEntityNamesReader getModifiedEntityNamesReader() {
        return modifiedEntityNamesReader;
    }
}
