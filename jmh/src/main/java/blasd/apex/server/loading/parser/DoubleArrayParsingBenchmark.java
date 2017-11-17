/**
 * Copyright (C) 2014 Benoit Lacelle (benoit.lacelle@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package blasd.apex.server.loading.parser;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.quartetfs.fwk.format.impl.AVectorParser;
import com.quartetfs.fwk.format.impl.DoubleVectorParser;

/**
 * If Exception in thread "main" java.lang.RuntimeException: ERROR: Unable to find the resource:
 * /META-INF/BenchmarkList, "mvn clean install" or "Run as/Maven generate-sources"
 * 
 * @author Benoit Lacelle
 *
 */
public class DoubleArrayParsingBenchmark {
	// Consider parsing arrays of 512 doubles
	private static final int NB_DOUBLES = 512;

	private static final String PARSE_SMALL_INTEGERS =
			IntStream.range(0, NB_DOUBLES).mapToObj(index -> Double.toString(index)).collect(
					Collectors.joining(Character.toString(AVectorParser.getDefaultDelimiter())));

	private static final String PARSE_BIG_LONGS = IntStream.range(0, NB_DOUBLES)
			.mapToLong(i -> Long.MAX_VALUE - i)
			.mapToObj(index -> Double.toString(index))
			.collect(Collectors.joining(Character.toString(AVectorParser.getDefaultDelimiter())));

	private static final String PARSE_ZEROES =
			IntStream.range(0, NB_DOUBLES).mapToObj(index -> Double.toString(0D)).collect(
					Collectors.joining(Character.toString(AVectorParser.getDefaultDelimiter())));

	private static final Random RANDOM = new Random(0);
	private static final String PARSE_RANDOM =
			IntStream.range(0, NB_DOUBLES).mapToObj(d -> Double.toString(RANDOM.nextDouble())).collect(
					Collectors.joining(Character.toString(AVectorParser.getDefaultDelimiter())));

	private static final String SINGLE_WIDE_DOUBLE =
			IntStream.range(0, NB_DOUBLES).mapToObj(d -> Double.toString(Math.sqrt(2D))).collect(
					Collectors.joining(Character.toString(AVectorParser.getDefaultDelimiter())));

	/**
	 * Holds the parsers
	 * 
	 * @author Benoit Lacelle
	 *
	 */
	@State(Scope.Benchmark)
	public static class SharedCounters {
		ApexDoubleArrayParser apex = new ApexDoubleArrayParser();
		DoubleVectorParser core = new DoubleVectorParser();
	}

	@Benchmark
	public double[] measureApexDoubleFromIntegers(SharedCounters state) {
		return state.apex.parse(PARSE_SMALL_INTEGERS);
	}

	@Benchmark
	public double[] measureApexDoubleFromLongs(SharedCounters state) {
		return state.apex.parse(PARSE_BIG_LONGS);
	}

	@Benchmark
	public double[] measureApexDoubleFromRandom(SharedCounters state) {
		return state.apex.parse(PARSE_RANDOM);
	}

	@Benchmark
	public double[] measureApexDoubleFromZeroes(SharedCounters state) {
		return state.apex.parse(PARSE_ZEROES);
	}

	@Benchmark
	public double[] measureApexDoubleFromSameButWide(SharedCounters state) {
		return state.apex.parse(SINGLE_WIDE_DOUBLE);
	}

	@Benchmark
	public double[] measureCoreDoubleFromIntegers(SharedCounters state) {
		return state.core.parse(PARSE_SMALL_INTEGERS);
	}

	@Benchmark
	public double[] measureCoreDoubleFromLongs(SharedCounters state) {
		return state.core.parse(PARSE_BIG_LONGS);
	}

	@Benchmark
	public double[] measureCoreDoubleFromRandom(SharedCounters state) {
		return state.core.parse(PARSE_RANDOM);
	}

	@Benchmark
	public double[] measureCoreDoubleFromZeroes(SharedCounters state) {
		return state.core.parse(PARSE_ZEROES);
	}

	@Benchmark
	public double[] measureCoreDoubleFromSameButWide(SharedCounters state) {
		return state.core.parse(SINGLE_WIDE_DOUBLE);
	}

	public static final int WARMUP_ITERATIONS = 3;
	public static final int MEASUREMENTS_ITERATIONS = 3;

	public static void main(String... args) throws Exception {
		Options opts = new OptionsBuilder().include(".*")
				.warmupIterations(WARMUP_ITERATIONS)
				.measurementIterations(MEASUREMENTS_ITERATIONS)
				// .jvmArgs("-server")
				.forks(1)
				// .outputFormat(OutputFormatType.TextReport)
				.build();

		new Runner(opts).run();
	}

	public static final int NB_ITERATIONS = 1000000;

	/**
	 * Enable running the JMH test in a plain JVM, in order to collect Jit data for JitWatch
	 * 
	 * @author Benoit Lacelle
	 *
	 */
	public static class JitWatchMain {
		public static void main(String[] args) {
			long start = System.currentTimeMillis();

			DoubleArrayParsingBenchmark benchmark = new DoubleArrayParsingBenchmark();
			SharedCounters state = new SharedCounters();

			// Run for 1 minute
			while (System.currentTimeMillis() < start + TimeUnit.MINUTES.toMillis(1)) {
				benchmark.measureCoreDoubleFromRandom(state);
			}
		}
	}
}