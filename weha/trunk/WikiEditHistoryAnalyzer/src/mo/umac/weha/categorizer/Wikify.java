package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import mo.umac.weha.data.Token;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.diff.token.TokenInsert;
import mo.umac.weha.diff.token.TokenMatch;
import mo.umac.weha.diff.token.TokenReplace;

public class Wikify extends AbstractEditAction {
	
	{
		weight = 0.5;
	}
	
	public Wikify() {
		this.basicEdits = null;
	}
	
	public Wikify(AbstractEdit b1, AbstractEdit b2) {
		this.basicEdits = new AbstractEdit[2];
		this.basicEdits[0] = b1;
		this.basicEdits[1] = b2;
	}
	
	public Wikify(AbstractEdit b) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = b;
	}
	
	public Wikify(AbstractEdit[] be) {
		this.basicEdits = be;
	}
	
	public int findAction(AbstractEdit edit) {
		if (!(edit instanceof TokenInsert) && !(edit instanceof TokenReplace)) {
			return -1;
		}
		
		TokenEdit tokEdit = (TokenEdit) edit;
		List<Token> content = tokEdit.getNewTokens();
		boolean flag = false;
		int lastOpen = -1;
		
		for (int i = 0; i < content.size(); i++) {
			String contentString = content.get(i).getContent();
			
			if (contentString.matches("''|'''|'''''")) {
				flag = !flag;
				lastOpen = flag ? i : lastOpen;
			}
			if (contentString.equals("[[")) {
				flag = true;
				lastOpen = i;
			}
			if (contentString.equals("]]")) {
				flag = false;
			}
		}
		
		if (flag) {
			return lastOpen;
		}
		
		return -1;
	}
	
	public int findClose(AbstractEdit edit, String tOpen) {
		if (!(edit instanceof TokenInsert) && !(edit instanceof TokenReplace)) {
			return -1;
		}
		
		TokenEdit tokEdit = (TokenEdit) edit;
		List<Token> content = tokEdit.getNewTokens();
		
		if (tOpen.matches("''|'''|'''''")) {
			boolean flag = false;
			int firstClose = -1;
			
			for (int i = content.size() - 1; i >= 0; i--) {
				String contentString = content.get(i).getContent();
				
				if (contentString.matches("''|'''|'''''")) {
					flag = !flag;
					firstClose = flag ? i : firstClose;
				}
			}
			
			if (flag) {
				return firstClose;
			}
		}
		else if (tOpen.equals("[[")) { 
			for (int i = 0; i < content.size(); i++) {
				String contentString = content.get(i).getContent();
				
				if (contentString.equals("[[")) {
					return -1;
				}
				else if (contentString.equals("]]")) {
					return i;
				}
			}
			
		}
		
		return -1;
	}
	
	public int findActionSingle(AbstractEdit edit) {
		if (!(edit instanceof TokenReplace)) {
			return -1;
		}
		else {
			TokenReplace replEdit = (TokenReplace) edit;
			String oldStr = new String();
			String newStr = new String();
			
			for (Token t : replEdit.getOldTokens()) {
				oldStr += t.getContent();
			}
			
			for (Token t : replEdit.getNewTokens()) {
				newStr += t.getContent();
			}
			
			newStr = newStr.replaceAll("\\[\\[|\\]\\]", "");
			
			if (oldStr.equals(newStr)) {
				return 0;
			}
		}
		
		return -1;
	}
	
	@Override
	public List<AbstractEditAction> classify(List<AbstractEdit> editList) {
		ArrayList<AbstractEditAction> classifiedActionList = new ArrayList<AbstractEditAction>();
		
		Collections.sort(editList, new Comparator<AbstractEdit>() {
			@Override
			public int compare(AbstractEdit arg0, AbstractEdit arg1) {
				return arg0.getNewPos() - arg1.getNewPos();
			}
		});
		
		for (ListIterator<AbstractEdit> iter = editList.listIterator(); iter.hasNext(); ) {
			int b1Index = iter.nextIndex();
			AbstractEdit b1 = iter.next();
			
			int b2Index = iter.nextIndex();
			AbstractEdit b2 = iter.hasNext() ? iter.next() : null;
			
			while (b2 != null && b2 instanceof TokenMatch) {
				b2Index = iter.nextIndex();
				b2 = iter.hasNext() ? iter.next() : null;
			}
			
			if (b2 == null) {
				break;
			}
			
			int markupOpen = findAction(b1);
			int markupSingle = findActionSingle(b1);
			
			if (markupSingle >= 0) {
				classifiedActionList.add(new Wikify(b1));
				
				do {
					iter.previous();
				} while (iter.previousIndex() >= b1Index);
				iter.remove();
			}
			else if (markupOpen >= 0) {
				TokenEdit te1 = (TokenEdit) b1;
				String tOpen = te1.getNewTokens().get(markupOpen).getContent();
				
				int markupClose = findClose(b2, tOpen);
				
				if (markupClose >= 0) {
					TokenEdit te2 = (TokenEdit) b2;
					
					TokenEdit[] te1Array = te1.splitNewTokens(markupOpen, te1.getNewTokens().size());
					TokenEdit[] te2Array = te2.splitNewTokens(0, markupClose + 1);
					
					classifiedActionList.add(new Wikify(te1Array[1], te2Array[1]));
					
					do {
						iter.previous();
					} while (iter.previousIndex() >= b1Index);
					iter.remove();
					
					if (te1Array[0] != null) {
						iter.add(te1Array[0]);
						b2Index++;
					}
					
					if (te1Array[2] != null) {
						iter.add(te1Array[2]);
						b2Index++;
					}
					
					do {
						iter.next();
					} while (iter.nextIndex() < b2Index);
					iter.remove();
					
					if (te2Array[2] != null) {
						if (iter.hasNext()) {
							iter.next();
						}
						iter.add(te2Array[2]);
						iter.previous();
					}
				}
			}
			else {
				iter.previous();
			}
		}
		
		return classifiedActionList;
	}
	
	public int lengthCount() {
		return 1;
	}
}
