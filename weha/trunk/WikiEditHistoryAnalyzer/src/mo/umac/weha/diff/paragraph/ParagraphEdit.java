package mo.umac.weha.diff.paragraph;

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
import mo.umac.weha.diff.sentence.SentenceDelete;
import mo.umac.weha.diff.sentence.SentenceEdit;
import mo.umac.weha.diff.sentence.SentenceInsert;
import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.diff.token.TokenMatch;
import mo.umac.weha.util.comparator.ParagraphPositionComparator;

public class ParagraphEdit implements AbstractEdit {

	protected int oldPos;
	protected int newPos;
	
	protected TreeSet<Paragraph> oldParagraphs;
	protected TreeSet<Paragraph> newParagraphs;

	protected int oldParagraphsLength;
	protected int newParagraphsLength;
	
	protected int matchingLength;
	protected double matchingRate;
	
	protected boolean isMovement;
	
	protected ParagraphEdit() {
		super();
	}
	
	private ParagraphEdit(Paragraph op, Paragraph np) {
		this.oldParagraphs = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		this.oldParagraphs.add(op);
		this.oldParagraphsLength = this.oldParagraphs.isEmpty() ? 0 : calculateParagraphsLength(this.oldParagraphs);
		this.oldPos = this.oldParagraphs.isEmpty() ? -1 : this.oldParagraphs.first().getPosition();
		
		this.newParagraphs = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		this.newParagraphs.add(np);
		this.newParagraphsLength = this.newParagraphs.isEmpty() ? 0 : calculateParagraphsLength(this.newParagraphs);
		this.newPos = this.newParagraphs.isEmpty() ? -1 : this.newParagraphs.first().getPosition();
		
		this.matchingLength = this.calculateMatchingLength();
		this.matchingRate = this.matchingLength * 2.0 / (double) (this.oldParagraphsLength + this.newParagraphsLength);
		this.isMovement = false;
	}
	
	protected ParagraphEdit(ParagraphEdit paragraphEdit) {
		this.oldParagraphs = paragraphEdit.oldParagraphs;
		this.oldParagraphsLength = paragraphEdit.oldParagraphsLength;
		this.oldPos = paragraphEdit.oldPos;
		
		this.newParagraphs = paragraphEdit.newParagraphs;
		this.newParagraphsLength = paragraphEdit.newParagraphsLength;
		this.newPos = paragraphEdit.newPos;
		
		this.matchingLength = paragraphEdit.matchingLength;
		this.matchingRate = paragraphEdit.matchingRate;
		this.isMovement = false;
	}
	
	public static ParagraphEdit createParagraphEdit(Paragraph op, Paragraph np) {
		ParagraphEdit pe = null;
				
		if (op == null && np == null) {
			return null;
		}
		
		if (op == null) {
			pe = new ParagraphInsert(np);
		}
		else if (np == null) {
			pe = new ParagraphDelete(op);
		}
		else {
			pe = new ParagraphEdit(op, np);
		}
		
		return pe;
	}
	
	protected int calculateParagraphsLength(TreeSet<Paragraph> paragraphsSet) {
		int len = 0;
		
		for (Paragraph p : paragraphsSet) {
			len += p.getLength();
		}
		
		return len;
	}

