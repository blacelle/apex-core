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
package blasd.apex.jconsole;

import javax.swing.SwingWorker;

/**
 * Demonstrate how to start a custom JConsole
 * 
 * @author Benoit Lacelle
 *
 */
public class BufferMonitorUpdater extends SwingWorker<Object, Object> {
	protected final BufferMonitorJPanel jconsolePlugin;

	public BufferMonitorUpdater(BufferMonitorJPanel jconsolePlugin) {
		this.jconsolePlugin = jconsolePlugin;
	}

	@Override
	protected Object doInBackground() throws Exception {
		jconsolePlugin.refreshValues();

		return null;
	}

	@Override
	protected void done() {
		try {
			jconsolePlugin.repaint();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
}