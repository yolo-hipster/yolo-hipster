package mo.umac.weha.diff.sentence;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;
import mo.umac.weha.diff.MatchedUnit;

public class MatchedSentence extends Token implements MatchedUnit {

	private int matchId;

	public MatchedSentence(Sentence sentence, int matchId) {
		super(0, sentence.getContent(), sentence.getTail(), sentence);
		this.matchId = matchId;
	}

	public int getMatchId() {
		return matchId;
	}

}
