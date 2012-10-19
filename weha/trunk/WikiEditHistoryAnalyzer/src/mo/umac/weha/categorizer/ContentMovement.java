package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.sentence.SentenceMatch;
import mo.umac.weha.diff.token.TokenMatch;

public class ContentMovement extends AbstractEditAction {
	
	{
		weight = 0.5;
	}
	
	public ContentMovement() {
		this.basicEdits = null;
	}
	
	public ContentMovement(AbstractEdit b) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = b;
	}

	public int findAction(AbstractEdit edit) {
		if (((edit instanceof SentenceMatch) || (edit instanceof TokenMatch)) && 
			edit.isMovement()) {
			return 0;
		}
		else {
			return -1;
		}
	}

	@Override
	public List<AbstractEditAction> classify(List<AbstractEdit> editList) {
		ArrayList<AbstractEditAction> ret = new ArrayList<AbstractEditAction>();
		
		for (ListIterator<AbstractEdit> iter = editList.listIterator(); iter.hasNext(); ) {
			AbstractEdit b = iter.next();
			
			if (findAction(b) >= 0) {
				ret.add(new ContentMovement(b));
				iter.remove();
			}
		}
		
		return ret;
	}

	@Override
	public int lengthCount() {
		return 1;
	}
	
}
