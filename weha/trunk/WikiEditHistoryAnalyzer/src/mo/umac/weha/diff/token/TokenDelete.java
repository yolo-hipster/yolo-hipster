package mo.umac.weha.diff.token;

import java.util.ArrayList;
import java.util.List;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;

public class TokenDelete extends TokenEdit {
	
	public TokenDelete(List<Token> delTokens) {
		this.oldTokens = delTokens;
	}

	@Override
	public String toString() {
		return "TokenDelete [oldPos=" + oldTokens.get(0).getPosition() + ", oldToken="
				+ oldTokens + "]\n";
	}

	@Override
	public List<TokenEdit> splitBySentence() {
		int lastIndex = 0;
		Sentence oldSent = null;
		List<TokenEdit> tokenEdits = new ArrayList<TokenEdit>();
		
		for (int i = 0; i < oldTokens.size(); i++) {
			Token ot = oldTokens.get(i);
			
			if (oldSent != ot.getSentence()) {
				if (oldSent != null) {
					tokenEdits.add(new TokenDelete(oldTokens.subList(lastIndex, i)));
				}
				oldSent = ot.getSentence();
				lastIndex = i;
			}
		}
		tokenEdits.add(new TokenDelete(oldTokens.subList(lastIndex, oldTokens.size())));
		
		return tokenEdits;
	}

	@Override
	public int getMatchingLength() {
		return 0;
	}

	@Override
	public double getMatchingRate() {
		return 0;
	}

	@Override
	public void labelAsMoved() {
		// NOP
	}
	
	@Override
	public TokenEdit[] splitOldTokens(int startPos, int endPos) {
		TokenEdit[] retList = new TokenEdit[3];
		
		List<Token> before = this.oldTokens.subList(0, startPos);
		List<Token> middle = this.oldTokens.subList(startPos, endPos);
		List<Token> remainder = this.oldTokens.subList(endPos, this.oldTokens.size());
		
		if (!before.isEmpty()) {
			retList[0] = new TokenDelete(before);
		}
		
		if (!middle.isEmpty()) {
			retList[1] = new TokenDelete(middle);
		}
		
		if (!remainder.isEmpty()) {
			retList[2] = new TokenDelete(remainder);
		}
		
		return retList;
	}
	
	@Override
	public TokenEdit[] splitNewTokens(int startPos, int endPos) {
		// NOP
		return null;
	}
	
}
