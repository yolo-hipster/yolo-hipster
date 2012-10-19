package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang3.StringUtils;

import mo.umac.weha.data.Token;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.token.TokenReplace;

public class ContentSubstitution extends AbstractEditAction {
	
	private int lenCount = 0;
	
	{
		weight = 0.25;
	}

	public ContentSubstitution() {
		this.basicEdits = null;
	}
	
	public ContentSubstitution(AbstractEdit b) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = b;
		this.lenCount = 0;
	}

	public int findAction(AbstractEdit edit) {
		if (!(edit instanceof TokenReplace)) {
			return -1;
		}
		else {
			TokenReplace replEdit = (TokenReplace) edit;
			List<Token> delContent = replEdit.getOldTokens();
			List<Token> insContent = replEdit.getNewTokens();
			
			boolean flag = false;
			
			for (int i = 0; i < delContent.size(); i++) {
				String text = delContent.get(i).getContent();
				if (StringUtils.isAlphanumeric(text) || 
					text.matches("(\\p{Alpha}\\.){2,}")) {
					flag = true;
					break;
				}
			}
			if (!flag) return -1;
			
			for (int i = 0; i < insContent.size(); i++) {
				String text = insContent.get(i).getContent();
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
		ArrayList<AbstractEditAction> ret = new ArrayList<AbstractEditAction>();
		
		for (ListIterator<AbstractEdit> iter = editList.listIterator(); iter.hasNext(); ) {
			AbstractEdit b = iter.next();
			if (findAction(b) >= 0) {
				ret.add(new ContentSubstitution(b));
				iter.remove();
			}
		}
		
		return ret;
	}

	@Override
	public int lengthCount() {
		if (lenCount > 0) {
			return lenCount;
		}
		
		lenCount = 0;
		
		for (AbstractEdit edit : this.basicEdits) {
			if (edit instanceof TokenReplace) {
				TokenReplace replEdit = (TokenReplace) edit;
				List<Token> delContent = replEdit.getOldTokens();
				List<Token> insContent = replEdit.getNewTokens();
				
				for (int i = 0; i < delContent.size(); i++) {
					String text = delContent.get(i).getContent();
					if (StringUtils.isAlphanumeric(text) || 
						text.matches("(\\p{Alpha}\\.){2,}")) {
						lenCount++;
					}
				}
				
				for (int i = 0; i < insContent.size(); i++) {
					String text = insContent.get(i).getContent();
					if (StringUtils.isAlphanumeric(text) || 
						text.matches("(\\p{Alpha}\\.){2,}")) {
						lenCount++;
					}
				}		
			}
		}
		
		return lenCount;
	}
	
}
