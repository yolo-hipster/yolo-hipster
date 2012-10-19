package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeSet;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.sentence.SentenceDelete;
import mo.umac.weha.diff.token.TokenDelete;

import org.apache.commons.lang3.StringUtils;

public class ContentRemoval extends AbstractEditAction {
	
	private int lenCount = 0;
	
	{
		weight = 0.5;
	}
	
	public ContentRemoval() {
		this.basicEdits = null;
	}
	
	public ContentRemoval(AbstractEdit b) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = b;
		this.lenCount = 0;
	}
	
	public int findAction(AbstractEdit edit) {
		if (!(edit instanceof SentenceDelete) &&
			!(edit instanceof TokenDelete)) {
			return -1;
		}
		else if (edit instanceof SentenceDelete) {
			SentenceDelete delEdit = (SentenceDelete) edit;
			TreeSet<Sentence> sentences = delEdit.getOldSentences();
			
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
		else if (edit instanceof TokenDelete) {
			TokenDelete insEdit = (TokenDelete) edit;
			List<Token> content = insEdit.getOldTokens();
			
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
				classifiedActionList.add(new ContentRemoval(b));
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
			if (edit instanceof SentenceDelete) {
				SentenceDelete delEdit = (SentenceDelete) edit;
				content = delEdit.getOldSentences().first().splitIntoTokens();
			}
			else if (edit instanceof TokenDelete) {
				TokenDelete delEdit = (TokenDelete) edit;
				content = delEdit.getOldTokens();
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
