package org.hibernate.jpa.test.graphs;

import java.util.Iterator;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;


import org.junit.Test;

import static org.junit.Assert.*;

import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;
import org.hibernate.jpa.graph.internal.SubgraphImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.loader.plan2.build.internal.FetchGraphLoadPlanBuildingStrategy;
import org.hibernate.loader.plan2.build.internal.FetchStyleLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan2.build.spi.AbstractLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan2.build.spi.LoadPlanTreePrinter;
import org.hibernate.loader.plan2.build.spi.MetamodelDrivenLoadPlanBuilder;
import org.hibernate.loader.plan2.exec.internal.AliasResolutionContextImpl;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.loader.plan2.spi.LoadPlan;
import org.hibernate.loader.plan2.spi.QuerySpace;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class EntityGraphLoadPlanBuilderTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Cat.class, Person.class, Country.class };
	}

	@Entity
	public static class Cat {
		@Id
		String name;
		@ManyToOne(fetch = FetchType.LAZY)
		Person owner;

	}

	@Entity
	public static class Person {
		@Id
		String name;
		@OneToMany(mappedBy = "owner")
		Set<Cat> pets;
		@Embedded
		Address homeAddress;
	}

	@Embeddable
	public static class Address {
		@ManyToOne
		Country country;

	}

	@Entity
	public static class Country {
		@Id
		String name;
	}

	@Test
	public void testBasicLoadPlanBuilding() {
		EntityManager em = getOrCreateEntityManager();
		EntityGraph eg = em.createEntityGraph( Cat.class );
		LoadPlan plan = buildLoadPlan( eg );
		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sfi() ) );
		QuerySpace rootQuerySpace = plan.getQuerySpaces().getRootQuerySpaces().get( 0 );
		assertFalse(
				"With fetchgraph property and an empty EntityGraph, there should be no join at all",
				rootQuerySpace.getJoins().iterator().hasNext()
		);
		// -------------------------------------------------- another a little more complicated case
		eg = em.createEntityGraph( Cat.class );
		eg.addSubgraph( "owner", Person.class );
		plan = buildLoadPlan( eg );
		LoadPlanTreePrinter.INSTANCE.logTree( plan, new AliasResolutionContextImpl( sfi() ) );
		rootQuerySpace = plan.getQuerySpaces().getRootQuerySpaces().get( 0 );
		Iterator<Join> iterator = rootQuerySpace.getJoins().iterator();
		assertTrue(
				"With fetchgraph property and an empty EntityGraph, there should be no join at all", iterator.hasNext()
		);
		Join personJoin = iterator.next();
		assertNotNull( personJoin );
		QuerySpace.Disposition disposition = personJoin.getRightHandSide().getDisposition();
		assertEquals(
				"This should be an entity join which fetches Person", QuerySpace.Disposition.ENTITY, disposition
		);

		iterator = personJoin.getRightHandSide().getJoins().iterator();
		assertTrue( "The composite address should be fetched", iterator.hasNext() );
		Join addressJoin = iterator.next();
		assertNotNull( addressJoin );
		disposition = addressJoin.getRightHandSide().getDisposition();
		assertEquals( QuerySpace.Disposition.COMPOSITE, disposition );
		assertFalse( iterator.hasNext() );
		assertFalse(
				"The ManyToOne attribute in composite should not be fetched",
				addressJoin.getRightHandSide().getJoins().iterator().hasNext()
		);
		em.close();
	}

	private SessionFactoryImplementor sfi() {
		return entityManagerFactory().unwrap( SessionFactoryImplementor.class );
	}

	private LoadPlan buildLoadPlan(EntityGraph entityGraph) {

		LoadQueryInfluencers loadQueryInfluencers = new LoadQueryInfluencers( sfi() );
		loadQueryInfluencers.setFetchGraph( entityGraph );
		EntityPersister ep = (EntityPersister) sfi().getClassMetadata( Cat.class );
		AbstractLoadPlanBuildingAssociationVisitationStrategy strategy = new FetchGraphLoadPlanBuildingStrategy(
				sfi(), loadQueryInfluencers, LockMode.NONE
		);
		return MetamodelDrivenLoadPlanBuilder.buildRootEntityLoadPlan( strategy, ep );
	}
}
