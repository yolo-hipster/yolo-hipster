package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.sentence.SentenceInsert;
import mo.umac.weha.diff.token.TokenInsert;

public class ContentAddition extends AbstractEditAction {
	
	private int lenCount = 0;
	
	{
		weight = 1.0;
	}
	
	public ContentAddition() {
		this.basicEdits = null;
	}
	
	public ContentAddition(AbstractEdit b) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = b;
		this.lenCount = 0;
	}
	
	public int findAction(AbstractEdit edit) {
		if (!(edit instanceof SentenceInsert) &&
			!(edit instanceof TokenInsert)) {
			return -1;
		}
		else if (edit instanceof SentenceInsert) {
			SentenceInsert insEdit = (SentenceInsert) edit;
			TreeSet<Sentence> sentences = insEdit.getNewSentences();
			
			for (Sentence sent : sentences) {
				List<Token> content = sent.splitIntoTokens();
				
				for (int i = 0; i < content.size(); i++) {
					String text = content.get(i).getContent();
					if (StringUtils.isAlphanumeric(text) || 
						text.matches("(\\p{Alpha}\\.){2,}")) {
						 return 0;
					}
				}
			}
		}
		else if (edit instanceof TokenInsert) {
			TokenInsert insEdit = (TokenInsert) edit;
			List<Token> content = insEdit.getNewTokens();
			
			for (int i = 0; i < content.size(); i++) {
				String text = content.get(i).getContent();
				if (StringUtils.isAlphanumeric(text) || 
					text.matches("(\\p{Alpha}\\.){2,}")) {
					 return 0;
				}
			}
		}
		
		return -1;
	}

	@Override
	public List<AbstractEditAction> classify(List<AbstractEdit> editList) {
		ArrayList<AbstractEditAction> classifiedActionList = new ArrayList<AbstractEditAction>();
		
		for (ListIterator<AbstractEdit> iter = editList.listIterator(); iter.hasNext(); )
		{
			AbstractEdit b = iter.next();
			if (findAction(b) >= 0) {
				classifiedActionList.add(new ContentAddition(b));
				iter.remove();
			}
		}
		
		return classifiedActionList;
	}
	
	public int lengthCount() {
		if (lenCount > 0) {
			return lenCount;
		}
		
		lenCount = 0;
		
		for (AbstractEdit edit : this.basicEdits) {
			List<Token> content = null;
			if (edit instanceof SentenceInsert) {
				SentenceInsert insEdit = (SentenceInsert) edit;
				content = insEdit.getNewSentences().first().splitIntoTokens();
			}
			else if (edit instanceof TokenInsert) {
				TokenInsert insEdit = (TokenInsert) edit;
				content = insEdit.getNewTokens();
			}
			
			for (int i = 0; i < content.size(); i++) {
				String text = content.get(i).getContent();
				if (StringUtils.isAlphanumeric(text) || 
					text.matches("(\\p{Alpha}\\.){2,}")) {
					lenCount++;
				}
			}
		}
		
		return lenCount;
	}
}