	protected int calculateMatchingLength() {
		Set<Sentence> oldSentences = new HashSet<Sentence>();
		Set<Sentence> newSentences = new HashSet<Sentence>();
		
		Set<TokenEdit> oldTokenEdit = new HashSet<TokenEdit>();
		Set<TokenEdit> newTokenEdit = new HashSet<TokenEdit>();
		
		int matchingLength = 0;
		
		for (Paragraph p : this.oldParagraphs) {
			if (p.getSentenceEdits() == null) {
				matchingLength += p.getLength();
			}
			else {
				oldSentences.addAll(p.splitIntoSentences());
			}
		}
		
		for (Paragraph p : this.newParagraphs) {
			if (p.getSentenceEdits() == null) {
				matchingLength += p.getLength();
			}
			else {
				newSentences.addAll(p.splitIntoSentences());
			}
		}
		
		for (Sentence s : oldSentences) {
			List<TokenEdit> teList = s.getTokenEdits();
			
			if (teList == null) {
				if (newSentences.contains(s)) {
					matchingLength += s.getFullLength();
				}
			}
			else {
				boolean hasMatch = false;
				for (TokenEdit te : teList) {
					if (te instanceof TokenMatch) {
						oldTokenEdit.add(te);
						hasMatch = true;
					}
				}
				if (hasMatch) {
					matchingLength += s.getTail().length();
				}
			}
		}
		
		for (Sentence s : newSentences) {
			List<TokenEdit> teList = s.getTokenEdits();
			
			if (teList == null) {
				if (oldSentences.contains(s)) {
					matchingLength += s.getFullLength();
				}
			}
			else {
				boolean hasMatch = false;
				for (TokenEdit te : teList) {
					if (te instanceof TokenMatch) {
						newTokenEdit.add(te);
						hasMatch = true;
					}
				}
				if (hasMatch) {
					matchingLength += s.getTail().length();
				}
			}
		}
		
		oldTokenEdit.retainAll(newTokenEdit);
		
		for (TokenEdit te : oldTokenEdit) {
			matchingLength += te.getFullLength() ;
		}
		
		return (matchingLength / 2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((newParagraphs == null) ? 0 : newParagraphs.hashCode());
		result = prime * result
				+ ((oldParagraphs == null) ? 0 : oldParagraphs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ParagraphEdit)) {
			return false;
		}
		ParagraphEdit other = (ParagraphEdit) obj;
		if (newParagraphs == null) {
			if (other.newParagraphs != null) {
				return false;
			}
		} else if (!newParagraphs.equals(other.newParagraphs)) {
			return false;
		}
		if (oldParagraphs == null) {
			if (other.oldParagraphs != null) {
				return false;
			}
		} else if (!oldParagraphs.equals(other.oldParagraphs)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("ParagraphEdit [matchingRate=").append(String.format("%.4f", matchingRate)).append(
						", oldPos=").append(oldPos).append(", ");
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

	public boolean contains(ParagraphEdit pe) {
		if (!this.oldParagraphs.containsAll(pe.oldParagraphs)) {
			return false;
		}
		else if (!this.newParagraphs.containsAll(pe.newParagraphs)) {
			return false;
		}
		return true;
	}

	public boolean containsOldParagraphs(ParagraphEdit pe) {
		if (!this.oldParagraphs.containsAll(pe.oldParagraphs)) {
			return false;
		}
		return true;
	}

	public boolean containsNewParagraphs(ParagraphEdit pe) {
		if (!this.newParagraphs.containsAll(pe.newParagraphs)) {
			return false;
		}
		return true;
	}
	
	public ParagraphEdit mergeWith(ParagraphEdit pe) {
		this.oldParagraphs.addAll(pe.oldParagraphs);
		this.oldParagraphsLength = calculateParagraphsLength(this.oldParagraphs);
		this.oldPos = this.oldParagraphs.first().getPosition();
		
		this.newParagraphs.addAll(pe.newParagraphs);
		this.newParagraphsLength = calculateParagraphsLength(this.newParagraphs);
		this.newPos = this.newParagraphs.first().getPosition();
		
		this.matchingLength = this.calculateMatchingLength();
		this.matchingRate = this.matchingLength * 2.0 / (double) (this.oldParagraphsLength + this.newParagraphsLength);
		
		this.isMovement = false;
		
		return this;
	}

	public int getMatchingLength() {
		return matchingLength;
	}
	
	public double getMatchingRate() {
		return matchingRate;
	}
	
	public TreeSet<Paragraph> getOldParagraphs() {
		return oldParagraphs;
	}

	public TreeSet<Paragraph> getNewParagraphs() {
		return newParagraphs;
	}
	
	public int getOldPos() {
		return oldPos;
	}

	public int getNewPos() {
		return newPos;
	}

	public void labelAsMoved() {
		this.isMovement = true;
	}
	
	public boolean isMovement() {
		return this.isMovement;
	}
	
	public List<SentenceEdit> getSentenceEdits() {
		HashSet<SentenceEdit> oldSeSet = new HashSet<SentenceEdit>();
		List<SentenceEdit> retList = new LinkedList<SentenceEdit>();
		
		if (this.oldParagraphs != null) {
			for (Paragraph op : this.oldParagraphs) {
				List<SentenceEdit> seList = op.getSentenceEdits();
				if (seList == null) {
					continue;
				}
				
				for (SentenceEdit se : seList) {
					if (se instanceof SentenceDelete) {
						retList.add(se);
					}
					else {
						oldSeSet.add(se);
					}
				}
			}
		}
		
		if (this.newParagraphs != null) {
			for (Paragraph np : this.newParagraphs) {
				List<SentenceEdit> seList = np.getSentenceEdits();
				if (seList == null) {
					continue;
				}
				
				for (SentenceEdit se : seList) {
					if (se instanceof SentenceInsert) {
						retList.add(se);
					}
					else if (oldSeSet.contains(se)) {
						retList.add(se);
						oldSeSet.remove(se);
					}
				}
			}
		}
		
		return retList;
	}
}
