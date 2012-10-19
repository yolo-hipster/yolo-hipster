package mo.umac.weha.summarizer;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import mo.umac.weha.categorizer.AbstractEditAction;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.formatter.WehaDiffTableFormatter;
import mo.umac.weha.diff.paragraph.ParagraphEdit;

public class EditSignificanceFormatter {
	
	public static String formatAction(
			Map<String, List<AbstractEditAction>> actionMap,
			Map<String, Double> outputStatistic, List<ParagraphEdit> paraDiff) {
		StringBuilder retString = new StringBuilder();
		Map<Integer, Integer> oldAnchorPos = new TreeMap<Integer, Integer>();
		Map<Integer, Integer> newAnchorPos = new TreeMap<Integer, Integer>();
		double sigTotal = 0.0;
		
		Iterator<String> iter = actionMap.keySet().iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			retString.append("<div class='weha-actionname' onclick='toggleExpand(\"weha-actiondetail-" + key.toLowerCase() + "\");'>" + key);
			if (outputStatistic.containsKey(key)) {
				double sigValue = outputStatistic.get(key);
				retString.append(": " + sigValue);
				sigTotal += sigValue;
			}
			retString.append("</div>\n");
			
			retString.append("<div class='weha-actionlist' id='weha-actiondetail-" + key.toLowerCase() + "'><ul>\n");
			if (actionMap.containsKey(key)) {
				List<AbstractEditAction> aeList = actionMap.get(key);
				ListIterator<AbstractEditAction> aeIter = aeList.listIterator();
				
				while (aeIter.hasNext()) {
					AbstractEditAction ae = aeIter.next();
					retString.append("<li>");
					AbstractEdit[] be = ae.getBasicEdits();
					
					for (int i = 0; i < be.length; i++) {
						retString.append(createLinkedDesc(be[i]) + "; ");
					}
					
					retString.append("</li>\n");
					
					for (AbstractEdit a : ae.getBasicEdits()) {
						oldAnchorPos.put(a.getOldPos(), a.getNewPos());
						newAnchorPos.put(a.getNewPos(), a.getOldPos());
					}
				}
			}
			
			retString.append("</ul></div>");
		}
		
		retString.append("<div class=\"weha-sig-total\">Total edit significance: ");
		retString.append(sigTotal);
		retString.append("</div>");
		
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
