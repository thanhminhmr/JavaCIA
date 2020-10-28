/*
 * Copyright (C) 2020 Mai Thanh Minh (a.k.a. thanhminhmr or mrmathami)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package mrmathami.cia.java.jdt.project.differ;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;
import mrmathami.cia.java.tree.JavaIdentifiedEntity;
import mrmathami.utils.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

final class EntityMatcher {

	@Nonnull private final Map<JavaIdentifiedEntity, EntityWrapper> identicalCodeMap = new IdentityHashMap<>();
	@Nonnull private final Map<JavaIdentifiedEntity, EntityWrapper> similarCodeMap = new IdentityHashMap<>();
	@Nonnull private final Map<Pair<JavaIdentifiedEntity, JavaIdentifiedEntity>, int[]> matchMap = new HashMap<>();


//	int matchCode(@Nullable JavaIdentifiedEntity entity, boolean identicalMatch) {
//		return entity != null ? wrap(entity, identicalMatch).hashCode() : 0;
//	}

	boolean match(@Nullable JavaIdentifiedEntity entityA, @Nullable JavaIdentifiedEntity entityB, boolean identicalMatch) {
		if (entityA == null || entityB == null) return entityA == entityB;

		final Pair<JavaIdentifiedEntity, JavaIdentifiedEntity> keyPair = Pair.immutableOf(entityA, entityB);

		final int[] stateWrapper = matchMap.computeIfAbsent(keyPair, any -> new int[]{0b1111});
		// state is the result of comparison
		// 0 : comparing
		// 1 : true
		// 2 : false
		// 3 : not compared
		final int state = stateWrapper[0] >> (identicalMatch ? 0 : 2) & 0b11;
		if (state <= 2) return state < 2;
		stateWrapper[0] &= 3 << (identicalMatch ? 2 : 0);
		final boolean matchResult = EntityPartialMatcher.internalMatch(entityA, entityB, this, identicalMatch);
		if (identicalMatch != matchResult || stateWrapper[0] >> (identicalMatch ? 2 : 0) != 0b11) {
			stateWrapper[0] |= matchResult ? 0b0100 : 0b0001;
		} else {
			stateWrapper[0] = matchResult ? 0b0101 : 0b1010;
		}
		return matchResult;
	}

	boolean matchOrdered(@Nonnull Collection<? extends JavaIdentifiedEntity> collectionA,
			@Nonnull Collection<? extends JavaIdentifiedEntity> collectionB, boolean identicalMatch) {
		if (collectionA.size() != collectionB.size()) return false;
		final Iterator<? extends JavaIdentifiedEntity> iteratorA = collectionA.iterator();
		final Iterator<? extends JavaIdentifiedEntity> iteratorB = collectionB.iterator();
		while (iteratorA.hasNext()/* && iteratorB.hasNext()*/) {
			if (!match(iteratorA.next(), iteratorB.next(), identicalMatch)) return false;
		}
		return true;
	}

	boolean matchNonOrdered(@Nonnull Collection<? extends JavaIdentifiedEntity> collectionA,
			@Nonnull Collection<? extends JavaIdentifiedEntity> collectionB, boolean identicalMatch) {
		if (collectionA.size() != collectionB.size()) return false;
		final Map<EntityWrapper, int[]> map = new HashMap<>();
		for (final JavaIdentifiedEntity entity : collectionA) {
			final EntityWrapper wrapper = entity != null ? wrap(entity, identicalMatch) : null;
			final int[] countWrapper = map.computeIfAbsent(wrapper, EntityMatcher::newIntWrapper);
			countWrapper[0] += 1;
		}
		for (final JavaIdentifiedEntity entity : collectionB) {
			final EntityWrapper wrapper = entity != null ? wrap(entity, identicalMatch) : null;
			final int[] countWrapper = map.get(wrapper);
			if (countWrapper == null || --countWrapper[0] < 0) return false;
		}
		return true;
	}

	@Nonnull
	EntityWrapper wrap(@Nonnull JavaIdentifiedEntity entity, boolean identicalMatch) {
		return identicalMatch
				? identicalCodeMap.computeIfAbsent(entity, this::identicalWrapper)
				: similarCodeMap.computeIfAbsent(entity, this::similarWrapper);
	}

	@Nonnull
	private static int[] newIntWrapper(@Nullable EntityWrapper wrapper) {
		return new int[]{0};
	}

	@Nonnull
	private EntityWrapper identicalWrapper(@Nonnull JavaIdentifiedEntity innerMatchable) {
		return new EntityWrapper(this, innerMatchable,
				EntityPartialMatcher.internalMatchCode(innerMatchable, true), true);
	}

	@Nonnull
	private EntityWrapper similarWrapper(@Nonnull JavaIdentifiedEntity innerMatchable) {
		return new EntityWrapper(this, innerMatchable,
				EntityPartialMatcher.internalMatchCode(innerMatchable, false), false);
	}
}
