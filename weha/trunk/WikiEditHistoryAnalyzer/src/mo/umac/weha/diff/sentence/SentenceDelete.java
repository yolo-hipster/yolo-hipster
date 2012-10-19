package mo.umac.weha.diff.sentence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.data.Sentence;

public class SentenceDelete extends SentenceEdit {

	public SentenceDelete(Sentence sentence) {
		super(0.0, sentence, null);
	}

	public SentenceDelete(TreeSet<Sentence> osSet) {
		super(osSet, null);
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("SentenceDelete [oldPos=").append(oldPos).append(", ");
		if (oldSentences != null)
			builder.append("oldSentences=").append(
					toString(oldSentences, maxLen));
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
		
		Sentence lastOldSent = null;
		Paragraph oldPara = null;
		
		for (Sentence oldS : oldSentences) {
			if (oldPara == null) {
				oldPara = oldS.getParagraph();
				lastOldSent = oldS;
			}
			
			if (oldPara != oldS.getParagraph() || oldS.equals(oldSentences.last())) {
				TreeSet<Sentence> osSet = new TreeSet<Sentence>(oldSentences.subSet(lastOldSent, true, oldS, oldS.equals(oldSentences.last())));
				if (!osSet.isEmpty()) {
					sentEdits.add(new SentenceDelete(osSet));
				}
				
				oldPara = oldS.getParagraph();
				lastOldSent = oldS;
			}
		}
		
		return sentEdits;
	}
	
}
