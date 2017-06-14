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
package blasd.apex.core.jmx;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Holder for BASIC authentication
 * 
 * @author Benoit Lacelle
 *
 */
public final class ApexBasicConnectionDTO {
	public final String host;
	public final int port;
	public final String userName;
	public final String password;

	public ApexBasicConnectionDTO(String host, int port, String userName, String password) {
		this.host = host;
		this.port = port;
		this.userName = userName;
		this.password = password;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("host", host)
				.add("port", port)
				.add("userName", userName)
				// Hidden for safety
				.add("password", "XXXX")
				.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(host, port, userName, password);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ApexBasicConnectionDTO other = (ApexBasicConnectionDTO) obj;
		if (host == null) {
			if (other.host != null) {
				return false;
			}
		} else if (!host.equals(other.host)) {
			return false;
		}
		if (password == null) {
			if (other.password != null) {
				return false;
			}
		} else if (!password.equals(other.password)) {
			return false;
		}
		if (port != other.port) {
			return false;
		}
		if (userName == null) {
			if (other.userName != null) {
				return false;
			}
		} else if (!userName.equals(other.userName)) {
			return false;
		}
		return true;
	}

}
