package mo.umac.weha.diff.sentence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.data.Sentence;

public class SentenceMatch extends SentenceEdit {
	
	public SentenceMatch(List<Sentence> oldSList, List<Sentence> newSList) {
		super(1.0, oldSList, newSList);
	}

	public SentenceMatch(TreeSet<Sentence> oldSSet, TreeSet<Sentence> newSSet, boolean isMov) {
		super(oldSSet, newSSet);
		this.matchingRate = 1.0;
		this.isMovement = isMov;
	}
	
	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("SentenceMatch [matchingRate=").append(matchingRate)
				.append(", oldPos=").append(oldPos).append(", ");
		if (oldSentences != null)
			builder.append("oldSentences=").append(
					toString(oldSentences, maxLen)).append(", ");
		builder.append("newPos=").append(newPos).append(", ");
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
		
		Sentence lastOldSent = null;
		Sentence lastNewSent = null;
		Paragraph oldPara = null;
		Paragraph newPara = null;
		
		Iterator<Sentence> nsIter = newSentences.iterator();
		
		for (Sentence oldS : oldSentences) {
			Sentence newS = nsIter.next();
			if (oldPara != oldS.getParagraph() || newPara != newS.getParagraph()) {
				if (oldPara != null && newPara != null) {
					TreeSet<Sentence> osSet = new TreeSet<Sentence>(oldSentences.subSet(lastOldSent, oldS));
					TreeSet<Sentence> nsSet = new TreeSet<Sentence>(newSentences.subSet(lastNewSent, newS));
					sentEdits.add(new SentenceMatch(osSet, nsSet, this.isMovement));
				}
				
				oldPara = oldS.getParagraph();
				newPara = newS.getParagraph();
				lastOldSent = oldS;
				lastNewSent = newS;
			}
		}
		
		TreeSet<Sentence> osSet = new TreeSet<Sentence>(oldSentences.tailSet(lastOldSent));
		TreeSet<Sentence> nsSet = new TreeSet<Sentence>(newSentences.tailSet(lastNewSent));
		sentEdits.add(new SentenceMatch(osSet, nsSet, this.isMovement));
		
		return sentEdits;
	}
}
