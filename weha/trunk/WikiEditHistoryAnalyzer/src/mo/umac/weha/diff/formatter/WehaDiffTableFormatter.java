package mo.umac.weha.diff.formatter;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;
import mo.umac.weha.diff.paragraph.ParagraphEdit;
import mo.umac.weha.diff.paragraph.ParagraphMatch;
import mo.umac.weha.diff.sentence.SentenceDelete;
import mo.umac.weha.diff.sentence.SentenceEdit;
import mo.umac.weha.diff.sentence.SentenceInsert;
import mo.umac.weha.diff.sentence.SentenceMatch;
import mo.umac.weha.diff.token.TokenDelete;
import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.diff.token.TokenInsert;
import mo.umac.weha.diff.token.TokenMatch;
import mo.umac.weha.diff.token.TokenReplace;
import mo.umac.weha.util.comparator.ParagraphPositionComparator;

import org.apache.commons.lang3.StringEscapeUtils;

public class WehaDiffTableFormatter {

	public static String formatDiff(List<ParagraphEdit> paraDiff) {
		return formatDiff(paraDiff, new TreeMap<Integer, Integer>(), new TreeMap<Integer, Integer>());
	}
	
	public static String formatDiff(List<ParagraphEdit> paraDiff, Map<Integer, Integer> oldAnchorPos, Map<Integer, Integer> newAnchorPos) {
		List<ParagraphsPair> sortedParaDiff = new LinkedList<ParagraphsPair>();
		
		TreeSet<Paragraph> includedOldParas = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		TreeSet<Paragraph> includedNewParas = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		
		for (ParagraphEdit pe : paraDiff) {			
			TreeSet<Paragraph> notIncludedOldParas = new TreeSet<Paragraph>(new ParagraphPositionComparator());
			TreeSet<Paragraph> notIncludedNewParas = new TreeSet<Paragraph>(new ParagraphPositionComparator());
			
			for (Paragraph p : pe.getOldParagraphs()) {
				if (!includedOldParas.contains(p)) {
					notIncludedOldParas.add(p);
				}
			}
			
			for (Paragraph p : pe.getNewParagraphs()) {
				if (!includedNewParas.contains(p)) {
					notIncludedNewParas.add(p);
				}
			}
			
			
			if (!notIncludedOldParas.isEmpty() || !notIncludedNewParas.isEmpty()) {
				includedOldParas.addAll(notIncludedOldParas);
				includedNewParas.addAll(notIncludedNewParas);
				if (pe.isMovement()) {
					TreeSet<Paragraph> emptyParas = new TreeSet<Paragraph>(new ParagraphPositionComparator());
					
					if (!notIncludedOldParas.isEmpty()) {
						sortedParaDiff.add(new ParagraphsPair(pe, notIncludedOldParas, emptyParas));
					}
					if (!notIncludedNewParas.isEmpty()) {
						sortedParaDiff.add(new ParagraphsPair(pe, emptyParas, notIncludedNewParas));					
					}
				}
				else {
					sortedParaDiff.add(new ParagraphsPair(pe, notIncludedOldParas, notIncludedNewParas));
				}
			}
		}
		
		Collections.sort(sortedParaDiff, new Comparator<ParagraphsPair>() {
			public int compare(ParagraphsPair arg0, ParagraphsPair arg1) {
				return arg0.oldPos - arg1.oldPos;
			}
		});
		
		Collections.sort(sortedParaDiff, new Comparator<ParagraphsPair>() {
			public int compare(ParagraphsPair arg0, ParagraphsPair arg1) {
				if (arg0.oldPos < 0 || arg1.oldPos < 0 || arg0.oldPos == arg1.oldPos) {
					return arg0.newPos - arg1.newPos;
				}
				return 0;
			}
		});
		
		StringBuilder formatResult = new StringBuilder();
		
		formatResult.append("<table class=\"weha-diff\">\n");
		formatResult.append("<col class=\"weha-diff-content\" />\n");
		formatResult.append("<col class=\"weha-diff-content\" />\n");
		
		for (ParagraphsPair pp : sortedParaDiff) {
			formatResult.append(formatParagraphPair(pp, oldAnchorPos, newAnchorPos));
		}
		
		formatResult.append("</table>");
		
		return formatResult.toString();
	}

