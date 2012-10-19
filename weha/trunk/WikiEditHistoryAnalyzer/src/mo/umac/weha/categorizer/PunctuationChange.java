package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import mo.umac.weha.data.Token;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.token.TokenDelete;
import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.diff.token.TokenInsert;
import mo.umac.weha.diff.token.TokenReplace;

public class PunctuationChange extends AbstractEditAction {

	{
		weight = 0.5;
	}
	
	public PunctuationChange() {
		this.basicEdits = null;
	}
	
	public PunctuationChange(AbstractEdit b) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = b;
	}
	
	@Override
	public int findAction(AbstractEdit edit) {
		
		if (edit instanceof TokenReplace || 
			edit instanceof TokenInsert ||
			edit instanceof TokenDelete) {
			TokenEdit tokEdit = (TokenEdit) edit;
			List<Token> delContent = tokEdit.getOldTokens();
			List<Token> insContent = tokEdit.getNewTokens();
			
			if (insContent != null) {
				for (int i = 0; i < insContent.size(); i++) {
					if (! insContent.get(i).getContent().matches("[\\p{IsP}\\p{Punct}]")) {
						return -1;
					}
				}
			}
			
			if (delContent != null) {
				for (int i = 0; i < delContent.size(); i++) {
					if (! delContent.get(i).getContent().matches("[\\p{IsP}\\p{Punct}]")) {
						return -1;
					}
				}
			}
		}
		else {
			return -1;
		}
		
		return 0;
	}
	
	@Override
	public List<AbstractEditAction> classify(List<AbstractEdit> editList) {
		ArrayList<AbstractEditAction> classifiedActionList = new ArrayList<AbstractEditAction>();
		
		for (ListIterator<AbstractEdit> iter = editList.listIterator(); iter.hasNext(); ) {
			AbstractEdit edit = iter.next();
			if (this.findAction(edit) >= 0) {
				classifiedActionList.add(new PunctuationChange(edit));
				iter.remove();
			}
		}
		
		return classifiedActionList;
	}
	
	public int lengthCount() {
		return 1;
	}
}
