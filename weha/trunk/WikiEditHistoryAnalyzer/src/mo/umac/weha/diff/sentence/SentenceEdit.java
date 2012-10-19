package mo.umac.weha.diff.sentence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.data.Sentence;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.token.TokenDelete;
import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.diff.token.TokenInsert;
import mo.umac.weha.diff.token.TokenMatch;
import mo.umac.weha.diff.token.TokenReplace;
import mo.umac.weha.util.comparator.SentencePositionComparator;

public class SentenceEdit implements AbstractEdit {
	
	protected int oldPos;
	protected int newPos;
		
	protected TreeSet<Sentence> oldSentences;
	protected TreeSet<Sentence> newSentences;
	
	protected int oldSentencesLength;
	protected int newSentencesLength;
	
	protected int matchingLength;
	protected double matchingRate;
	
	protected boolean isMovement;
	
	public SentenceEdit(double mr, List<Sentence> oldSentence, List<Sentence> newSentence) {		
		this.oldSentences = new TreeSet<Sentence>(new SentencePositionComparator());
		this.oldSentences.addAll(oldSentence);
		this.oldSentencesLength = calculateSentencesLength(this.oldSentences);
		this.oldPos = this.oldSentences.first().getPosition();
		
		this.newSentences = new TreeSet<Sentence>(new SentencePositionComparator());
		this.newSentences.addAll(newSentence);
		this.newSentencesLength = calculateSentencesLength(this.newSentences);
		this.newPos = this.newSentences.first().getPosition();
		
		this.matchingLength = calculateMatchingLength();
		this.matchingRate = mr;
	}

	public SentenceEdit(double mr, Sentence oldSentence, Sentence newSentence) {
		this.matchingRate = mr;
		
		this.oldSentences = new TreeSet<Sentence>(new SentencePositionComparator());
		if (oldSentence != null) {
			this.oldSentences.add(oldSentence);
			this.oldSentencesLength = calculateSentencesLength(this.oldSentences);
			this.oldPos = this.oldSentences.first().getPosition();
		}
		else {
			this.oldPos = -1;
		}
		
		this.newSentences = new TreeSet<Sentence>(new SentencePositionComparator());
		if (newSentence != null) {
			this.newSentences.add(newSentence);
			this.newSentencesLength = calculateSentencesLength(this.newSentences);
			this.newPos = this.newSentences.first().getPosition();
		}
		else {
			this.newPos = -1;
		}
		
		this.matchingLength = calculateMatchingLength();
		this.matchingRate = mr;
	}

	public SentenceEdit(SentenceEdit sentenceEdit) {
		this.oldSentences = new TreeSet<Sentence>(new SentencePositionComparator());
		if (!sentenceEdit.oldSentences.isEmpty()) {
			this.oldSentences.addAll(sentenceEdit.oldSentences);
			this.oldSentencesLength = calculateSentencesLength(this.oldSentences);
			this.oldPos = this.oldSentences.first().getPosition();
		}
		else {
			this.oldPos = -1;
		}
		
		this.newSentences = new TreeSet<Sentence>(new SentencePositionComparator());
		if (!sentenceEdit.newSentences.isEmpty()) {
			this.newSentences.addAll(sentenceEdit.newSentences);
			this.newSentencesLength = calculateSentencesLength(this.newSentences);
			this.newPos = this.newSentences.first().getPosition();
		}
		else {
			this.newPos = -1;
		}
		
		this.matchingLength = sentenceEdit.matchingLength;
		this.matchingRate = sentenceEdit.matchingRate;
	}

	protected SentenceEdit(TreeSet<Sentence> osSet, TreeSet<Sentence> nsSet) {
		if (osSet != null) {
			this.oldSentences = new TreeSet<Sentence>(osSet);
			this.oldSentencesLength = calculateSentencesLength(this.oldSentences);
			this.oldPos = this.oldSentences.first().getPosition();
		}
		else {
			this.oldSentences = new TreeSet<Sentence>(new SentencePositionComparator());
			this.oldSentencesLength = 0;
			this.oldPos = -1;
		}
		
		if (nsSet != null) {
			this.newSentences = new TreeSet<Sentence>(nsSet);
			this.newSentencesLength = calculateSentencesLength(this.newSentences);
			this.newPos = this.newSentences.first().getPosition();
		}
		else {
			this.newSentences = new TreeSet<Sentence>(new SentencePositionComparator());
			this.newSentencesLength = 0;
			this.newPos = -1;
		}
		
		this.matchingLength = calculateMatchingLength();
		this.matchingRate = this.matchingLength * 2.0 / (double) (this.oldSentencesLength + this.newSentencesLength);
	}

