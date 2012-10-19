package mo.umac.weha.diff.paragraph;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.util.comparator.ParagraphPositionComparator;

public class ParagraphInsert extends ParagraphEdit {

	protected ParagraphInsert(Paragraph np) {
		this.oldParagraphs = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		this.oldParagraphsLength = 0;
		this.oldPos = -1;
		
		this.newParagraphs = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		this.newParagraphs.add(np);
		this.newParagraphsLength = calculateParagraphsLength(this.newParagraphs);
		this.newPos = this.newParagraphs.first().getPosition();
		
		this.matchingLength = 0;
		this.matchingRate = 0.0;
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("ParagraphInsert [newPos=").append(newPos).append(", ");
		if (newParagraphs != null)
			builder.append("newParagraphs=").append(
					toString(newParagraphs, maxLen));
		builder.append("]");
		return builder.toString();
	}

	private String toString(Collection<Paragraph> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<Paragraph> iterator = collection.iterator(); iterator.hasNext()
				&& i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next().getPosition());
		}
		builder.append("]");
		return builder.toString();
	}

}
