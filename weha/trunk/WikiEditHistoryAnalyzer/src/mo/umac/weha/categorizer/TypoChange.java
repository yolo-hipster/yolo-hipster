package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import mo.umac.weha.data.Token;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.token.TokenReplace;

import org.apache.commons.lang3.StringUtils;

public class TypoChange extends AbstractEditAction {

	{
		weight = 0.5;
	}
	
	public TypoChange() {
		this.basicEdits = null;
	}
	
	public TypoChange(AbstractEdit b) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = b;
	}
	
	public int findAction(AbstractEdit edit) {
		
		if (!(edit instanceof TokenReplace)) {
			return -1;
		}
		else {
			TokenReplace replEdit = (TokenReplace) edit;
			List<Token> insContent = replEdit.getOldTokens();
			List<Token> delContent = replEdit.getNewTokens();
						
			StringBuffer insString = new StringBuffer();
			StringBuffer delString = new StringBuffer();
			
			for (int i = 0; i < insContent.size(); i++) {
				insString.append(insContent.get(i).getContent());
			}
				
			for (int i = 0; i < delContent.size(); i++) {
				delString.append(delContent.get(i).getContent());
			}
			
			if (insString.length() > 100 ||
				delString.length() > 100 ||
				insString.length() * 10 < delString.length() ||
				delString.length() * 10 < insString.length()) {
					return -1;
			}
			
			int dist = StringUtils.getLevenshteinDistance(insString.toString(), delString.toString());
			if (dist < Math.max((insString.length() + delString.length()) * 2 / 10, 2)) {
				return 0;
			}
		}
		
		return -1;
	}

	@Override
	public List<AbstractEditAction> classify(List<AbstractEdit> editList) {
		ArrayList<AbstractEditAction> classifiedActionList = new ArrayList<AbstractEditAction>();
		
		for (ListIterator<AbstractEdit> iter = editList.listIterator(); iter.hasNext(); ) {
			AbstractEdit edit = iter.next();
			if (this.findAction(edit) >= 0) {
				classifiedActionList.add(new TypoChange(edit));
				iter.remove();
			}
		}
		
		return classifiedActionList;
	}
	
	public int lengthCount() {
		return 1;
	}
}
