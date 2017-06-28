/**
 * The MIT License
 * Copyright (c) 2014 Benoit Lacelle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
