/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.attribute;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.source.MappingException;

/**
 * Contains the information about a single {@link javax.persistence.AttributeOverride}. Instances of this class
 * are creating during annotation processing and then applied onto the persistence attributes.
 *
 * @author Hardy Ferentschik
 * @todo Take care of prefixes of the form 'element', 'key' and 'value'. Add another type enum to handle this. (HF)
 */
public class AttributeOverride extends AbstractOverrideDefinition{
	private static final String PROPERTY_PATH_SEPARATOR = ".";
	private final Column column;
	private final AnnotationInstance columnAnnotation;
	private final String attributePath;

	public AttributeOverride(AnnotationInstance attributeOverrideAnnotation) {
		this( null, attributeOverrideAnnotation );
	}

	public AttributeOverride(String prefix, AnnotationInstance attributeOverrideAnnotation) {
		if ( attributeOverrideAnnotation == null ) {
			throw new IllegalArgumentException( "An AnnotationInstance needs to be passed" );
		}

		if ( !JPADotNames.ATTRIBUTE_OVERRIDE.equals( attributeOverrideAnnotation.name() ) ) {
			throw new AssertionFailure( "A @AttributeOverride annotation needs to be passed to the constructor" );
		}

		this.columnAnnotation= JandexHelper.getValue( attributeOverrideAnnotation, "column", AnnotationInstance.class );
		this.column = new Column( columnAnnotation );
		this.attributePath = createAttributePath(
				prefix,
				JandexHelper.getValue( attributeOverrideAnnotation, "name", String.class )
		);
	}

	public Column getColumnValue() {
		return column;
	}
	@Override
	public String getAttributePath() {
		return attributePath;
	}

	@Override
	public void apply(MappedAttribute mappedAttribute) {
		int columnSize = mappedAttribute.getColumnValues().size();
		switch ( columnSize ){
			case 0:
				mappedAttribute.getColumnValues().add( column );
				break;
			case 1:
				mappedAttribute.getColumnValues().get( 0 ).applyColumnValues( columnAnnotation );
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AttributeOverride" );
		sb.append( "{column=" ).append( column );
		sb.append( ", attributePath='" ).append( attributePath ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		AttributeOverride that = (AttributeOverride) o;

		if ( attributePath != null ? !attributePath.equals( that.attributePath ) : that.attributePath != null ) {
			return false;
		}
		if ( column != null ? !column.equals( that.column ) : that.column != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = column != null ? column.hashCode() : 0;
		result = 31 * result + ( attributePath != null ? attributePath.hashCode() : 0 );
		return result;
	}

	private static String createAttributePath(String prefix, String name) {
		if ( StringHelper.isEmpty( name ) ) {
			throw new AssertionFailure( "name attribute in @AttributeOverride can't be empty" );
		}
		String path = "";
		if ( StringHelper.isNotEmpty( prefix ) ) {
			path += prefix;
		}
		if ( StringHelper.isNotEmpty( path ) && !path.endsWith( PROPERTY_PATH_SEPARATOR ) ) {
			path += PROPERTY_PATH_SEPARATOR;
		}
		path += name;
		return path;
	}
}


