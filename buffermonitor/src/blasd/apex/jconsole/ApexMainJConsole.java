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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrate how to start a custom JConsole
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexMainJConsole {
	protected ApexMainJConsole() {
		// hidden
	}

	// http://docs.oracle.com/javase/7/docs/technotes/guides/management/jconsole.html
	public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
		String pathToJar = getPathToJar(BufferMonitorJConsolePlugin.class);
		System.out.println(pathToJar);

		List<String> cmd = new ArrayList<>();

		boolean jdk7 = true;

		{
			boolean defaultJdkFodler = true;
			String jconsoleFolder;
			if (defaultJdkFodler) {
				jconsoleFolder = "C:\\Program Files\\Java\\jdk1.8.0_92\\bin\\";
			} else {
				if (jdk7) {
					jconsoleFolder = "C:\\HOMEWARE\\ITEC-Toolbox\\apps\\jdk\\jdk1.7.0_72-windows-x64\\bin\\";
				} else {
					jconsoleFolder = "C:\\HOMEWARE\\ITEC-Toolbox\\apps\\jdk\\jdk1.8.0_25-windows-x64\\bin\\";
				}
			}

			cmd.add(jconsoleFolder + "jconsole.exe");
		}

		boolean runInJarFolder = false;

		String folder = "D:\\blacelle112212\\workspace4.4\\apex\\shared\\buffermonitor\\target";
		folder = "C:\\NB5419\\workspace\\apex\\shared\\buffermonitor\\target";

		boolean useBufferMonitorPlugin = true;
		if (useBufferMonitorPlugin) {
			cmd.add("-pluginpath");

			String pluginPath = "";
			if (!runInJarFolder) {
				pluginPath += folder;
			}
			pluginPath += "\\apex-buffermonitor-2.2-SNAPSHOT.jar";
			cmd.add(pluginPath);
		}

		boolean debug = false;

		if (debug) {
			cmd.add("-J-Xdebug");
			cmd.add("-J-Xnoagent");
			cmd.add("-J-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000");
		}

		ProcessBuilder pb = new ProcessBuilder(cmd);
		if (runInJarFolder) {
			pb = pb.directory(new File(folder));
		}
		pb = pb.inheritIO();

		// JDK_HOME/bin/jconsole
		Process p = pb.start();

		int code = p.waitFor();

		System.out.println(code);
	}

	// http://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
	public static String getPathToJar(Class<?> clazz) throws URISyntaxException {
		// ClassLoader.getSystemClassLoader().getResource(".").getPath();
		// String decodedPath = URLDecoder.decode(path, "UTF-8");
		return clazz.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
	}
}
