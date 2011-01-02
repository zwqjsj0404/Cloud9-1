/*
 * Cloud9: A MapReduce Library for Hadoop
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.umd.cloud9.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * An implementation of a conditional frequency distribution for arbitrary
 * events, backed by a fastutil open hash map. This class keeps track of
 * frequencies using ints, so beware when dealing with a large number of
 * observations.
 *
 * @author Jimmy Lin
 *
 */
public class OpenConditionalFrequencyDistribution<K extends Comparable<K>> implements ConditionalFrequencyDistribution<K> {

	private final Object2ObjectMap<K, OpenFrequencyDistribution<K>> distributions = new Object2ObjectOpenHashMap<K, OpenFrequencyDistribution<K>>();
	private final OpenFrequencyDistribution<K> marginals = new OpenFrequencyDistribution<K>();

	private long sumOfAllFrequencies = 0;

	@Override
	public void set(K k, K cond, int v) {
		if (!distributions.containsKey(cond)) {
			OpenFrequencyDistribution<K> fd = new OpenFrequencyDistribution<K>();
			fd.set(k, v);
			distributions.put(cond, fd);
			marginals.increment(k, v);

			sumOfAllFrequencies += v;
		} else {
			OpenFrequencyDistribution<K> fd = distributions.get(cond);
			int rv = fd.get(k);

			fd.set(k, v);
			distributions.put(cond, fd);
			marginals.increment(k, -rv + v);

			sumOfAllFrequencies = sumOfAllFrequencies - rv + v;
		}
	}

	@Override
	public void increment(K k, K cond) {
		increment(k, cond, 1);
	}

	@Override
	public void increment(K k, K cond, int v) {
		int cur = get(k, cond);
		if (cur == 0) {
			set(k, cond, v);
		} else {
			set(k, cond, cur + v);
		}
	}

	@Override
	public int get(K k, K cond) {
		if ( !distributions.containsKey(cond)) {
			return 0;
		}

		return distributions.get(cond).get(k);
	}

	@Override
	public int getMarginalCount(K k) {
		return marginals.get(k);
	}

	@Override
	public OpenFrequencyDistribution<K> getConditionalDistribution(K cond) {
		if ( distributions.containsKey(cond) ) {
			return distributions.get(cond);
		}

		return new OpenFrequencyDistribution<K>();
	}

	@Override
	public long getSumOfAllFrequencies() {
		return sumOfAllFrequencies;
	}

	@Override
	public void check() {
		OpenFrequencyDistribution<K> m = new OpenFrequencyDistribution<K>();

		long totalSum = 0;
		for (OpenFrequencyDistribution<K> fd : distributions.values()) {
			long conditionalSum = 0;

			for (PairOfObjectInt<K> pair : fd.getSortedEvents()) {
				conditionalSum += pair.getRightElement();
				m.increment(pair.getLeftElement(), pair.getRightElement());
			}

			if (conditionalSum != fd.getSumOfFrequencies()) {
				throw new RuntimeException("Internal Error!");
			}
			totalSum += fd.getSumOfFrequencies();
		}

		if (totalSum != getSumOfAllFrequencies()) {
			throw new RuntimeException("Internal Error! Got " + totalSum + ", Expected "	+ getSumOfAllFrequencies());
		}

		for (PairOfObjectInt<K> e : m.getSortedEvents()) {
			if ( e.getRightElement() != marginals.get(e.getLeftElement()) ) {
				throw new RuntimeException("Internal Error!");
			}
		}

		for (PairOfObjectInt<K> e : m.getSortedEvents()) {
			if ( e.getRightElement() != m.get(e.getLeftElement()) ) {
				throw new RuntimeException("Internal Error!");
			}
		}
	}
}
