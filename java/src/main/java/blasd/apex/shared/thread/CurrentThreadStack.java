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
package blasd.apex.shared.thread;

/**
 * Producing a Throwable is the fastest way to retrieve current thread stack.
 * 
 * 'new Exception().getStackTrace()' is much faster than 'Thread.currentThread().getStackTrace()'
 * http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6375302
 * 
 * We use a name which does not refer to Exception as this is NOT an exception. it should be used to produce logs
 * referring to current thread stack.
 * 
 * We extends {@link Throwable} as we do no expect to use this for rethrowing
 * 
 * @author Benoit Lacelle
 *
 */
public class CurrentThreadStack extends Throwable {
	private static final long serialVersionUID = -4426770891772850366L;

	public static CurrentThreadStack snapshot() {
		return new CurrentThreadStack();
	}

	public static StackTraceElement[] snapshotStackTrace() {
		return snapshot().getStackTrace();
	}

}
