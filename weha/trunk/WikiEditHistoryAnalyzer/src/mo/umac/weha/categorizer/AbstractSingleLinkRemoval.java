package mo.umac.weha.categorizer;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.TreeSet;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.sentence.SentenceDelete;
import mo.umac.weha.diff.token.TokenDelete;
import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.diff.token.TokenReplace;

public abstract class AbstractSingleLinkRemoval extends AbstractEditAction {

	protected Set<String> prefixes;

	@Override
	public int findAction(AbstractEdit edit) {
		if (!(edit instanceof SentenceDelete) && 
			!(edit instanceof TokenDelete) && 
			!(edit instanceof TokenReplace)) {
			return -1;
		}
		else if (edit instanceof SentenceDelete) {
			SentenceDelete delEdit = (SentenceDelete) edit;
			TreeSet<Sentence> sentences = delEdit.getOldSentences();
			List<Token> content = sentences.first().splitIntoTokens();
			
			if (content.size() < 3) {
				return -1;
			}
			
			if (content.get(0).getContent().equals("[[") && 
				content.get(2).getContent().equals(":") && 
				prefixes.contains(content.get(1).getContent().toLowerCase()) && 
				content.get(content.size() - 1).getContent().equals("]]")) {
				return 0;
			}
		}
		else {
			TokenEdit tokEdit = (TokenEdit) edit;
			List<Token> content = tokEdit.getOldTokens();
			
			for (int i = 0; i < content.size() - 2; i++) {
				if (content.get(i).getContent().equals("[[") && 
					content.get(i+2).getContent().equals(":") && 
					prefixes.contains(content.get(i+1).getContent().toLowerCase())) {
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
			int linkStartPos = this.findAction(ae);
			
			if (linkStartPos >= 0) {
				try {
					Class<? extends AbstractSingleLinkRemoval> aaClass = this.getClass();
					Constructor<? extends AbstractSingleLinkRemoval> aaConstructor = aaClass.getConstructor(AbstractEdit.class);
					
					if (ae instanceof SentenceDelete) {
						classifiedActionList.add(aaConstructor.newInstance(ae));
						iter.remove();
					}
					else {
						TokenEdit tokEdit = (TokenEdit) ae;
						List<Token> content = tokEdit.getOldTokens();
						int linkEndPos = -1;
						int doubleBracketLevel = 0;
						
						for (int i = linkStartPos; i < content.size(); i++) {
							if (content.get(i).getContent().equals("[[")) {
								doubleBracketLevel++;
							}
							if (content.get(i).getContent().equals("]]")) {
								doubleBracketLevel--;
								if (doubleBracketLevel == 0) {
									linkEndPos = i;
									break;
								}
							}
						}
						
						if (linkEndPos > linkStartPos) {
							TokenEdit[] splittedEdit = tokEdit.splitOldTokens(linkStartPos, linkEndPos + 1);
							classifiedActionList.add(aaConstructor.newInstance(splittedEdit[1]));
							
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
				catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
		
		return classifiedActionList;
	}
	
	public int lengthCount() {
		return 1;
	}
}
