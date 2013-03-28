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

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jboss.jandex.IndexView;

import org.hibernate.MappingException;
import org.hibernate.envers.entities.EntitiesConfigurations;
import org.hibernate.envers.entities.PropertyData;
import org.hibernate.envers.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.envers.synchronization.AuditProcessManager;
import org.hibernate.envers.tools.reflection.ReflectionTools;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.jaxb.spi.JaxbRoot;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.property.Getter;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Stephanie Pau at Markit Group Plc
 */
public class AuditConfiguration {
	private final CoreConfiguration coreConfiguration;
	private final GlobalConfiguration globalCfg;
	private final AuditEntitiesConfiguration auditEntCfg;
	private final AuditProcessManager auditProcessManager;
	private final AuditStrategy auditStrategy;
	private final EntitiesConfigurations entCfg;
	private final RevisionInfoQueryCreator revisionInfoQueryCreator;
	private final RevisionInfoNumberReader revisionInfoNumberReader;
	private final ModifiedEntityNamesReader modifiedEntityNamesReader;
	private final ClassLoaderService classLoaderService;
	private final JaxbRoot revisionInfoEntityMapping;
//	TODO: private final List<JaxbRoot> auditEntitiesMapping;

	public AuditEntitiesConfiguration getAuditEntCfg() {
		return auditEntCfg;
	}

	public AuditProcessManager getSyncManager() {
		return auditProcessManager;
	}

	public GlobalConfiguration getGlobalCfg() {
		return globalCfg;
	}

	public EntitiesConfigurations getEntCfg() {
		return entCfg;
	}

	public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
		return revisionInfoQueryCreator;
	}

	public RevisionInfoNumberReader getRevisionInfoNumberReader() {
		return revisionInfoNumberReader;
	}

	public ModifiedEntityNamesReader getModifiedEntityNamesReader() {
		return modifiedEntityNamesReader;
	}

	public AuditStrategy getAuditStrategy() {
		return auditStrategy;
	}

	public JaxbRoot getRevisionInfoEntityMapping() {
		return revisionInfoEntityMapping;
	}

	public AuditConfiguration(MetadataImplementor metadata, IndexView jandexIndex) {
		coreConfiguration = new CoreConfiguration( metadata, jandexIndex );
		globalCfg = new GlobalConfiguration( metadata.getServiceRegistry() );
		RevisionInfoConfiguration revInfoCfg = new RevisionInfoConfiguration( globalCfg );
		RevisionInfoConfigurationResult revInfoCfgResult = revInfoCfg.configure( coreConfiguration );
		revisionInfoEntityMapping = revInfoCfgResult.getRevisionInfoMapping();
		auditEntCfg = new AuditEntitiesConfiguration( metadata.getServiceRegistry(), revInfoCfgResult.getRevisionInfoEntityName() );
		auditProcessManager = new AuditProcessManager( revInfoCfgResult.getRevisionInfoGenerator() );
		revisionInfoQueryCreator = revInfoCfgResult.getRevisionInfoQueryCreator();
		revisionInfoNumberReader = revInfoCfgResult.getRevisionInfoNumberReader();
		modifiedEntityNamesReader = revInfoCfgResult.getModifiedEntityNamesReader();
		classLoaderService = metadata.getServiceRegistry().getService( ClassLoaderService.class );
		auditStrategy = initializeAuditStrategy(
				revInfoCfgResult.getRevisionInfoClass(),
				revInfoCfgResult.getRevisionInfoTimestampData()
		);
		entCfg = new EntitiesConfigurator().configure(
				coreConfiguration, globalCfg, auditEntCfg, auditStrategy,
				revInfoCfgResult.getRevisionInfoMapping(), revInfoCfgResult.getRevisionInfoRelationMapping()
		);
	}

	private AuditStrategy initializeAuditStrategy(Class<?> revisionInfoClass, PropertyData revisionInfoTimestampData) {
		AuditStrategy strategy;

		try {
			Class<?> auditStrategyClass = null;
			if ( classLoaderService != null ) {
				auditStrategyClass = classLoaderService.classForName( auditEntCfg.getAuditStrategyName() );
			}
			else {
				// TODO: Is this block really necessary?
				auditStrategyClass = Thread.currentThread()
						.getContextClassLoader()
						.loadClass( auditEntCfg.getAuditStrategyName() );
			}

			strategy = (AuditStrategy) ReflectHelper.getDefaultConstructor( auditStrategyClass ).newInstance();
		}
		catch ( Exception e ) {
			throw new MappingException(
					String.format( "Unable to create AuditStrategy[%s] instance.", auditEntCfg.getAuditStrategyName() ),
					e
			);
		}

		if ( strategy instanceof ValidityAuditStrategy ) {
			// further initialization required
			Getter revisionTimestampGetter = ReflectionTools.getGetter( revisionInfoClass, revisionInfoTimestampData );
			( (ValidityAuditStrategy) strategy ).setRevisionTimestampGetter( revisionTimestampGetter );
		}

		return strategy;
	}

	private static Map<MetadataImplementor, AuditConfiguration> cfgs = new WeakHashMap<MetadataImplementor, AuditConfiguration>();

	public synchronized static AuditConfiguration register(MetadataImplementor metadata, IndexView jandexIndex) {
		AuditConfiguration verCfg = cfgs.get( metadata );

		if ( verCfg == null ) {
			verCfg = new AuditConfiguration( metadata, jandexIndex );
			cfgs.put( metadata, verCfg );
		}

		return verCfg;
	}

	public synchronized static AuditConfiguration get(MetadataImplementor metadata) {
		return cfgs.get( metadata );
	}
}
