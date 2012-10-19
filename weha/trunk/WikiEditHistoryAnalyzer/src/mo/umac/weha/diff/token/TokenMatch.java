package mo.umac.weha.diff.token;

import java.util.ArrayList;
import java.util.List;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;

public class TokenMatch extends TokenEdit {
	protected int length;
	
	private boolean isMovement;

	public TokenMatch(int length, List<Token> oldTokens, List<Token> newTokens) {
		this.length = length;
		this.oldTokens = oldTokens;
		this.newTokens = newTokens;
		this.isMovement = false;
	}

	public TokenMatch(int length, List<Token> oldTokens, List<Token> newTokens, boolean isMov) {
		this.length = length;
		this.oldTokens = oldTokens;
		this.newTokens = newTokens;
		this.isMovement = isMov;
	}

	public TokenMatch(TokenMatch obj) {
		this.length = obj.length;
		this.oldTokens = obj.oldTokens;
		this.newTokens = obj.newTokens;
		this.isMovement = obj.isMovement;
	}

	public int getLength() {
		return length;
	}
	
	@Override
	public String toString() {
		return (this.isMovement ? "TokenMove" : "TokenMatch") + " [length=" + length 
			+ ", oldPos=" + oldTokens.get(0).getPosition()
			+ ", newPos=" + newTokens.get(0).getPosition() + "]\n";
	}

	@Override
	public List<TokenEdit> splitBySentence() {
		int lastIndex = 0;
		Sentence oldSent = null;
		Sentence newSent = null;
		List<TokenEdit> tokenEdits = new ArrayList<TokenEdit>();
		
		for (int i = 0; i < oldTokens.size(); i++) {
			Token ot = oldTokens.get(i);
			Token nt = newTokens.get(i);
			
			if (oldSent != ot.getSentence() || newSent != nt.getSentence()) {
				if (oldSent != null && newSent != null) {
					tokenEdits.add(new TokenMatch(i - lastIndex,
							oldTokens.subList(lastIndex, i), newTokens.subList(lastIndex, i), this.isMovement));
				}
				oldSent = ot.getSentence();
				newSent = nt.getSentence();
				lastIndex = i;
			}
		}
		
		tokenEdits.add(new TokenMatch(oldTokens.size() - lastIndex, 
				oldTokens.subList(lastIndex, oldTokens.size()), newTokens.subList(lastIndex, newTokens.size()), this.isMovement));
		
		return tokenEdits;
	}

	public void labelAsMoved() {
		this.isMovement = true;
	}

	public boolean isMovement() {
		return isMovement;
	}

	@Override
	public int getMatchingLength() {
		return this.getFullLength();
	}

	@Override
	public double getMatchingRate() {
		return 1.0;
	}
	
	@Override
	public TokenEdit[] splitOldTokens(int startPos, int endPos) {
		return split(startPos, endPos);
	}
	
	@Override
	public TokenEdit[] splitNewTokens(int startPos, int endPos) {
		return split(startPos, endPos);
	}
	
	private TokenEdit[] split(int startPos, int endPos) {
		TokenEdit[] retList = new TokenEdit[3];
		
		List<Token> beforeOld = this.oldTokens.subList(0, startPos);
		List<Token> beforeNew = this.newTokens.subList(0, startPos);
		List<Token> middleOld = this.oldTokens.subList(startPos, endPos);
		List<Token> middleNew = this.newTokens.subList(startPos, endPos);
		List<Token> remainderOld = this.oldTokens.subList(endPos, this.oldTokens.size());
		List<Token> remainderNew = this.newTokens.subList(endPos, this.newTokens.size());
		
		if (!beforeOld.isEmpty()) {
			retList[0] = new TokenMatch(beforeOld.size(), beforeOld, beforeNew, this.isMovement);
		}
		
		if (!middleOld.isEmpty()) {
			retList[1] = new TokenMatch(middleOld.size(), middleOld, middleNew, this.isMovement);
		}
		
		if (!remainderOld.isEmpty()) {
			retList[2] = new TokenMatch(remainderOld.size(), remainderOld, remainderNew, this.isMovement);
		}
		
		return retList;
	}
}