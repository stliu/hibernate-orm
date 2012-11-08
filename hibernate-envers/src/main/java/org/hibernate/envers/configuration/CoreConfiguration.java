package org.hibernate.envers.configuration;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class CoreConfiguration {
	private final MetadataImplementor metadata;
	private final IndexView jandexIndex;
	private final AnnotationProxyBuilder annotationProxyBuilder;

	public CoreConfiguration(MetadataImplementor metadata, IndexView jandexIndex) {
		this.metadata = metadata;
		this.jandexIndex = jandexIndex;
		this.annotationProxyBuilder = new AnnotationProxyBuilder();
	}

	public EntityBinding locateEntityBinding(final ClassInfo clazz) {
		// TODO: Is there a better way?
//		final AnnotationInstance jpaEntityAnnotation = JandexHelper.getSingleAnnotation( clazz, JPADotNames.ENTITY );
//		String entityName = JandexHelper.getValue( jpaEntityAnnotation, "name", String.class );
//		if ( entityName == null ) {
//			entityName = clazz.name().toString();
//		}
		return locateEntityBinding( clazz.name().toString() );
	}

	public EntityBinding locateEntityBinding(final String entityName) {
		return metadata.getEntityBinding( entityName );
	}

	public ClassInfo locateClassInfo(final EntityBinding entityBinding) {
		return locateClassInfo( entityBinding.getEntity().getClassName() );
	}

	public ClassInfo locateClassInfo(final Class clazz) {
		return locateClassInfo( clazz.getName() );
	}

	public ClassInfo locateClassInfo(final String className) {
		final DotName dotName = DotName.createSimple( className );
		return jandexIndex.getClassByName( dotName );
	}

	public Map<DotName, List<AnnotationInstance>> locateAttributeAnnotations(final AttributeBinding attributeBinding) {
		final EntityBinding entityBinding = attributeBinding.getContainer().seekEntityBinding();
		final ClassInfo classInfo = locateClassInfo( entityBinding );
		return JandexHelper.getMemberAnnotations( classInfo, attributeBinding.getAttribute().getName() );
	}

	public <T> T createAnnotationProxy(final AnnotationInstance annotationInstance, final Class<T> annotation) {
		return annotationProxyBuilder.getAnnotationProxy( annotationInstance, annotation );
	}

	public MetadataImplementor getMetadata() {
		return metadata;
	}

	public IndexView getJandexIndex() {
		return jandexIndex;
	}
}
