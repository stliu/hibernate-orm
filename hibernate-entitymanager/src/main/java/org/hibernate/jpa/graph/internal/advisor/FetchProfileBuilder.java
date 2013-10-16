/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

package org.hibernate.jpa.graph.internal.advisor;

import java.util.Map;

import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.graph.internal.EntityGraphImpl;

/**
 * @author Strong Liu
 */
public class FetchProfileBuilder {
	public static FetchProfile build(Map<String, Object> hints){
		if ( CollectionHelper.isEmpty( hints ) ) {
			return null;
		}
		if ( hints.containsKey( AvailableSettings.FETCH_GRAPH ) ) {
			return build(
					(EntityGraphImpl) hints.get( AvailableSettings.FETCH_GRAPH ), AdviceStyle.FETCH
			);
		}
		else if ( hints.containsKey( AvailableSettings.LOAD_GRAPH ) ) {
			return build(
					(EntityGraphImpl) hints.get( AvailableSettings.LOAD_GRAPH ), AdviceStyle.LOAD
			);
		}
		return null;
	}

	public static FetchProfile build(EntityGraphImpl graph, AdviceStyle style) {
		EntityGraphVisitationStrategy strategy = null;
		switch ( style ) {
			case FETCH:
				strategy = new FetchEntityGraphVisitationStrategy();
				break;
			case LOAD:
				strategy = new LoadEntityGraphVisitationStrategy();
		}
		EntityGraphWalker.visit( strategy, graph );
		return strategy.buildFetchProfile();
	}

}
