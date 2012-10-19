package mo.umac.weha.diff.token;

import java.util.List;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;
import mo.umac.weha.diff.AbstractEdit;

public abstract class TokenEdit implements AbstractEdit {
	
	protected List<Token> oldTokens;
	protected List<Token> newTokens;

	public int getOldPos() {
		if (this.oldTokens == null) {
			return -1;
		}
		else {
			return this.oldTokens.get(0).getPosition();
		}
	}
	
	public int getNewPos() {
		if (this.newTokens == null) {
			return -1;
		}
		else {
			return this.newTokens.get(0).getPosition();
		}
	}

	public int getOldIndex() {
		return this.oldTokens.get(0).getArrayIndex();
	}
	
	public int getNewIndex() {
		return this.newTokens.get(0).getArrayIndex();
	}
	
	public List<Token> getOldTokens() {
		return this.oldTokens;
	}
	
	public List<Token> getNewTokens() {
		return this.newTokens;
	}
	
	public Sentence getFirstOldSentence() {
		if (this.oldTokens == null) {
			return null;
		}
		
		return this.oldTokens.get(0).getSentence();
	}
	
	public Sentence getFirstNewSentence() {
		if (this.newTokens == null) {
			return null;
		}
		
		return this.newTokens.get(0).getSentence();
	}
	
	public abstract List<TokenEdit> splitBySentence();
	
	public int getTokenLength() {
		int length = 0;
		
		if (oldTokens != null) {
			for (Token t : oldTokens) {
				length += t.getLength();
			}
		}
		
		if (newTokens != null) {
			for (Token t : newTokens) {
				length += t.getLength();
			}
		}
		
		return length;
	}
	
	public int getFullLength() {
		int length = 0;
		
		if (oldTokens != null) {
			for (Token t : oldTokens) {
				length += t.getFullLength();
			}
		}
		
		if (newTokens != null) {
			for (Token t : newTokens) {
				length += t.getFullLength();
			}
		}
		
		return length;
	}

	public boolean isMovement() {
		return false;
	}
	
	public abstract TokenEdit[] splitOldTokens(int startPos, int endPos);
	public abstract TokenEdit[] splitNewTokens(int startPos, int endPos);
	
}
