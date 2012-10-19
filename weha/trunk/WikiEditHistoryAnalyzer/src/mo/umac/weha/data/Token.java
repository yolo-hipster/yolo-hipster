package mo.umac.weha.data;

public class Token {

	public enum TokenKind {
		WORD, PUNCTUATION, MARKUP
	}
	
	/**
	 *  Position is relative to the beginning of the sentence.
	 */
	private int position;		
	private int arrayIndex;
	
	private String content;
	private String tail;
	
	private Sentence sentence;
	
	private TokenKind kind;

	public Token(int pos, String content, String tail, Sentence sent) {
		this.position	= pos;
		this.content	= content;
		this.tail		= tail;
		this.sentence	= sent;
	}

	public void setArrayIndex(int arrayIndex) {
		this.arrayIndex = arrayIndex;
	}

	public int getArrayIndex() {
		return arrayIndex;
	}

	public int getPosition() {
		return this.position + this.sentence.getPosition();
	}
	
	public int getLength() {
		return this.content.length();
	}

	public int getFullLength() {
		return this.content.length() + this.tail.length();
	}
	
	public Sentence getSentence() {
		return this.sentence;
	}

	@Override
	public String toString() {
		return content + tail;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
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
		Token other = (Token) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		return true;
	}

	public String getContent() {
		return this.content;
	}
	
}