	protected SentenceEdit(TreeSet<Sentence> osSet, TreeSet<Sentence> nsSet, boolean isMov) {
		this(osSet, nsSet);
		this.isMovement = isMov;
	}

	public SentenceEdit mergeWith(SentenceEdit edit) {
		// Merge contained sentences and update position
		this.oldSentences.addAll(edit.oldSentences);
		this.newSentences.addAll(edit.newSentences);

		this.oldSentencesLength = calculateSentencesLength(this.oldSentences);
		this.newSentencesLength = calculateSentencesLength(this.newSentences);
		
		this.oldPos = this.oldSentences.first().getPosition();
		this.newPos = this.newSentences.first().getPosition();
		
		// Update matching rate
		this.matchingLength = calculateMatchingLength();
		this.matchingRate = this.matchingLength * 2.0 / (double) (this.oldSentencesLength + this.newSentencesLength);
		
		return this;
	}
	
	private int calculateSentencesLength(TreeSet<Sentence> sentencesSet) {
		int len = 0;
		
		for (Sentence s : sentencesSet) {
			len += s.getLength();
		}
		
		return len;
	}
	
	private int calculateMatchingLength() {
		int matchingLen = 0;
		
		// Extract matches from old sentences
		Set<TokenMatch> oldMatchSet = new HashSet<TokenMatch>();
		for (Sentence s : this.oldSentences) {
			List<TokenEdit> teList = s.getTokenEdits();
			
			if (teList == null) {
				matchingLen += s.getFullLength();
			}
			else {
				for (TokenEdit te : teList) {
					if (te instanceof TokenMatch) {
						oldMatchSet.add((TokenMatch) te);
					}
				}
			}
		}
		
		// Extract matches from new sentences
		Set<TokenMatch> newMatchSet = new HashSet<TokenMatch>();
		for (Sentence s : this.newSentences) {
			List<TokenEdit> teList = s.getTokenEdits();
			
			if (teList == null) {
				matchingLen += s.getFullLength();
			}
			else { 
				for (TokenEdit te : teList) {
					if (te instanceof TokenMatch) {
						newMatchSet.add((TokenMatch) te);
					}
				}
			}
		}
		
		// Intersect (only matches contain in both sets retain)
		oldMatchSet.retainAll(newMatchSet);
		
		for (TokenMatch tm : oldMatchSet) {
			matchingLen += tm.getFullLength();
		}
		
		return (matchingLen / 2);
	}

	public boolean containsNewSentence(Sentence sentence) {
		return newSentences.contains(sentence);
	}
	
	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("SentenceEdit [matchingRate=").append(String.format("%.4f", matchingRate))
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

	public TreeSet<Sentence> getOldSentences() {
		return oldSentences;
	}

	public TreeSet<Sentence> getNewSentences() {
		return newSentences;
	}

	public int getOldPos() {
		return oldPos;
	}

	public int getNewPos() {
		return newPos;
	}

	public double getMatchingRate() {
		return matchingRate;
	}
	
	public int getMatchingLength() {
		return matchingLength;
	}

	public double mergedMatchingRate(SentenceEdit edit) {
		SentenceEdit tempEdit = new SentenceEdit(this);
		tempEdit.mergeWith(edit);
		
		return tempEdit.getMatchingRate();
	}

	public void labelAsMoved() {
		this.isMovement = true;
	}
	
	public boolean isMovement() {
		return this.isMovement;
	}
	
