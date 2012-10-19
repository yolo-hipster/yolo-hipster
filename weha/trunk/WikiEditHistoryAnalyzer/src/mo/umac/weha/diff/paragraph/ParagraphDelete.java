package mo.umac.weha.diff.paragraph;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.util.comparator.ParagraphPositionComparator;

public class ParagraphDelete extends ParagraphEdit {

	protected ParagraphDelete(Paragraph op) {
		this.oldParagraphs = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		this.oldParagraphs.add(op);
		this.oldParagraphsLength = calculateParagraphsLength(this.oldParagraphs);
		this.oldPos = this.oldParagraphs.first().getPosition();
		
		this.newParagraphs = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		this.newParagraphsLength = 0;
		this.newPos = -1;
		
		this.matchingLength = 0;
		this.matchingRate = 0.0;
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("ParagraphDelete [oldPos=").append(oldPos).append(", ");
		if (oldParagraphs != null)
			builder.append("oldParagraphs=").append(
					toString(oldParagraphs, maxLen));
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
