package mo.umac.weha.diff.engine;

public class MatchInfo {
	private int[] matchedOld;
	private int[] matchedNew;
	private Match[] matchesArray;
	
	public MatchInfo(int[] matchedOld, int[] matchedNew, Match[] matchesArray) {
		this.matchedOld = matchedOld;
		this.matchedNew = matchedNew;
		this.matchesArray = matchesArray;
	}

	public int[] getMatchedOld() {
		return matchedOld;
	}

	public int[] getMatchedNew() {
		return matchedNew;
	}

	public Match[] getMatchesArray() {
		return matchesArray;
	}

}
