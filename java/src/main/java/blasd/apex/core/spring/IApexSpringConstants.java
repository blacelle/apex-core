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
package blasd.apex.core.spring;

/**
 * Some constants to helps using Spring profiles
 * 
 * @author Benoit Lacelle
 *
 */
public interface IApexSpringConstants {

	/**
	 * -Dspring.profiles.active=apex.offline
	 */
	String SPRING_PROFILE_DISCONNECTED = "apex.offline";

	/**
	 * -Dspring.profiles.active=apex.offline
	 */
	String SPRING_PROFILE_NOT_DISCONNECTED = "!" + SPRING_PROFILE_DISCONNECTED;
}
