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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

/**
 * Demonstrate how to start a custom JConsole
 * 
 * @author Benoit Lacelle
 *
 */
public class BufferMonitorUpdater extends SwingWorker<Object, Object> {

	// SLF4J in not available in the JConsole
	protected static final Logger LOGGER = Logger.getLogger(BufferMonitorJPanel.class.getName());

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
			LOGGER.log(Level.WARNING, "Ouch", e);
		}
	}
}