	private static Object formatParagraphPair(ParagraphsPair pp, Map<Integer, Integer> oldAnchorPos, Map<Integer, Integer> newAnchorPos) {
		StringBuilder formattedString = new StringBuilder();
		
		String paraEditText = StringEscapeUtils.escapeHtml4(pp.paraEdit.toString()).replace("\n", "<br />");
		
		if (pp.paraEdit instanceof ParagraphMatch) {
			if (!pp.paraEdit.isMovement()) {
				formattedString.append("<tr>");
				formattedString.append("<td class=\"weha-diff-para-match\" title=\"" + paraEditText + "\"><div>...</div></td>");
				formattedString.append("<td class=\"weha-diff-para-match\" title=\"" + paraEditText + "\"><div>...</div></td>");
				formattedString.append("</tr>");
			}
			else {
				formattedString.append("<tr>");
				
				if (!pp.oldParas.isEmpty()) {
					formattedString.append("<td class=\"weha-diff-para-mov\" title=\"" + paraEditText + "\"><div>");
					for (Paragraph p : pp.oldParas) {
						formattedString.append(StringEscapeUtils.escapeHtml4(p.getContent() + p.getTail()).replace("\n", "<br />"));
					}
					formattedString.append("</div></td>");
				}
				else {
					formattedString.append("<td class=\"weha-diff-para-match\" title=\"" + paraEditText + "\"><div></div></td>");
				}
				
				if (!pp.newParas.isEmpty()) {
					formattedString.append("<td class=\"weha-diff-para-mov\" title=\"" + paraEditText + "\"><div>");
					for (Paragraph p : pp.newParas) {
						formattedString.append(StringEscapeUtils.escapeHtml4(p.getContent() + p.getTail()).replace("\n", "<br />"));
					}
					formattedString.append("</div></td>");
				}
				else {
					formattedString.append("<td class=\"weha-diff-para-match\" title=\"" + paraEditText + "\"><div></div></td>");
				}
				
				formattedString.append("</tr>");
			}
			return formattedString.toString();
		}
		
		formattedString.append("<tr><td class=\"weha-diff-para-chg\" title=\"" + paraEditText + "\"><div>");
		
		for (Paragraph p : pp.oldParas) {
			formattedString.append(formatOldParagraph(p, oldAnchorPos, newAnchorPos));
		}
		
		formattedString.append("</div></td><td class=\"weha-diff-para-chg\" title=\"" + paraEditText + "\"><div>");
		
		for (Paragraph p : pp.newParas) {
			formattedString.append(formatNewParagraph(p, oldAnchorPos, newAnchorPos));
		}
		
		formattedString.append("</div></td></tr>\n");
		
		return formattedString.toString();
	}

	private static String formatOldParagraph(Paragraph p, Map<Integer, Integer> oldAnchorPos, Map<Integer, Integer> newAnchorPos) {
		StringBuilder formattedString = new StringBuilder();
		
		if (p.getSentenceEdits() != null && !p.getSentenceEdits().isEmpty()) {
			formattedString.append(formatOldSentenceEdit(p, oldAnchorPos, newAnchorPos));
		}
		else {
			formattedString.append(StringEscapeUtils.escapeHtml4(p.getContent()).replace("\n", "<br />"));
		}
		
		formattedString.append(StringEscapeUtils.escapeHtml4(p.getTail()).replace("\n", "<br />"));
		
		return formattedString.toString();
	}

