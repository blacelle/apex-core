package org.eclipse.mat.snapshot;

import java.io.File;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.util.ConsoleProgressListener;

/**
 * Typically used to parse a heap-dump. While trying to diminish the heap of mat: -XX:+HeapDumpOnOutOfMemoryError
 * -XX:HeapDumpPath=/disk2/dumps
 * 
 * @author Benoit Lacelle
 *
 */
public class MainSnapshotPreparer {
	public static void main(String[] args) throws SnapshotException {
		// SnapshotFactory.openSnapshot(
		// new File("D:\\blacelle112212\\HeapDUmp\\20170811 Grommet Equity\\crosstie.77831.hprof"),
		// new ConsoleProgressListener(System.out));

		SnapshotFactory.openSnapshot(new File("C:\\NB5419\\HeapDumps\\java_pid21052.hprof"),
				new ConsoleProgressListener(System.out));
	}
}