	public List<SentenceEdit> splitByParagraph() {
		List<SentenceEdit> sentEdits = new ArrayList<SentenceEdit>();
		
		Sentence lastOldSent = null;
		Sentence lastNewSent = null;
		Paragraph oldPara = null;
		Paragraph newPara = null;
		
		Iterator<Sentence> oldIter = oldSentences.iterator();
		Iterator<Sentence> newIter = newSentences.iterator();
		
		while (oldIter.hasNext()) {
			Sentence oldS = oldIter.next();
			if (oldPara == null) {
				oldPara = oldS.getParagraph();
				lastOldSent = oldS;
			}
						
			if (oldPara != oldS.getParagraph()) {
				TreeSet<Sentence> osSet = new TreeSet<Sentence>(oldSentences.subSet(lastOldSent, oldS));
			
				while (newIter.hasNext()) {
					Sentence newS = newIter.next();
					if (newPara == null) {
						newPara = newS.getParagraph();
						lastNewSent = newS;
					}
					
					if (newPara != newS.getParagraph()) {
						TreeSet<Sentence> nsSet = new TreeSet<Sentence>(newSentences.subSet(lastNewSent, newS));
						sentEdits.add(createEdit(osSet, nsSet));
						
						newPara = newS.getParagraph();
						lastNewSent = newS;
						break;
					}
				}
				
				TreeSet<Sentence> lastNsSet = new TreeSet<Sentence>(newSentences.tailSet(lastNewSent));
				if (!newIter.hasNext() && !lastNsSet.isEmpty()) {
					sentEdits.add(createEdit(osSet, lastNsSet));
				}
				
				oldPara = oldS.getParagraph();
				lastOldSent = oldS;
			}
		}
		
		TreeSet<Sentence> lastOsSet = new TreeSet<Sentence>(oldSentences.tailSet(lastOldSent));
		while (newIter.hasNext()) {
			Sentence newS = newIter.next();
			if (newPara == null) {
				newPara = newS.getParagraph();
				lastNewSent = newS;
			}
			
			if (newPara != newS.getParagraph()) {
				TreeSet<Sentence> nsSet = new TreeSet<Sentence>(newSentences.subSet(lastNewSent, newS));
				sentEdits.add(createEdit(lastOsSet, nsSet));
				
				newPara = newS.getParagraph();
				lastNewSent = newS;
			}
		}
		
		TreeSet<Sentence> lastNsSet = new TreeSet<Sentence>(newSentences.tailSet(lastNewSent));
		SentenceEdit lastEdit = createEdit(lastOsSet, lastNsSet);
		if (lastEdit != null) {
			sentEdits.add(lastEdit);
		}
		
		return sentEdits;
	}
	
	private SentenceEdit createEdit(TreeSet<Sentence> osSet, TreeSet<Sentence> nsSet) {
		SentenceEdit retEdit = null;
		
		if (!osSet.isEmpty() && !nsSet.isEmpty()) {
			retEdit = new SentenceEdit(osSet, nsSet, this.isMovement);
		}
		else if (!osSet.isEmpty()) {
			retEdit = new SentenceDelete(osSet);
		}
		else if (!nsSet.isEmpty()) {
			retEdit = new SentenceInsert(nsSet);
		}
		
		return retEdit;
	}

	public List<TokenEdit> getTokenEdits() {
		HashSet<TokenEdit> oldTeSet = new HashSet<TokenEdit>();
		LinkedList<TokenEdit> retList = new LinkedList<TokenEdit>();
		
		if (this.oldSentences != null) {
			for (Sentence op : this.oldSentences) {
				List<TokenEdit> teList = op.getTokenEdits();
				if (teList == null) {
					continue;
				}
				
				for (TokenEdit te : teList) {
					if (te instanceof TokenDelete || te instanceof TokenReplace) {
						retList.add(te);
					}
					else {
						oldTeSet.add(te);
					}
				}
			}
		}
		
		if (this.newSentences != null) {
			for (Sentence np : this.newSentences) {
				List<TokenEdit> teList = np.getTokenEdits();
				if (teList == null) {
					continue;
				}
				
				for (TokenEdit te : teList) {
					if (te instanceof TokenInsert || te instanceof TokenReplace) {
						retList.add(te);
					}
					else if (oldTeSet.contains(te)) {
						retList.add(te);
						oldTeSet.remove(te);
					}
				}
			}
		}
		
		return retList;
	}
	
}