	private static String formatOldSentenceEdit(Paragraph p, Map<Integer, Integer> oldAnchorPos, Map<Integer, Integer> newAnchorPos) {
		StringBuilder formattedString = new StringBuilder();
		
		List<SentenceEdit> sentenceEdits = p.getSentenceEdits();
		List<Sentence> sentences = p.splitIntoSentences();
		
		for (SentenceEdit se : sentenceEdits) {
			TreeSet<Sentence> oldSentences = new TreeSet<Sentence>(se.getOldSentences());
			Iterator<Sentence> iter = oldSentences.iterator();
			
			while (iter.hasNext()) {
				Sentence s = iter.next();
				if (!sentences.contains(s)) {
					iter.remove();
				}
			}
			
			if (oldSentences.isEmpty()) {
				continue;
			}
			else if (se.isMovement()) {
				formattedString.append("<span class=\"weha-diff-sentence-mov\" title=\"" + StringEscapeUtils.escapeHtml4(se.toString()) + "\">");
				
				boolean anchorOpen = false;
				for (Sentence s : oldSentences) {
					int pos = s.getPosition();
					
					if (se instanceof SentenceMatch && oldAnchorPos.containsKey(pos)) {
						if (anchorOpen) {
							formattedString.append("</a>");
							anchorOpen = false;
						}
						formattedString.append(createAnchorOpen(pos, oldAnchorPos.get(pos), "weha-ot", "weha-nt"));
						anchorOpen = true;
						oldAnchorPos.remove(pos);
					}
					
					formattedString.append(formatOldSentence(s, oldAnchorPos, newAnchorPos));
					sentences.remove(s);
				}
				
				if (anchorOpen) {
					formattedString.append("</a>");
				}
				formattedString.append("</span>");
			}
			else if (se instanceof SentenceMatch) {
				formattedString.append("<span class=\"weha-diff-sentence-match\">");
				for (Sentence s : oldSentences) {
					formattedString.append(StringEscapeUtils.escapeHtml4(s.getContent() + s.getTail()).replace("\n", "<br />"));
					sentences.remove(s);
				}
				formattedString.append("</span>");
			}
			else if (se instanceof SentenceDelete) {
				formattedString.append("<span class=\"weha-diff-sentence-del\" title=\"" + StringEscapeUtils.escapeHtml4(se.toString()) + "\">");
				
				boolean anchorOpen = false;
				for (Sentence s : oldSentences) {
					int pos = s.getPosition();
					
					if (oldAnchorPos.containsKey(pos)) {
						if (anchorOpen) {
							formattedString.append("</a>");
							anchorOpen = false;
						}
						formattedString.append(createAnchorOpen(pos, oldAnchorPos.get(pos), "weha-ot", "weha-nt"));
						anchorOpen = true;
						oldAnchorPos.remove(pos);
					}
					
					formattedString.append(StringEscapeUtils.escapeHtml4(s.getContent() + s.getTail()).replace("\n", "<br />"));
					sentences.remove(s);
				}
				
				if (anchorOpen) {
					formattedString.append("</a>");
				}
				formattedString.append("</span>");
			}
			else {
				formattedString.append("<span class=\"weha-diff-sentence-chg\" title=\"" + StringEscapeUtils.escapeHtml4(se.toString()) + "\">");
				for (Sentence s : oldSentences) {
					formattedString.append(formatOldSentence(s, oldAnchorPos, newAnchorPos));
					sentences.remove(s);
				}
				formattedString.append("</span>");
			}
		}
		
		return formattedString.toString();
	}

	private static String formatOldSentence(Sentence s, Map<Integer, Integer> oldAnchorPos, Map<Integer, Integer> newAnchorPos) {
		StringBuilder formattedString = new StringBuilder();
		List<TokenEdit> tokenEdits = s.getTokenEdits();
		
		if (tokenEdits == null) {
			formattedString.append(StringEscapeUtils.escapeHtml4(s.getContent()).replace("\n", "<br />"));
		}
		else {
			for (TokenEdit te : tokenEdits) {
				formattedString.append(formatOldTokenEdit(te, oldAnchorPos, newAnchorPos));
			}
		}
		
		formattedString.append(StringEscapeUtils.escapeHtml4(s.getTail()).replace("\n", "<br />"));
		
		return formattedString.toString();
	}

