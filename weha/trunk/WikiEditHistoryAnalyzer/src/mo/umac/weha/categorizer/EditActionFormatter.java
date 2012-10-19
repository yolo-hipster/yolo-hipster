package mo.umac.weha.categorizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.formatter.WehaDiffTableFormatter;
import mo.umac.weha.diff.paragraph.ParagraphEdit;

public class EditActionFormatter {
	
	public static String formatAction(List<AbstractEditAction> actionList, List<ParagraphEdit> paraDiff) {
		StringBuilder retString = new StringBuilder();
		
		Map<String, ArrayList<AbstractEditAction>> actionMap = 
				new TreeMap<String, ArrayList<AbstractEditAction>>();
		
		for (AbstractEditAction action : actionList) {
			String actionName = action.getClass().getSimpleName();
			
			ArrayList<AbstractEditAction> preList;
			if (actionMap.containsKey(actionName)) {
				preList = actionMap.get(actionName);
			}
			else {
				preList = new ArrayList<AbstractEditAction>();
			}
			preList.add(action);
			actionMap.put(actionName, preList);
		}
		
		Iterator<String> iter = actionMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			retString.append("<div class='weha-actionname' onclick='toggleExpand(\"weha-actiondetail-" + key.toLowerCase() + "\");'>" + key + "</div>\n");
			
			retString.append("<div class='weha-actionlist' id='weha-actiondetail-" + key.toLowerCase() + "'><ul>\n");
			if (actionMap.containsKey(key)) {
				ArrayList<AbstractEditAction> aeList = actionMap.get(key);
				ListIterator<AbstractEditAction> aeIter = aeList.listIterator();
				
				while (aeIter.hasNext()) {
					AbstractEditAction ae = aeIter.next();
					retString.append("<li>");
					AbstractEdit[] be = ae.getBasicEdits();
					
					for (int i = 0; i < be.length; i++) {
						retString.append(createLinkedDesc(be[i]) + "; ");
					}
					
					retString.append("</li>\n");
				}
			}
			
			retString.append("</ul></div>");
		}
		
		Map<Integer, Integer> oldAnchorPos = new TreeMap<Integer, Integer>();
		Map<Integer, Integer> newAnchorPos = new TreeMap<Integer, Integer>();
		
		for (AbstractEditAction action : actionList) {
			for (AbstractEdit ae : action.basicEdits) {
				oldAnchorPos.put(ae.getOldPos(), ae.getNewPos());
				newAnchorPos.put(ae.getNewPos(), ae.getOldPos());
			}
		}
		
		retString.append(WehaDiffTableFormatter.formatDiff(paraDiff, oldAnchorPos, newAnchorPos));
		
		return retString.toString();
	}

	private static String createLinkedDesc(AbstractEdit abstractEdit) {
		StringBuilder retString = new StringBuilder();
		
		retString.append(abstractEdit.getClass().getSimpleName());
		int oldPos = abstractEdit.getOldPos();
		int newPos = abstractEdit.getNewPos();
		
		retString.append(" (");
		if (oldPos >= 0) {
			retString.append("<a class=\"weha-diff-link\" href=\"#weha-ot");
			retString.append(oldPos);
			retString.append("\">");
			retString.append(oldPos);
			retString.append("</a>");
		}
		
		if (oldPos >= 0 && newPos >= 0) {
			retString.append(", ");
		}
		
		if (newPos >= 0) {
			retString.append("<a class=\"weha-diff-link\" href=\"#weha-nt");
			retString.append(newPos);
			retString.append("\">");
			retString.append(newPos);
			retString.append("</a>");
		}
		retString.append(")");
		
		return retString.toString();
	}
	
}
