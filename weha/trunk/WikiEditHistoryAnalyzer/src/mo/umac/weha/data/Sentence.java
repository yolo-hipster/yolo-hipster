package mo.umac.weha.data;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.lexer.TokenSplitter;

public class Sentence {

	/**
	 * Position is relative to the beginning of the paragraph.
	 */
	private int position;
	private int arrayIndex;
	
	private String content;
	private String tail;
	
	private Paragraph paragraph;
	private List<Token> tokens;
	
	private LinkedList<TokenEdit> tokenEdits;
	
	public Sentence(int pos, String content, Paragraph para) {
		this.position	= pos;
		
		int wsPos = content.length();
		char[] contentArray = content.toCharArray();
		for (int i = contentArray.length - 1; i >= 0; i--) {
			if (!Character.isWhitespace(contentArray[i])) {
				wsPos = i + 1;
				break;
			}
		}
		
		this.content	= content.substring(0, wsPos);
		this.tail		= content.substring(wsPos);
		
		this.paragraph	= para;
	}

	public void setArrayIndex(int arrayIndex) {
		this.arrayIndex = arrayIndex;
	}

	public int getArrayIndex() {
		return arrayIndex;
	}

	public int getPosition() {
		return this.position + this.paragraph.getPosition();
	}

	public String getContent() {
		return this.content;
	}

	public String getTail() {
		return this.tail;
	}
	
	public int getLength() {
		return this.content.length();
	}

	public int getFullLength() {
		return this.content.length() + this.tail.length();
	}
	
	public Paragraph getParagraph() {
		return paragraph;
	}

	public List<Token> splitIntoTokens() {
		if (this.tokens == null) {
			this.tokens = TokenSplitter.separateToken(this);
		}
		
		return this.tokens;
	}

	public List<TokenEdit> getTokenEdits() {
		return this.tokenEdits;
	}
	
	public void addOldTokenEdit(TokenEdit editToAdd) {
		if (this.tokenEdits == null) {
			this.tokenEdits = new LinkedList<TokenEdit>();
			this.tokenEdits.add(editToAdd);
			return;
		}
		
		ListIterator<TokenEdit> iter = tokenEdits.listIterator();
		while (iter.hasNext()) {
			TokenEdit e = iter.next();
			if (editToAdd.getOldPos() < e.getOldPos()) {
				iter.previous();
				iter.add(editToAdd);
				return;
			}
		}
		
		this.tokenEdits.addLast(editToAdd);
	}
	
	public void addNewTokenEdit(TokenEdit editToAdd) {
		if (this.tokenEdits == null) {
			this.tokenEdits = new LinkedList<TokenEdit>();
			this.tokenEdits.add(editToAdd);
			return;
		}
		
		ListIterator<TokenEdit> iter = tokenEdits.listIterator();
		while (iter.hasNext()) {
			TokenEdit e = iter.next();
			if (editToAdd.getNewPos() < e.getNewPos()) {
				iter.previous();
				iter.add(editToAdd);
				return;
			}
		}
		
		this.tokenEdits.addLast(editToAdd);
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		
		builder.append("Sentence [position=").append(this.getPosition()).append(", ");
		if (content != null)
			builder.append("content=\"").append(content).append("\", ");
		if (tail != null)
			builder.append("tail=\"").append(tail).append("\", ");
		if (tokens != null)
			builder.append("tokens=\n").append(
					tokens.subList(0, Math.min(tokens.size(), maxLen))).append(
					", ");
		if (paragraph != null)
			builder.append("paragraph=").append(paragraph.getPosition()).append(", ");
		if (tokenEdits != null)
			builder.append("tokenEdits=\n").append(
					tokenEdits.subList(0, Math.min(tokenEdits.size(), maxLen)));
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
		Sentence other = (Sentence) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		return true;
	}
	
}
