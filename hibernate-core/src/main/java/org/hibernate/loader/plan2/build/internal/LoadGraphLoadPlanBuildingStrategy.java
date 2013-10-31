package org.hibernate.loader.plan2.build.internal;


import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.EntityGraphImplementor;
import org.hibernate.loader.plan2.build.spi.AbstractLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class LoadGraphLoadPlanBuildingStrategy extends AbstractEntityGraphVisitationStrategy {
	private final EntityGraphImplementor rootEntityGraph;

	public LoadGraphLoadPlanBuildingStrategy(
			final SessionFactoryImplementor sessionFactory, final LoadQueryInfluencers loadQueryInfluencers,final LockMode lockMode) {
		super( sessionFactory, loadQueryInfluencers , lockMode);
		this.rootEntityGraph = (EntityGraphImplementor) loadQueryInfluencers.getLoadGraph();
	}

	@Override
	protected EntityGraphImplementor getRootEntityGraph() {
		return rootEntityGraph;
	}

	@Override
	protected FetchStrategy resolveImplicitFetchStrategyFromEntityGraph(
			final AssociationAttributeDefinition attributeDefinition) {
		FetchStrategy fetchStrategy = attributeDefinition.determineFetchPlan(
				loadQueryInfluencers,
				currentPropertyPath
		);
		if ( fetchStrategy.getTiming() == FetchTiming.IMMEDIATE && fetchStrategy.getStyle() == FetchStyle.JOIN ) {
			// see if we need to alter the join fetch to another form for any reason
			fetchStrategy = adjustJoinFetchIfNeeded( attributeDefinition, fetchStrategy );
		}
		return fetchStrategy;
	}

}
