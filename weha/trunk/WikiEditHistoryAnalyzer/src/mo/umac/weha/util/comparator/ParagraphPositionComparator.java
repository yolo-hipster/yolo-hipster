package mo.umac.weha.util.comparator;

import java.util.Comparator;

import mo.umac.weha.data.Paragraph;

public class ParagraphPositionComparator implements Comparator<Paragraph> {

	public int compare(Paragraph o1, Paragraph o2) {
		return o1.getPosition() - o2.getPosition();
	}
	
}
