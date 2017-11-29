package blasd.apex.hadoop;

import java.io.FileNotFoundException;

import org.apache.hadoop.util.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some basic utilities for Hadoop
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexHadoopHelper {
	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexHadoopHelper.class);

	protected ApexHadoopHelper() {
		// hidden
	}

	/**
	 * 
	 * @return true if we already have the property env "hadoop.home.dir", or we succeed finding a good value for it
	 */
	public static boolean isHadoopReady() {
		if (Shell.WINDOWS) {
			try {
				if (Shell.getWinUtilsFile().isFile()) {
					return true;
				}
			} catch (FileNotFoundException e) {
				// https://wiki.apache.org/hadoop/WindowsProblems
				LOGGER.trace("Wintutils seems to be missing", e);
			}
		}

		// If we get here, it means winutils is missing
		LOGGER.error(
				"Haddop winutils seems not installed. They can be checked-out from 'git clone https://github.com/steveloughran/winutils.git'");
		return false;
	}
}
