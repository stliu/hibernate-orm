package org.hibernate.envers.configuration;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.jandex.AnnotationInstance;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class AnnotationProxyBuilder {
	private final Map<AnnotationInstance, Object> proxyObjectMap = new HashMap<AnnotationInstance, Object>();
	private final Map<Class, ProxyFactory> proxyFactoryMap = new HashMap<Class, ProxyFactory>();

	public <T> T getAnnotationProxy(final AnnotationInstance annotationInstance, final Class<T> annotationClass) {
		T annotationProxy = (T) proxyObjectMap.get( annotationInstance );
		if ( annotationProxy == null ) {
			annotationProxy = buildAnnotationProxy( annotationInstance, annotationClass );
			proxyObjectMap.put( annotationInstance, annotationProxy );
		}
		return annotationProxy;
	}

	private <T> T buildAnnotationProxy(final AnnotationInstance annotationInstance, final Class<T> annotationClass) {
		try {
			final Class annotation = annotationClass.getClassLoader().loadClass( annotationClass.getName() );
			final Class proxyClass = getProxyFactory( annotation ).createClass();
			final ProxyObject proxyObject = (ProxyObject) proxyClass.newInstance();
			proxyObject.setHandler( new MethodHandler() {
				@Override
				public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
					String executedMethodName = thisMethod.getName();
					if ( "toString".equals( executedMethodName ) ) {
						return proxyClass.getName() + "@" + System.identityHashCode( self );
					}
					return JandexHelper.getValue( annotationInstance, executedMethodName, Object.class );
				}
			} );
			return (T) proxyObject;
		}
		catch ( Exception e ) {
			throw new HibernateException( e );
		}
	}

	private ProxyFactory getProxyFactory(final Class annotation) {
		ProxyFactory proxyFactory = proxyFactoryMap.get( annotation );
		if ( proxyFactory == null ) {
			proxyFactory = new ProxyFactory();
			proxyFactoryMap.put( annotation, proxyFactory );
		}
		proxyFactory.setInterfaces( new Class[] { annotation } );
		return proxyFactory;
	}
}
