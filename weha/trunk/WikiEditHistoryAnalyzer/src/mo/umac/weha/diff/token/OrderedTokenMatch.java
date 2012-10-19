package mo.umac.weha.diff.token;

import java.util.List;

import mo.umac.weha.data.Token;

public class OrderedTokenMatch {

	private TokenMatch tokenMatch;
	private int oldMatchOrder;
	private int newMatchOrder;
	
	public OrderedTokenMatch(TokenMatch tm) {
		this.tokenMatch = tm;
		this.oldMatchOrder = 0;
		this.newMatchOrder = 0;
	}

	public int getOldIndex() {
		return tokenMatch.getOldIndex();
	}

	public int getNewIndex() {
		return tokenMatch.getNewIndex();
	}
	
	public List<Token> getOldTokens() {
		return tokenMatch.getOldTokens();
	}
	
	public List<Token> getNewTokens() {
		return tokenMatch.getNewTokens();
	}
	
	public int getLength() {
		return tokenMatch.getLength();
	}
	
	public int getFullLength() {
		return tokenMatch.getFullLength();
	}
	
	public int getOldMatchOrder() {
		return oldMatchOrder;
	}

	public void setOldMatchOrder(int oldMatchOrder) {
		this.oldMatchOrder = oldMatchOrder;
	}

	public int getNewMatchOrder() {
		return newMatchOrder;
	}

	public void setNewMatchOrder(int newMatchOrder) {
		this.newMatchOrder = newMatchOrder;
	}

	public TokenMatch getTokenMatch() {
		return tokenMatch;
	}
	
}
