package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.paragraph.ParagraphMatch;
import mo.umac.weha.diff.sentence.SentenceMatch;
import mo.umac.weha.diff.token.TokenMatch;

public class Uncategorized extends AbstractEditAction {
	
	{
		weight = 0;
	}
	
	public Uncategorized() {
		this.basicEdits = null;
	}
	
	public Uncategorized(AbstractEdit ae) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = ae;
	}

	public int findAction(AbstractEdit edit) {
		if (edit instanceof ParagraphMatch || edit instanceof SentenceMatch || edit instanceof TokenMatch) {
			if (edit.isMovement()) {
				return 0;
			}
			else {
				return -1;
			}
		}
		else {
			return 0;
		}
	}
	
	public List<AbstractEditAction> classify(List<AbstractEdit> editList) {
		List<AbstractEditAction> classifiedActionList = new ArrayList<AbstractEditAction>();
		
		for (ListIterator<AbstractEdit> iter = editList.listIterator(); iter.hasNext(); ) {
			AbstractEdit ae = iter.next();
			if (this.findAction(ae) >= 0) {
				classifiedActionList.add(new Uncategorized(ae));
				iter.remove();
			}
		}
		
		return classifiedActionList;
	}
	
	public int lengthCount() {
		return 0;
	}
}
