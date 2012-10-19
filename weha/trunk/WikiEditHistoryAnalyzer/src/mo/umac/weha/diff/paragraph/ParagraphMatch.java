package mo.umac.weha.diff.paragraph;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.util.comparator.ParagraphPositionComparator;

public class ParagraphMatch extends ParagraphEdit {

	public ParagraphMatch(List<Paragraph> oldParaList, List<Paragraph> newParaList) {
		this.oldParagraphs = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		this.oldParagraphs.addAll(oldParaList);
		this.oldParagraphsLength = calculateParagraphsLength(this.oldParagraphs);
		this.oldPos = this.oldParagraphs.first().getPosition();
		
		this.newParagraphs = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		this.newParagraphs.addAll(newParaList);
		this.newParagraphsLength = calculateParagraphsLength(this.newParagraphs);
		this.newPos = this.newParagraphs.first().getPosition();
		
		this.matchingLength = this.oldParagraphsLength + this.newParagraphsLength;
		this.matchingRate	= 1.0;
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("ParagraphMatch [matchingRate=").append(String.format("%.4f", matchingRate))
				.append(", oldPos=").append(oldPos).append(", ");
		if (oldParagraphs != null)
			builder.append("oldParagraphs=").append(
					toString(oldParagraphs, maxLen)).append(", ");
		builder.append("newPos=").append(newPos).append(", ");
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
