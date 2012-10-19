package mo.umac.weha.data;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import mo.umac.weha.diff.sentence.SentenceEdit;
import mo.umac.weha.lexer.SentenceSplitter;

public class Paragraph {
	
	private int position;
	
	private String content;
	private String tail;
	
	private List<Sentence> sentences;
	
	private LinkedList<SentenceEdit> sentenceEdits;
	
	public Paragraph(int pos, String content, String tail) {
		this.position	= pos;
		this.content	= content;
		this.tail		= tail;
	}

	public int getPosition() {
		return this.position;
	}
	
	public String getContent() {
		return this.content;
	}

	public String getTail() {
		return tail;
	}
	
	public LinkedList<SentenceEdit> getSentenceEdits() {
		return sentenceEdits;
	}

	public int getLength() {
		return content.length();
	}
	
	public List<Sentence> splitIntoSentences() {
		if (this.sentences == null) {
			this.sentences = SentenceSplitter.separateSentence(this);
		}
		
		return this.sentences;
	}
	
	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		
		builder.append("Paragraph [position=").append(position).append(", ");
		if (content != null)
			builder.append("content=\"").append(content).append("\", ");
		if (tail != null)
			builder.append("tail=\"").append(tail).append("\", ");
		if (sentences != null)
			builder.append("sentences=").append(
					sentences.subList(0, Math.min(sentences.size(), maxLen)))
					.append(", ");
		if (sentenceEdits != null)
			builder.append("sentenceEdits=").append(
					sentenceEdits.subList(0, Math.min(sentenceEdits.size(),
							maxLen)));
		builder.append("]\n");
		
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Paragraph other = (Paragraph) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		return true;
	}

	public void addOldSentenceEdit(SentenceEdit editToAdd) {
		if (this.sentenceEdits == null) {
			this.sentenceEdits = new LinkedList<SentenceEdit>();
			this.sentenceEdits.add(editToAdd);
			return;
		}
		
		ListIterator<SentenceEdit> iter = sentenceEdits.listIterator();
		while (iter.hasNext()) {
			SentenceEdit e = iter.next();
			if (editToAdd.getOldPos() < e.getOldPos()) {
				iter.previous();
				iter.add(editToAdd);
				return;
			}
			else if (editToAdd.getOldPos() == e.getOldPos()) {
				if (editToAdd.equals(e)) {
					return;
				}
				else if (editToAdd.getMatchingRate() > e.getMatchingRate()) {
					iter.previous();
					iter.add(editToAdd);
					return;
				}
				else {
					iter.add(editToAdd);
					return;
				}
			}
		}
		
		this.sentenceEdits.addLast(editToAdd);
	}
	
	public void addNewSentenceEdit(SentenceEdit editToAdd) {
		if (this.sentenceEdits == null) {
			this.sentenceEdits = new LinkedList<SentenceEdit>();
			this.sentenceEdits.add(editToAdd);
			return;
		}
		
		ListIterator<SentenceEdit> iter = sentenceEdits.listIterator();
		while (iter.hasNext()) {
			SentenceEdit e = iter.next();
			if (editToAdd.getNewPos() < e.getNewPos()) {
				iter.previous();
				iter.add(editToAdd);
				return;
			}
			else if (editToAdd.getNewPos() == e.getNewPos()) {
				if (editToAdd.equals(e)) {
					return;
				}
				else if (editToAdd.getMatchingRate() > e.getMatchingRate()) {
					iter.previous();
					iter.add(editToAdd);
					return;
				}
				else {
					iter.add(editToAdd);
					return;
				}
			}
				
		}
		
		this.sentenceEdits.addLast(editToAdd);
	}
	
}
