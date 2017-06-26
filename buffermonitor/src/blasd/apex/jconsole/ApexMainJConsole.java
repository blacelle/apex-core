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
