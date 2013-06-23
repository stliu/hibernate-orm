/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceResolutionContext;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.FilterSource;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.Orderable;
import org.hibernate.metamodel.spi.source.PluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeKeySource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.Sortable;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;
import org.hibernate.metamodel.spi.source.ToOneAttributeSource;

/**
 * @author Hardy Ferentschik
 */
public class PluralAttributeSourceImpl implements PluralAttributeSource, Orderable, Sortable {

	private final PluralAssociationAttribute associationAttribute;
	private final ConfiguredClass entityClass;
	private final Nature nature;
	private final ExplicitHibernateTypeSource typeSource;
	private final PluralAttributeKeySource keySource;
	private final FilterSource[] filterSources;

	// If it is not the owner side (i.e., mappedBy != null), then the AttributeSource
	// for the owner is required to determine elementSource.
	private PluralAttributeElementSource elementSource;
	private AttributeSource ownerAttributeSource;

	public PluralAttributeSourceImpl(
			final PluralAssociationAttribute associationAttribute,
			final ConfiguredClass entityClass ) {
		this.associationAttribute = associationAttribute;
		this.entityClass = entityClass;
		this.keySource = new PluralAttributeKeySourceImpl( associationAttribute );
		this.typeSource = new ExplicitHibernateTypeSourceImpl( associationAttribute );
		this.nature = associationAttribute.getPluralAttributeNature();
		if ( associationAttribute.getMappedBy() == null ) {
			this.ownerAttributeSource = this;
			this.elementSource = determineElementSource( this, associationAttribute, entityClass );
		}
		this.filterSources = determineFilterSources(associationAttribute);
	}

	private static FilterSource[] determineFilterSources(PluralAssociationAttribute associationAttribute) {
		AnnotationInstance filtersAnnotation = JandexHelper.getSingleAnnotation(
				associationAttribute.annotations(),
				HibernateDotNames.FILTERS
		);
		List<FilterSource> filterSourceList = new ArrayList<FilterSource>();
		if ( filtersAnnotation != null ) {
			AnnotationInstance[] annotationInstances = filtersAnnotation.value().asNestedArray();
			for ( AnnotationInstance filterAnnotation : annotationInstances ) {
				FilterSource filterSource = new FilterSourceImpl( filterAnnotation );
				filterSourceList.add( filterSource );
			}

		}
		AnnotationInstance filterAnnotation = JandexHelper.getSingleAnnotation(
				associationAttribute.annotations(),
				HibernateDotNames.FILTER
		);
		if ( filterAnnotation != null ) {
			FilterSource filterSource = new FilterSourceImpl( filterAnnotation );
			filterSourceList.add( filterSource );
		}
		if ( filterSourceList.isEmpty() ) {
			return null;
		}
		else {
			return filterSourceList.toArray( new FilterSource[filterSourceList.size()] );
		}
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public PluralAttributeElementSource getElementSource() {
		if ( elementSource == null ) {
			throw new IllegalStateException( "elementSource has not been initialized yet." );
		}
		return elementSource;
	}

	@Override
	public FilterSource[] getFilterSources() {
		return filterSources;
	}

	@Override
	public int getBatchSize() {
		return associationAttribute.getBatchSize();
	}

	public static boolean usesJoinTable(AttributeSource ownerAttributeSource) {
		return ownerAttributeSource.isSingular() ?
				( (ToOneAttributeSource) ownerAttributeSource ).getContainingTableName() != null :
				( (PluralAttributeSource) ownerAttributeSource ).usesJoinTable();
	}

	public boolean usesJoinTable() {
		if ( associationAttribute.getMappedBy() != null ) {
			throw new IllegalStateException( "Cannot determine if a join table is used because plural attribute is not the owner." );
		}
		// By default, a unidirectional one-to-many (i.e., with mappedBy == null) uses a join table,
		// unless it has join columns defined.
		return associationAttribute.getJoinTableAnnotation() != null ||
				( associationAttribute.getJoinTableAnnotation() == null &&
						associationAttribute.getJoinColumnValues().size() == 0 );
	}

	@Override
	public ValueHolder<Class<?>> getElementClassReference() {
		// needed for arrays
		Class<?> attributeType = associationAttribute.getAttributeType();
		if ( attributeType.isArray() ) {
			return new ValueHolder<Class<?>>( attributeType.getComponentType() );
		}
		else {
			return null;
		}
	}

	private static PluralAttributeElementSource determineElementSource(
			AttributeSource ownerAttributeSource,
			PluralAssociationAttribute associationAttribute,
			ConfiguredClass entityClass) {
		switch ( associationAttribute.getNature() ) {
			case MANY_TO_MANY:
				return new ManyToManyPluralAttributeElementSourceImpl( ownerAttributeSource, associationAttribute, false );
			case MANY_TO_ANY:
				return new ManyToAnyPluralAttributeElementSourceImpl( associationAttribute );
			case ONE_TO_MANY:
				return usesJoinTable( ownerAttributeSource ) ?
						new ManyToManyPluralAttributeElementSourceImpl( ownerAttributeSource, associationAttribute, true ) :
						new OneToManyPluralAttributeElementSourceImpl( ownerAttributeSource, associationAttribute );
			case ELEMENT_COLLECTION_BASIC:
				return new BasicPluralAttributeElementSourceImpl( associationAttribute, entityClass );
			case ELEMENT_COLLECTION_EMBEDDABLE: {
				// TODO: cascadeStyles?
				return new CompositePluralAttributeElementSourceImpl(
						associationAttribute, entityClass
				);
			}
		}
		throw new AssertionError( "Unexpected attribute nature for a association:" + associationAttribute.getNature() );
	}

	@Override
	public PluralAttributeKeySource getKeySource() {
		return keySource;
	}

	@Override
	public TableSpecificationSource getCollectionTableSpecificationSource() {
		// todo - see org.hibernate.metamodel.internal.Binder#bindOneToManyCollectionKey
		// todo - needs to cater for @CollectionTable and @JoinTable
		if ( associationAttribute.getMappedBy() != null ) {
			if ( ownerAttributeSource.isSingular() ) {
				ToOneAttributeSource ownerSingularAttributeSource = (ToOneAttributeSource) ownerAttributeSource;
				throw new NotYetImplementedException( "mappedBy many-to-many owned by many-to-one not supported yet." );
			}
			else {
				PluralAttributeSource ownerPluralAttributeSource = (PluralAttributeSource) ownerAttributeSource;
				return ownerPluralAttributeSource.getCollectionTableSpecificationSource();
			}
		}
		final AnnotationInstance joinTableAnnotation = associationAttribute.getJoinTableAnnotation();
		return joinTableAnnotation == null ? null : new TableSourceImpl( joinTableAnnotation );
	}

	@Override
	public String getCollectionTableComment() {
		return null;
	}

	@Override
	public String getCollectionTableCheck() {
		return associationAttribute.getCheckCondition();
	}

	@Override
	public Caching getCaching() {
		return associationAttribute.getCaching();
	}

	@Override
	public String getCustomPersisterClassName() {
		return associationAttribute.getCustomPersister();
	}

	@Override
	public String getWhere() {
		return associationAttribute.getWhereClause();
	}

	@Override
	public String getMappedBy() {
		return associationAttribute.getMappedBy();
	}

	@Override
	public boolean isInverse() {
		return getMappedBy() != null;
	}

	@Override
	public String getCustomLoaderName() {
		return associationAttribute.getCustomLoaderName();
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return associationAttribute.getCustomInsert();
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return associationAttribute.getCustomUpdate();
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return associationAttribute.getCustomDelete();
	}

	@Override
	public CustomSQL getCustomSqlDeleteAll() {
		return associationAttribute.getCustomDeleteAll();
	}

	@Override
	public String getName() {
		return associationAttribute.getName();
	}

	@Override
	public boolean isSingular() {
		return false;
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return associationAttribute.getAccessType();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return associationAttribute.isOptimisticLockable();
	}

	@Override
	public Iterable<MetaAttributeSource> getMetaAttributeSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}

