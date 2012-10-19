package mo.umac.weha.diff.token;

import java.util.ArrayList;
import java.util.List;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;

public class TokenInsert extends TokenEdit {

	public TokenInsert(List<Token> insTokens) {
		this.newTokens = insTokens;
	}

	@Override
	public String toString() {
		return "TokenInsert [newPos=" + newTokens.get(0).getPosition() + ", newToken="
				+ newTokens + "]\n";
	}

	@Override
	public List<TokenEdit> splitBySentence() {
		int lastIndex = 0;
		Sentence newSent = null;
		List<TokenEdit> tokenEdits = new ArrayList<TokenEdit>();
		
		for (int i = 0; i < newTokens.size(); i++) {
			Token nt = newTokens.get(i);
			
			if (newSent != nt.getSentence()) {
				if (newSent != null) {
					tokenEdits.add(new TokenInsert(newTokens.subList(lastIndex, i)));
				}
				newSent = nt.getSentence();
				lastIndex = i;
			}
		}
		tokenEdits.add(new TokenInsert(newTokens.subList(lastIndex, newTokens.size())));
		
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
		// NOP
		return null;
	}
	
	@Override
	public TokenEdit[] splitNewTokens(int startPos, int endPos) {
		TokenEdit[] retList = new TokenEdit[3];
		
		List<Token> before = this.newTokens.subList(0, startPos);
		List<Token> middle = this.newTokens.subList(startPos, endPos);
		List<Token> remainder = this.newTokens.subList(endPos, this.newTokens.size());
		
		if (!before.isEmpty()) {
			retList[0] = new TokenInsert(before);
		}
		
		if (!middle.isEmpty()) {
			retList[1] = new TokenInsert(middle);
		}
		
		if (!remainder.isEmpty()) {
			retList[2] = new TokenInsert(remainder);
		}
		
		return retList;
	}
	
}
