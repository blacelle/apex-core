package blasd.apex.primitives;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.ints.AbstractIntList;
import it.unimi.dsi.fastutil.ints.IntList;

public class ConcatIntList extends AbstractIntList {

	protected final List<IntList> intLists;

	public ConcatIntList(IntList... intLists) {
		this(Arrays.asList(intLists));
	}

	public ConcatIntList(List<IntList> intLists) {
		this.intLists = intLists;
	}

	@Override
	public int getInt(final int index) {
		Iterator<IntList> it = intLists.iterator();

		int previousSize = 0;

		while (it.hasNext()) {
			IntList next = it.next();
			int size = next.size();

			if (index >= previousSize + size) {
				previousSize += size;
				continue;
			} else {
				return next.getInt(index - previousSize);
			}
		}

		throw new ArrayIndexOutOfBoundsException("index=" + index + " while size=" + size());
	}

	@Override
	public int size() {
		return intLists.stream().mapToInt(IntList::size).sum();
	}

}
