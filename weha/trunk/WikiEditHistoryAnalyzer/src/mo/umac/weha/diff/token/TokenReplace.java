package mo.umac.weha.diff.token;

import java.util.ArrayList;
import java.util.List;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;

public class TokenReplace extends TokenEdit {

	public TokenReplace(List<Token> delContent, List<Token> insContent) {
		this.oldTokens = delContent;
		this.newTokens = insContent;
	}

	@Override
	public String toString() {
		return "TokenReplace [newPos=" + newTokens.get(0).getPosition() 
			+ ", newTokens=" + newTokens.size() + newTokens  
			+ ", oldPos=" + oldTokens.get(0).getPosition() 
			+ ", oldTokens=" + oldTokens.size() + oldTokens + "]\n";
	}

	@Override
	public List<TokenEdit> splitBySentence() {
		int lastOldIndex = 0;
		int lastNewIndex = 0;
		Sentence oldSent = null;
		Sentence newSent = null;
		List<TokenEdit> tokenEdits = new ArrayList<TokenEdit>();
		
		for (int i = 0; i <= oldTokens.size(); i++) {
			Token ot = oldTokens.get(Math.min(i, oldTokens.size() - 1));
			
			if (oldSent == null) {
				oldSent = ot.getSentence();
				lastOldIndex = i;
			}
			
			if (oldSent != ot.getSentence() || i == oldTokens.size()) {
				List<Token> otList = oldTokens.subList(lastOldIndex, i);
				
				for (int j = lastNewIndex; j <= newTokens.size(); j++) {
					Token nt = newTokens.get(Math.min(j, newTokens.size() - 1));
					
					if (newSent == null) {
						newSent = nt.getSentence();
						lastNewIndex = j;
					}
					
					if (newSent != nt.getSentence() || j == newTokens.size()) {
						List<Token> ntList = newTokens.subList(lastNewIndex, j);
						if (otList.size() > 0 && ntList.size() > 0) {
							tokenEdits.add(new TokenReplace(otList, ntList));
						}
						else if (otList.size() > 0) {
							tokenEdits.add(new TokenDelete(otList));
						}
						else if (ntList.size() > 0) {
							tokenEdits.add(new TokenInsert(ntList));
						}
						newSent = nt.getSentence();
						lastNewIndex = j;
						break;
					}
				}
				oldSent = ot.getSentence();
				lastOldIndex = i;
			}
		}
		
		for (int j = lastNewIndex; j <= newTokens.size(); j++) {
			Token nt = newTokens.get(Math.min(j, newTokens.size() - 1));
			
			if (newSent == null) {
				newSent = nt.getSentence();
				lastNewIndex = j;
			}
			
			if (newSent != nt.getSentence() || j == newTokens.size()) {
				List<Token> ntList = newTokens.subList(lastNewIndex, j);
				if (ntList.size() > 0) {
					tokenEdits.add(new TokenInsert(ntList));
				}
				newSent = nt.getSentence();
				lastNewIndex = j;
			}
		}
		
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
			retList[2] = new TokenReplace(remainder, this.newTokens);
		}
		else {
			retList[2] = new TokenInsert(this.newTokens);
		}
		
		return retList;
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
			retList[2] = new TokenReplace(this.oldTokens, remainder);
		}
		else {
			retList[2] = new TokenDelete(this.oldTokens);
		}
		
		return retList;
	}
	
}