	@Override
	public String getOrder() {
		return elementSource.getNature() == PluralAttributeElementSource.Nature.MANY_TO_MANY ?
				null :
				associationAttribute.getOrderBy();
	}

	@Override
	public boolean isMutable() {
		return associationAttribute.isMutable();
	}

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( getOrder() );
	}

	@Override
	public String getComparatorName() {
		return associationAttribute.getComparatorName();
	}

	@Override
	public boolean isSorted() {
		return associationAttribute.isSorted();
	}

	@Override
	public FetchTiming getFetchTiming() {
		if ( associationAttribute.isExtraLazy() ) {
			return FetchTiming.EXTRA_DELAYED;
		}
		else if ( associationAttribute.isLazy() ) {
			return FetchTiming.DELAYED;
		}
		else {
			return FetchTiming.IMMEDIATE;
		}
	}

	@Override
	public FetchStyle getFetchStyle() {
		return associationAttribute.getFetchStyle();
	}

	@Override
	public PluralAttributeElementSource resolvePluralAttributeElementSource(
			AttributeSourceResolutionContext context) {
		if ( elementSource == null ) {
			// elementSource has not been initialized, so we need to resolve it using the
			// association owner.
			// Get the owner attribute source that maps the opposite side of the association.
			ownerAttributeSource = context.resolveAttributeSource(
					associationAttribute.getReferencedEntityType(),
					associationAttribute.getMappedBy()
			);
			// Initialize resolved entitySource.
			elementSource = determineElementSource( ownerAttributeSource, associationAttribute, entityClass );
		}
		return elementSource;
	}

	protected PluralAssociationAttribute pluralAssociationAttribute() {
		return associationAttribute;
	}
}