	private static String formatOldTokenEdit(TokenEdit te, Map<Integer, Integer> oldAnchorPos, Map<Integer, Integer> newAnchorPos) {
		StringBuilder formattedString = new StringBuilder();
		
		if (te instanceof TokenMatch) {
			TokenMatch tm = (TokenMatch) te;
			if (tm.isMovement()) {
				formattedString.append("<span class=\"weha-diff-mov\" title=\"" + StringEscapeUtils.escapeHtml4(te.toString()) + "\">");
			}
			else {
				formattedString.append("<span class=\"weha-diff-match\">");		
			}
		}
		else if (te instanceof TokenDelete || 
				 te instanceof TokenReplace) {
			formattedString.append("<span class=\"weha-diff-del" + 
					((te instanceof TokenReplace) ? " weha-diff-repl" : "" ) + 
					"\" title=\"" + StringEscapeUtils.escapeHtml4(te.toString()) + "\">");
		}
		
		boolean anchorOpen = false;
		for (Token t : te.getOldTokens()) {
			String text = StringEscapeUtils.escapeHtml4(t.toString()).replace("\n", "<br />");
			int pos = t.getPosition();
			
			if (oldAnchorPos.containsKey(pos)) {
				if (anchorOpen) {
					formattedString.append("</a>");
					anchorOpen = false;
				}
				formattedString.append(createAnchorOpen(pos, oldAnchorPos.get(pos), "weha-ot", "weha-nt"));
				anchorOpen = true;
				oldAnchorPos.remove(pos);
			}
			
			formattedString.append(text);
		}
		
		if (anchorOpen) {
			formattedString.append("</a>");
		}
		formattedString.append("</span>");
		
		return formattedString.toString();
	}
	
	private static String formatNewParagraph(Paragraph p, Map<Integer, Integer> oldAnchorPos, Map<Integer, Integer> newAnchorPos) {
		StringBuilder formattedString = new StringBuilder();

		if (p.getSentenceEdits() != null && !p.getSentenceEdits().isEmpty()) {
			formattedString.append(formatNewSentenceEdit(p, oldAnchorPos, newAnchorPos));
		}
		else {
			formattedString.append(StringEscapeUtils.escapeHtml4(p.getContent()).replace("\n", "<br />"));
		}
		formattedString.append(StringEscapeUtils.escapeHtml4(p.getTail()).replace("\n", "<br />"));

		return formattedString.toString();
	}

	private static String formatNewSentenceEdit(Paragraph p, Map<Integer, Integer> oldAnchorPos, Map<Integer, Integer> newAnchorPos) {
		StringBuilder formattedString = new StringBuilder();
		
		List<SentenceEdit> sentenceEdits = p.getSentenceEdits();
		List<Sentence> sentences = p.splitIntoSentences();
		
		for (SentenceEdit se : sentenceEdits) {
			TreeSet<Sentence> newSentences = new TreeSet<Sentence>(se.getNewSentences());
			Iterator<Sentence> iter = newSentences.iterator();
			
			while (iter.hasNext()) {
				Sentence s = iter.next();
				if (!sentences.contains(s)) {
					iter.remove();
				}
			}
			
			if (newSentences.isEmpty()) {
				continue;
			}
			else if (se.isMovement()) {
				formattedString.append("<span class=\"weha-diff-sentence-mov\" title=\"" + StringEscapeUtils.escapeHtml4(se.toString()) + "\">");
				
				boolean anchorOpen = false;
				for (Sentence s : newSentences) {
					int pos = s.getPosition();
					
					if (se instanceof SentenceMatch && newAnchorPos.containsKey(pos)) {
						if (anchorOpen) {
							formattedString.append("</a>");
							anchorOpen = false;
						}
						formattedString.append(createAnchorOpen(pos, newAnchorPos.get(pos), "weha-nt", "weha-ot"));
						anchorOpen = true;
						newAnchorPos.remove(pos);
					}
					
					formattedString.append(formatNewSentence(s, oldAnchorPos, newAnchorPos));
					sentences.remove(s);
				}
				
				if (anchorOpen) {
					formattedString.append("</a>");
				}
				formattedString.append("</span>");
			}
			else if (se instanceof SentenceMatch) {
				formattedString.append("<span class=\"weha-diff-sentence-match\">");
				for (Sentence s : newSentences) {
					formattedString.append(StringEscapeUtils.escapeHtml4(s.getContent() + s.getTail()).replace("\n", "<br />"));
					sentences.remove(s);
				}
				formattedString.append("</span>");
			}
			else if (se instanceof SentenceInsert) {
				formattedString.append("<span class=\"weha-diff-sentence-ins\" title=\"" + StringEscapeUtils.escapeHtml4(se.toString()) + "\">");
				
				boolean anchorOpen = false;
				for (Sentence s : newSentences) {
					int pos = s.getPosition();
					
					if (newAnchorPos.containsKey(pos)) {
						if (anchorOpen) {
							formattedString.append("</a>");
							anchorOpen = false;
						}
						formattedString.append(createAnchorOpen(pos, newAnchorPos.get(pos), "weha-nt", "weha-ot"));
						anchorOpen = true;
						newAnchorPos.remove(pos);
					}
					
					formattedString.append(StringEscapeUtils.escapeHtml4(s.getContent() + s.getTail()).replace("\n", "<br />"));
					sentences.remove(s);
				}
				
				if (anchorOpen) {
					formattedString.append("</a>");
				}
				formattedString.append("</span>");
			}
			else {
				formattedString.append("<span class=\"weha-diff-sentence-chg\" title=\"" + StringEscapeUtils.escapeHtml4(se.toString()) + "\">");
				for (Sentence s : newSentences) {
					formattedString.append(formatNewSentence(s, oldAnchorPos, newAnchorPos));
					sentences.remove(s);
				}
				formattedString.append("</span>");
			}
		}
		
		return formattedString.toString();
	}

