/*******************************************************************************
 * Copyright (c) 2008 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.mat.tests.regression;

/* package */class PerfData {
	private String testName;
	private String time;

	public PerfData(String testName, String time) {
		this.testName = testName;
		this.time = time;
	}

	public String getTestName() {
		return testName;
	}

	public String getTime() {
		return time;
	}
}
