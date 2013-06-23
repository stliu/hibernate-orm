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

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.attribute.Column;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.PluralAttributeKeySource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Hardy Ferentschik
 * @author Strong Liu <stliu@hibernate.org>
 */
public class PluralAttributeKeySourceImpl implements PluralAttributeKeySource {
	private final PluralAssociationAttribute attribute;
	private final ForeignKey.ReferentialAction deleteAction;

	public PluralAttributeKeySourceImpl(PluralAssociationAttribute attribute) {
		this.attribute = attribute;
		this.deleteAction = attribute.getOnDeleteAction() == OnDeleteAction.CASCADE
				? ForeignKey.ReferentialAction.CASCADE : ForeignKey.ReferentialAction.NO_ACTION;
	}
	@Override
	public ForeignKey.ReferentialAction getOnDeleteAction() {
		return deleteAction;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return attribute.getExplicitForeignKeyName();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {

		for ( Column joinColumn : attribute.getJoinColumnValues() ) {
			if ( StringHelper.isNotEmpty( joinColumn.getReferencedColumnName() ) ) {
				return new JoinColumnResolutionDelegateImpl( attribute );
			}
		}
		return null;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		List<Column> joinClumnValues = attribute.getJoinColumnValues();
		if ( joinClumnValues.isEmpty() ) {
			return Collections.emptyList();
		}
		List<RelationalValueSource> result = new ArrayList<RelationalValueSource>( joinClumnValues.size() );
		for ( Column joinColumn : joinClumnValues ) {
			result.add( new ColumnSourceImpl( joinColumn ) );
		}
		return result;
	}

	public static class JoinColumnResolutionDelegateImpl implements JoinColumnResolutionDelegate {
		private final PluralAssociationAttribute attribute;

		public JoinColumnResolutionDelegateImpl(PluralAssociationAttribute attribute) {
			this.attribute = attribute;
		}

		@Override
		public List<Value> getJoinColumns(JoinColumnResolutionContext context) {
			List<Column> joinColumnValues = attribute.getJoinColumnValues();
			if ( joinColumnValues.isEmpty() ) {
				return null;
			}
			List<Value> result = new ArrayList<Value>( joinColumnValues.size() );
			for ( Column column : attribute.getJoinColumnValues() ) {
				result.add( context.resolveColumn( column.getReferencedColumnName(), null, null, null ) );
			}
			return result;
		}

		@Override
		public String getReferencedAttributeName() {
			return null;
		}

		@Override
		public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
			return context.resolveTableForAttribute( null );
		}
	}
}