	private static String formatNewSentence(Sentence s, Map<Integer, Integer> oldAnchorPos, Map<Integer, Integer> newAnchorPos) {
		StringBuilder formattedString = new StringBuilder();
		List<TokenEdit> tokenEdits = s.getTokenEdits();
		
		if (tokenEdits == null) {
			formattedString.append(StringEscapeUtils.escapeHtml4(s.getContent()).replace("\n", "<br />"));
		}
		else {
			for (TokenEdit te : tokenEdits) {
				formattedString.append(formatNewTokenEdit(te, oldAnchorPos, newAnchorPos));
			}
		}
		
		formattedString.append(StringEscapeUtils.escapeHtml4(s.getTail()).replace("\n", "<br />"));
		
		return formattedString.toString();
	}

	private static String formatNewTokenEdit(TokenEdit te, Map<Integer, Integer> oldAnchorPos, Map<Integer, Integer> newAnchorPos) {
		StringBuilder formattedString = new StringBuilder();
		
		if (te instanceof TokenMatch) {
			TokenMatch tm = (TokenMatch) te;
			if (tm.isMovement()) {
				formattedString.append("<span class=\"weha-diff-mov\" title=\"" + StringEscapeUtils.escapeHtml4(te.toString()) + "\">");
			}
			else {
				formattedString.append("<span class=\"weha-diff-match\">");
			}
		}
		else if (te instanceof TokenInsert || 
				 te instanceof TokenReplace) {
			formattedString.append("<span class=\"weha-diff-ins" + 
					((te instanceof TokenReplace) ? " weha-diff-repl" : "" ) + 
					"\" title=\"" + StringEscapeUtils.escapeHtml4(te.toString()) + "\">");
		}
		
		boolean anchorOpen = false;
		for (Token t : te.getNewTokens()) {
			String text = StringEscapeUtils.escapeHtml4(t.toString()).replace("\n", "<br />");
			int pos = t.getPosition();
			
			if (newAnchorPos.containsKey(pos)) {
				if (anchorOpen) {
					formattedString.append("</a>");
					anchorOpen = false;
				}
				formattedString.append(createAnchorOpen(pos, newAnchorPos.get(pos), "weha-nt", "weha-ot"));
				anchorOpen = true;
				newAnchorPos.remove(pos);
			}
			
			formattedString.append(text);
		}
		
		if (anchorOpen) {
			formattedString.append("</a>");
		}
		formattedString.append("</span>");
		
		return formattedString.toString();
	}
	
	private static String createAnchorOpen(int pos, int linkPos, String anchorPrefix, String linkPrefix) {
		StringBuilder ret = new StringBuilder();
		
		ret.append("<a class=\"weha-diff-anchor\" id=\"");
		ret.append(anchorPrefix);
		ret.append(pos);
		ret.append("\"");
		
		if (linkPos >= 0) {
			ret.append(" href=\"#");
			ret.append(linkPrefix);
			ret.append(linkPos);
			ret.append("\"");
		}
		
		ret.append(">");
		
		return ret.toString();
	}
	
}
