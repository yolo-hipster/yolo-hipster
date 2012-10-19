package mo.umac.weha.diff.sentence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.data.Sentence;

public class SentenceInsert extends SentenceEdit {

	public SentenceInsert(Sentence sentence) {
		super(0.0, null, sentence);
	}

	public SentenceInsert(TreeSet<Sentence> nsSet) {
		super(null, nsSet);
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("SentenceInsert [newPos=").append(newPos).append(", ");
		if (newSentences != null)
			builder.append("newSentences=").append(
					toString(newSentences, maxLen));
		builder.append("]");
		return builder.toString();
	}

	private String toString(Collection<Sentence> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<Sentence> iterator = collection.iterator(); iterator.hasNext()
				&& i < maxLen; i++) {
			if (i > 0)
				builder.append(", ");
			builder.append(iterator.next().getPosition());
		}
		builder.append("]");
		return builder.toString();
	}

	public List<SentenceEdit> splitByParagraph() {
		List<SentenceEdit> sentEdits = new ArrayList<SentenceEdit>();
		
		Sentence lastNewSent = null;
		Paragraph newPara = null;
		
		for (Sentence newS : newSentences) {
			if (newPara == null) {
				newPara = newS.getParagraph();
				lastNewSent = newS;
			}
			
			if (newPara != newS.getParagraph() || newS.equals(newSentences.last())) {
				TreeSet<Sentence> nsSet = new TreeSet<Sentence>(newSentences.subSet(lastNewSent, true, newS, newS.equals(newSentences.last())));
				if (!nsSet.isEmpty()) {
					sentEdits.add(new SentenceInsert(nsSet));
				}
				
				newPara = newS.getParagraph();
				lastNewSent = newS;
			}
		}
		
		return sentEdits;
	}
	
}
