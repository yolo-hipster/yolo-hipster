package mo.umac.weha.util.comparator;

import java.util.Comparator;

import mo.umac.weha.data.Sentence;

public class SentencePositionComparator implements Comparator<Sentence> {

	public int compare(Sentence o1, Sentence o2) {
		return o1.getPosition() - o2.getPosition();
	}
	
}
