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

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import blasd.apex.core.primitive.ApexParserHelper;
import javolution.text.TypeFormat;

/**
 * If Exception in thread "main" java.lang.RuntimeException: ERROR: Unable to find the resource:
 * /META-INF/BenchmarkList, "mvn clean install" or "Run as/Maven generate-sources"
 * 
 * @author Benoit Lacelle
 *
 */
public class DoubleParsingBenchmark {

	private static final String PARSE_SMALL_INTEGER = Double.toString(123);
	private static final String PARSE_BIG_LONG = Double.toString(Long.MAX_VALUE - 123);
	private static final String PARSE_ZERO = Double.toString(0D);
	private static final String PARSE_WIDE_FLOAT = Float.toString((float) Math.sqrt(2F));
	private static final String PARSE_WIDE_DOUBLE = Double.toString(Math.sqrt(2D));

	private interface StringToDouble {
		double parseDouble(CharSequence s);
	}

	/**
	 * Holds the parsers
	 * 
	 * @author Benoit Lacelle
	 *
	 */
	@State(Scope.Benchmark)
	public static class SharedCounters {
		StringToDouble jdk = s -> Double.parseDouble(s.toString());
		StringToDouble javolution = s -> TypeFormat.parseDouble(s);
		StringToDouble apex = s -> ApexParserHelper.parseDouble(s);
	}

	@Benchmark
	public double measureApexDoubleFromInteger(SharedCounters state) {
		return state.apex.parseDouble(PARSE_SMALL_INTEGER);
	}

	@Benchmark
	public double measureApexDoubleFromLong(SharedCounters state) {
		return state.apex.parseDouble(PARSE_BIG_LONG);
	}

	@Benchmark
	public double measureApexDoubleFromZero(SharedCounters state) {
		return state.apex.parseDouble(PARSE_ZERO);
	}

	@Benchmark
	public double measureApexDoubleFromWideFloat(SharedCounters state) {
		return state.apex.parseDouble(PARSE_WIDE_FLOAT);
	}

	@Benchmark
	public double measureApexDoubleFromWideDouble(SharedCounters state) {
		return state.apex.parseDouble(PARSE_WIDE_DOUBLE);
	}

	@Benchmark
	public double measureJdkDoubleFromInteger(SharedCounters state) {
		return state.jdk.parseDouble(PARSE_SMALL_INTEGER);
	}

	@Benchmark
	public double measureJdkDoubleFromLong(SharedCounters state) {
		return state.jdk.parseDouble(PARSE_BIG_LONG);
	}

	@Benchmark
	public double measureJdkDoubleFromZero(SharedCounters state) {
		return state.jdk.parseDouble(PARSE_ZERO);
	}

	@Benchmark
	public double measureJdkDoubleFromWideFloat(SharedCounters state) {
		return state.jdk.parseDouble(PARSE_WIDE_FLOAT);
	}

	@Benchmark
	public double measureApexJdkFromWideDouble(SharedCounters state) {
		return state.jdk.parseDouble(PARSE_WIDE_DOUBLE);
	}

	@Benchmark
	public double measureJavolutionDoubleFromInteger(SharedCounters state) {
		return state.javolution.parseDouble(PARSE_SMALL_INTEGER);
	}

	@Benchmark
	public double measureJavolutionDoubleFromLong(SharedCounters state) {
		return state.javolution.parseDouble(PARSE_BIG_LONG);
	}

	@Benchmark
	public double measureJavolutionDoubleFromZero(SharedCounters state) {
		return state.javolution.parseDouble(PARSE_ZERO);
	}

	@Benchmark
	public double measureJavolutionDoubleFromWideFLoat(SharedCounters state) {
		return state.javolution.parseDouble(PARSE_WIDE_FLOAT);
	}

	@Benchmark
	public double measureJavolutionDoubleFromWideDouble(SharedCounters state) {
		return state.javolution.parseDouble(PARSE_WIDE_DOUBLE);
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

			DoubleParsingBenchmark benchmark = new DoubleParsingBenchmark();
			SharedCounters state = new SharedCounters();

			// Run for 1 minute
			while (System.currentTimeMillis() < start + TimeUnit.MINUTES.toMillis(1)) {
				benchmark.measureApexDoubleFromZero(state);
			}
		}
	}
}