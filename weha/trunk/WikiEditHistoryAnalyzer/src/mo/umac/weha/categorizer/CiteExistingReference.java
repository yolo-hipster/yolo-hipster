package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import mo.umac.weha.data.Token;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.diff.token.TokenInsert;
import mo.umac.weha.diff.token.TokenReplace;

public class CiteExistingReference extends AbstractEditAction {
	
	{
		weight = 2.0;
	}
	
	public CiteExistingReference() {
		this.basicEdits = null;
	}
	
	public CiteExistingReference(AbstractEdit b) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = b;
	}
	
	public int findAction(AbstractEdit edit) {
		
		if (!(edit instanceof TokenInsert) && 
			!(edit instanceof TokenReplace)) {
			return -1;
		}
		else {
			TokenEdit tokEdit = (TokenEdit) edit;
			List<Token> content = tokEdit.getNewTokens();
			
			for (int i = 0; i < content.size(); i++) {
				String contentString = content.get(i).getContent().toLowerCase();
				if (contentString.startsWith("<ref") && contentString.endsWith("/>")) {
					return i;
				}
			}
		}
		
		return -1;
	}

	@Override
	public List<AbstractEditAction> classify(List<AbstractEdit> editList) {
		ArrayList<AbstractEditAction> classifiedActionList = new ArrayList<AbstractEditAction>();
		
		for (ListIterator<AbstractEdit> iter = editList.listIterator(); iter.hasNext(); ) {
			AbstractEdit ae = iter.next();
			int refStartPos = this.findAction(ae);
			
			if (refStartPos >= 0) {
				TokenEdit tokEdit = (TokenEdit) ae;
				int refEndPos = refStartPos + 1;
				
				TokenEdit[] splittedEdit = tokEdit.splitNewTokens(refStartPos, refEndPos);
				classifiedActionList.add(new CiteExistingReference(splittedEdit[1]));
				
				iter.remove();
				
				if (splittedEdit[0] != null) {
					iter.add(splittedEdit[0]);
				}
					
				if (splittedEdit[2] != null) {
					iter.add(splittedEdit[2]);
					iter.previous();
				}
			}
		}
		
		return classifiedActionList;
	}
	
	public int lengthCount() {
		return 1;
	}
}

