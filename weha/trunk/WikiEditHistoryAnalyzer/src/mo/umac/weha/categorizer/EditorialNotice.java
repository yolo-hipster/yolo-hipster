package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import mo.umac.weha.data.Token;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.diff.token.TokenInsert;
import mo.umac.weha.diff.token.TokenReplace;

public class EditorialNotice extends AbstractEditAction {

	private static Set<String> editorialTemplates;
	
	static {
		editorialTemplates = new HashSet<String>();
		
		editorialTemplates.add("fact");
		editorialTemplates.add("unreferenced");
		editorialTemplates.add("stub");
	}
	
	{
		weight = 0.5;
	}
	
	public EditorialNotice() {
		this.basicEdits = null;
	}
	
	public EditorialNotice(AbstractEdit b) {
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
			
			for (int i = 0; i < content.size() - 1; i++) {
				if (content.get(i).getContent().equals("{{") && 
					editorialTemplates.contains(content.get(i+1).getContent().toLowerCase())) {
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
			int iwStartPos = this.findAction(ae);
			
			if (iwStartPos >= 0) {
				TokenEdit tokEdit = (TokenEdit) ae;
				List<Token> content = tokEdit.getNewTokens();
				int iwEndPos = -1;
			
				for (int i = iwStartPos; i < content.size(); i++) {
					if (content.get(i).getContent().equals("}}")) {
						iwEndPos = i;
						break;
					}
				}
				
				if (iwEndPos > iwStartPos) {
					TokenEdit[] splittedEdit = tokEdit.splitNewTokens(iwStartPos, iwEndPos + 1);
					classifiedActionList.add(new EditorialNotice(splittedEdit[1]));
					
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
		}
		
		return classifiedActionList;
	}
	
	public int lengthCount() {
		return 1;
	}
}

