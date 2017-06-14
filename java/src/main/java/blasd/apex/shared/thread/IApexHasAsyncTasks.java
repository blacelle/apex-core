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

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Some abstraction for data-structures doing asynchronous processes. Useful to collect them all and wait for the end of
 * all asynchronous processes.
 * 
 * @author Benoit Lacelle
 *
 */
@ManagedResource
public interface IApexHasAsyncTasks {

	/**
	 * 
	 * @return true if we have some pending tasks, maybe not already started
	 */
	@ManagedAttribute
	boolean getHasPending();

	/**
	 * 
	 * @return true if this bean is active, synchronously or asynchronously
	 */
	@ManagedAttribute
	boolean getIsActive();
}
