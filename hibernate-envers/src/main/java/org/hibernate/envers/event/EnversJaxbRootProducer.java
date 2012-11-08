package org.hibernate.envers.event;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jaxb.spi.JaxbRoot;
import org.hibernate.metamodel.spi.AdditionalJaxbRootProducer;
import org.hibernate.metamodel.spi.MetadataImplementor;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EnversJaxbRootProducer implements AdditionalJaxbRootProducer {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			EnversJaxbRootProducer.class.getName()
	);

	@Override
	public List<JaxbRoot> produceRoots(MetadataImplementor metadata, IndexView jandexIndex) {
		final AuditConfiguration configuration = AuditConfiguration.register( metadata, jandexIndex );
		final List<JaxbRoot> entities = new LinkedList<>();
		if ( configuration.getRevisionInfoEntityMapping() != null ) {
			// User did not provide custom @RevisionEntity.
			entities.add( configuration.getRevisionInfoEntityMapping() );
		}
		return Collections.unmodifiableList( entities );
	}
}
