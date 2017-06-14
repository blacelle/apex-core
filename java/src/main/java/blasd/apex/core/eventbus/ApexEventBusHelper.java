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
package blasd.apex.core.eventbus;

import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.eventbus.EventBus;

/**
 * Helps working with an EventBus
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexEventBusHelper {
	protected ApexEventBusHelper() {
		// hidden
	}

	public static Optional<Consumer<Object>> asConsumer(EventBus eventBus) {
		return Optional.ofNullable(eventBus).<Consumer<Object>>map(eb -> eb::post);
	}

}
