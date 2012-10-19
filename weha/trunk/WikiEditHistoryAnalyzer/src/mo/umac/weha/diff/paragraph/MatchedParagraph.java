package mo.umac.weha.diff.paragraph;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.data.Sentence;
import mo.umac.weha.diff.MatchedUnit;

public class MatchedParagraph extends Sentence implements MatchedUnit {

	private int matchId;

	public MatchedParagraph(Paragraph para, int matchId) {
		super(0, para.getContent() + para.getTail(), para);
		this.matchId = matchId;
	}

	public int getMatchId() {
		return this.matchId;
	}

}
