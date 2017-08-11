package org.eclipse.mat.snapshot;

import java.io.File;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.util.ConsoleProgressListener;

public class MainSnapshotPreparer {
	public static void main(String[] args) throws SnapshotException {
		SnapshotFactory.openSnapshot(
				new File("D:\\blacelle112212\\HeapDUmp\\20170811 Grommet Equity\\crosstie.77831.hprof"),
				new ConsoleProgressListener(System.out));
	}
}
