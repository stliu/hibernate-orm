package org.hibernate.loader.plan2.build.internal;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.AttributeNode;
import javax.persistence.Subgraph;
import javax.persistence.metamodel.Attribute;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphNodeImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan2.build.spi.AbstractLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan2.spi.EntityReturn;
import org.hibernate.loader.plan2.spi.LoadPlan;
import org.hibernate.loader.plan2.spi.Return;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.persister.walking.spi.WalkingException;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public abstract class AbstractEntityGraphVisitationStrategy
		extends AbstractLoadPlanBuildingAssociationVisitationStrategy {
	private static final Logger LOG = CoreLogging.logger( AbstractEntityGraphVisitationStrategy.class );
	protected static final FetchStrategy DEFAULT_EAGER = new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );
	protected static final FetchStrategy DEFAULT_LAZY = new FetchStrategy( FetchTiming.DELAYED, FetchStyle.SELECT );
	protected final LoadQueryInfluencers loadQueryInfluencers;
	protected final ArrayDeque<GraphNodeImplementor> graphStack = new ArrayDeque<GraphNodeImplementor>();
	protected final ArrayDeque<AttributeNodeImplementor> attributeStack = new ArrayDeque<AttributeNodeImplementor>();
	//the attribute nodes defined in the current graph node (entity graph or subgraph) we're working on
	protected Map<String, AttributeNodeImplementor> attributeNodeImplementorMap = Collections.emptyMap();
	private EntityReturn rootEntityReturn;
	private final LockMode lockMode;

	protected AbstractEntityGraphVisitationStrategy(
			final SessionFactoryImplementor sessionFactory, final LoadQueryInfluencers loadQueryInfluencers,final LockMode lockMode) {
		super( sessionFactory );
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockMode = lockMode;
	}

	@Override
	public void start() {
		super.start();
		graphStack.addLast( getRootEntityGraph() );
	}

	@Override
	public void finish() {
		super.finish();
		graphStack.removeLast();
		if ( !graphStack.isEmpty() || !attributeStack.isEmpty() || !attributeNodeImplementorMap.isEmpty() ) {
			throw new WalkingException( "Internal stack error" );
		}
	}

	@Override
	public void startingEntity(final EntityDefinition entityDefinition) {
		//TODO check if the passed in entity definition is the same as the root entity graph (a.k.a they are came from same entity class)?
		//this maybe the root entity graph or a sub graph.
		attributeNodeImplementorMap = buildAttributeNodeMap();
		super.startingEntity( entityDefinition );
	}

	protected Map<String, AttributeNodeImplementor> buildAttributeNodeMap() {
		GraphNodeImplementor graphNode = graphStack.peek();
		List<AttributeNodeImplementor<?>> attributeNodeImplementors = graphNode.attributeImplementorNodes();
		Map<String, AttributeNodeImplementor> attributeNodeImplementorMap = attributeNodeImplementors.isEmpty() ? Collections
				.<String, AttributeNodeImplementor>emptyMap() : new HashMap<String, AttributeNodeImplementor>(
				attributeNodeImplementors.size()
		);
		for ( AttributeNodeImplementor attribute : attributeNodeImplementors ) {
			attributeNodeImplementorMap.put( attribute.getAttributeName(), attribute );
		}
		return attributeNodeImplementorMap;
	}

	@Override
	public void finishingEntity(final EntityDefinition entityDefinition) {
		attributeNodeImplementorMap = Collections.emptyMap();
		super.finishingEntity( entityDefinition );
	}


	@Override
	public boolean startingAttribute(AttributeDefinition attributeDefinition) {
		final String attrName = attributeDefinition.getName();
		AttributeNodeImplementor attributeNode = NON_EXIST_ATTRIBUTE_NODE;
		GraphNodeImplementor subGraphNode = NON_EXIST_SUBGRAPH_NODE;
		//the attribute is in the EntityGraph, so, let's continue
		if ( attributeNodeImplementorMap.containsKey( attrName ) ) {
			attributeNode = attributeNodeImplementorMap.get( attrName );
			//here we need to check if there is a subgraph (or sub key graph if it is an indexed attribute )
			Map<Class, Subgraph> subGraphs = attributeNode.getSubgraphs();
			Class javaType = attributeDefinition.getType().getReturnedClass();
			if ( !subGraphs.isEmpty() && subGraphs.containsKey( javaType ) ) {
				subGraphNode = (GraphNodeImplementor) subGraphs.get( javaType );
			}

		}
		attributeStack.addLast( attributeNode );
		graphStack.addLast( subGraphNode );
		return super.startingAttribute( attributeDefinition );
	}


	@Override
	public void finishingAttribute(final AttributeDefinition attributeDefinition) {
		attributeStack.removeLast();
		graphStack.removeLast();
		super.finishingAttribute( attributeDefinition );
	}

	@Override
	protected boolean handleAssociationAttribute(
			final AssociationAttributeDefinition attributeDefinition) {
		return super.handleAssociationAttribute( attributeDefinition );
	}

	@Override
	protected boolean handleCompositeAttribute(
			final CompositionDefinition attributeDefinition) {
		return super.handleCompositeAttribute( attributeDefinition );
	}


	@Override
	public void startingComposite(final CompositionDefinition compositionDefinition) {
		super.startingComposite( compositionDefinition );
	}


	@Override
	public void finishingComposite(final CompositionDefinition compositionDefinition) {
		super.finishingComposite( compositionDefinition );
	}


	@Override
	public void startingCollection(final CollectionDefinition collectionDefinition) {
		super.startingCollection( collectionDefinition );
	}

	@Override
	public void finishingCollection(final CollectionDefinition collectionDefinition) {
		super.finishingCollection( collectionDefinition );
	}


	@Override
	public void startingCollectionElements(
			final CollectionElementDefinition elementDefinition) {
		super.startingCollectionElements( elementDefinition );
	}

	@Override
	public void finishingCollectionElements(
			final CollectionElementDefinition elementDefinition) {
		super.finishingCollectionElements( elementDefinition );
	}


	@Override
	public void startingCollectionIndex(final CollectionIndexDefinition indexDefinition) {
		super.startingCollectionIndex( indexDefinition );
	}

	@Override
	public void finishingCollectionIndex(final CollectionIndexDefinition indexDefinition) {
		super.finishingCollectionIndex( indexDefinition );
	}


	@Override
	protected boolean supportsRootCollectionReturns() {
		return false; //entity graph doesn't support root collection.
	}


	@Override
	protected void addRootReturn(final Return rootReturn) {
		if ( this.rootEntityReturn != null ) {
			throw new HibernateException( "Root return already identified" );
		}
		if ( !( rootReturn instanceof EntityReturn ) ) {
			throw new HibernateException( "Load entity graph only supports EntityReturn" );
		}
		this.rootEntityReturn = (EntityReturn) rootReturn;
	}

	@Override
	protected FetchStrategy determineFetchStrategy(
			final AssociationAttributeDefinition attributeDefinition) {
		return attributeStack.peekLast() != NON_EXIST_ATTRIBUTE_NODE ? DEFAULT_EAGER : resolveImplicitFetchStrategyFromEntityGraph(
				attributeDefinition
		);
	}

	protected abstract FetchStrategy resolveImplicitFetchStrategyFromEntityGraph(
			final AssociationAttributeDefinition attributeDefinition);

	protected FetchStrategy adjustJoinFetchIfNeeded(
			AssociationAttributeDefinition attributeDefinition, FetchStrategy fetchStrategy) {
		final Integer maxFetchDepth = sessionFactory().getSettings().getMaximumFetchDepth();
		if ( maxFetchDepth != null && currentDepth() > maxFetchDepth ) {
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		if ( attributeDefinition.getType().isCollectionType() && isTooManyCollections() ) {
			// todo : have this revert to batch or subselect fetching once "sql gen redesign" is in place
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		return fetchStrategy;
	}

	@Override
	public LoadPlan buildLoadPlan() {
		LOG.debug( "Building LoadPlan..." );
		return new LoadPlanImpl( rootEntityReturn, getQuerySpaces() );
	}

	abstract protected GraphNodeImplementor getRootEntityGraph();

	private static final AttributeNodeImplementor NON_EXIST_ATTRIBUTE_NODE = new AttributeNodeImplementor() {
		@Override
		public Attribute getAttribute() {
			return null;
		}

		@Override
		public AttributeNodeImplementor makeImmutableCopy() {
			return this;
		}

		@Override
		public String getAttributeName() {
			return null;
		}

		@Override
		public Map<Class, Subgraph> getSubgraphs() {
			return Collections.emptyMap();
		}

		@Override
		public Map<Class, Subgraph> getKeySubgraphs() {
			return Collections.emptyMap();
		}

		@Override
		public String toString() {
			return "Mocked NON-EXIST attribute node";
		}
	};
	private static final GraphNodeImplementor NON_EXIST_SUBGRAPH_NODE = new GraphNodeImplementor() {
		@Override
		public List<AttributeNodeImplementor<?>> attributeImplementorNodes() {
			return Collections.EMPTY_LIST;
		}

		@Override
		public List<AttributeNode<?>> attributeNodes() {
			return Collections.EMPTY_LIST;
		}
	};
}
