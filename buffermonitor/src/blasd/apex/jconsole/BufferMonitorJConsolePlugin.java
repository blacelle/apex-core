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

import java.util.Collections;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingWorker;

import com.sun.tools.jconsole.JConsolePlugin;

/**
 * Demonstrate how to start a custom JConsole
 * 
 * @author Benoit Lacelle
 *
 */
// https://community.oracle.com/blogs/mandychung/2006/05/04/mustang-jconsole
public class BufferMonitorJConsolePlugin extends JConsolePlugin {

	protected BufferMonitorJPanel panel = null;

	@Override
	public Map<String, JPanel> getTabs() {
		if (panel == null) {
			panel = new BufferMonitorJPanel(this);
		}
		return Collections.<String, JPanel>singletonMap("BufferMonitor", panel);
	}

	@Override
	public SwingWorker<?, ?> newSwingWorker() {
		return new BufferMonitorUpdater(panel);
	}

}
