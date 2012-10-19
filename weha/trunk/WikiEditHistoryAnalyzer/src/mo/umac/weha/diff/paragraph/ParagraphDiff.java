package mo.umac.weha.diff.paragraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.data.Sentence;
import mo.umac.weha.diff.engine.DiffEngine;
import mo.umac.weha.diff.engine.Match;
import mo.umac.weha.diff.engine.MatchInfo;
import mo.umac.weha.diff.movement.MovementLabeler;
import mo.umac.weha.diff.sentence.SentenceDiff;
import mo.umac.weha.diff.sentence.SentenceEdit;
import mo.umac.weha.util.comparator.ParagraphPositionComparator;

public class ParagraphDiff {
	private static DiffEngine<Paragraph> diffEngine = new DiffEngine<Paragraph>(1);
	private static MovementLabeler<ParagraphEdit> movementLabeler = new MovementLabeler<ParagraphEdit>();
	
	public static List<ParagraphEdit> diff(List<Paragraph> oldParagraphs, List<Paragraph> newParagraphs) {
		List<ParagraphEdit> paragraphEdits = new ArrayList<ParagraphEdit>();
		Set<ParagraphEdit> paragraphEditsSet = new HashSet<ParagraphEdit>();
		
		// Paragraph level exact matching
		MatchInfo matchResult = diffEngine.doDiff(oldParagraphs, newParagraphs);
		
		// Obtain exact match result
		int[] matchedOld = matchResult.getMatchedOld();
		int[] matchedNew = matchResult.getMatchedNew();
		Match[] matchesArray = matchResult.getMatchesArray();
		
		// Separate unmatched paragraph into sentences
		List<Sentence> unmatchedOldSentences = separateSentences(oldParagraphs, matchedOld);
		List<Sentence> unmatchedNewSentences = separateSentences(newParagraphs, matchedNew);
		
		// Run sentence level differencing
		List<SentenceEdit> sentenceEdits = SentenceDiff.diff(unmatchedOldSentences, unmatchedNewSentences);
		
		TreeMap< Integer, LinkedList<ParagraphEdit> > oldParagraphEdits = new TreeMap< Integer, LinkedList<ParagraphEdit> >();
		TreeMap< Integer, LinkedList<ParagraphEdit> > newParagraphEdits = new TreeMap< Integer, LinkedList<ParagraphEdit> >();
		Set<Paragraph> exactMatchedOldParas = new HashSet<Paragraph>();
		Set<Paragraph> exactMatchedNewParas = new HashSet<Paragraph>();
		
		// Mark exact matches
		for (Match m: matchesArray) {
			int op = m.getOldIndex();
			int np = m.getNewIndex();
			int len = m.getLength();
			
			ParagraphEdit paraMatch = new ParagraphMatch(oldParagraphs.subList(op, op+len), newParagraphs.subList(np, np+len));
			
			for (int i = op; i < op + len; i++) {
				LinkedList<ParagraphEdit> peList = getParagraphEditList(oldParagraphEdits, oldParagraphs.get(i).getPosition());
				peList.add(paraMatch);
				oldParagraphEdits.put(oldParagraphs.get(i).getPosition(), peList);
				
				exactMatchedOldParas.add(oldParagraphs.get(i));
			}
			
			for (int i = np; i < np + len; i++) {
				LinkedList<ParagraphEdit> peList = getParagraphEditList(newParagraphEdits, newParagraphs.get(i).getPosition());
				peList.add(paraMatch);
				newParagraphEdits.put(newParagraphs.get(i).getPosition(), peList);
				
				exactMatchedNewParas.add(newParagraphs.get(i));
			}
		}
		
		// Process sentence level edits
		for (SentenceEdit se : sentenceEdits) {
			List<SentenceEdit> pseList = se.splitByParagraph();
			
			for (SentenceEdit pse : pseList) {
				TreeSet<Sentence> oldSents = new TreeSet<Sentence>(pse.getOldSentences());
				TreeSet<Sentence> newSents = new TreeSet<Sentence>(pse.getNewSentences());
				
				// Create paragraph edit
				Sentence os = oldSents.pollFirst();
				Paragraph op = (os == null) ? null : os.getParagraph();
				
				if (op != null) {
					if (exactMatchedOldParas.contains(op)) {
						continue;
					}
					else {
						op.addOldSentenceEdit(pse);
					}
				}
				
				Sentence ns = newSents.pollFirst();
				Paragraph np = (ns == null) ? null : ns.getParagraph();
				
				if (np != null) {
					if (exactMatchedNewParas.contains(np)) {
						continue;
					}
					else {
						np.addNewSentenceEdit(pse);
					}
				}
						
				ParagraphEdit pe = ParagraphEdit.createParagraphEdit(op, np);
				
				// Add paragraph edits to corresponding paragraphs and merge consecutive edits
				if (op != null) {
					LinkedList<ParagraphEdit> peList = getParagraphEditList(oldParagraphEdits, op.getPosition());
					boolean addToList = true;
					
					for (ParagraphEdit e : peList) {
						if (e.contains(pe)) {
							addToList = false;
							break;
						}
						else if (pe.containsOldParagraphs(e) && 
								 paragraphsAreConsecutive(e.newParagraphs, pe.newParagraphs, newParagraphs)) {
							addToList = false;
							pe.mergeWith(e.mergeWith(pe));
						}
					}
					
					if (addToList) {
						peList.add(pe);
					}
					oldParagraphEdits.put(op.getPosition(), peList);
				}
				
				if (np != null) {
					LinkedList<ParagraphEdit> peList = getParagraphEditList(newParagraphEdits, np.getPosition());
					boolean addToList = true;
					
					for (ParagraphEdit e : peList) {
						if (e.contains(pe)) {
							addToList = false;
							break;
						}
						else if (pe.containsNewParagraphs(e) && 
								 paragraphsAreConsecutive(e.oldParagraphs, pe.oldParagraphs, oldParagraphs)) {
							addToList = false;
							pe.mergeWith(e.mergeWith(pe));
						}
					}
					
					if (addToList) {
						peList.add(pe);
					}
					newParagraphEdits.put(np.getPosition(), peList);
				}
			}
		}
		
		for (LinkedList<ParagraphEdit> peList : oldParagraphEdits.values()) {
			paragraphEditsSet.addAll(peList);
		}
		
		for (LinkedList<ParagraphEdit> peList : newParagraphEdits.values()) {
			paragraphEditsSet.addAll(peList);
		}
		
		paragraphEdits.addAll(paragraphEditsSet);

		// Paragraph move detection
		movementLabeler.labelMovement(paragraphEdits);
		
		// Sort the paragraph differencing result
		Collections.sort(paragraphEdits, new Comparator<ParagraphEdit>() {
			public int compare(ParagraphEdit o1, ParagraphEdit o2) {
				if (Math.abs(o2.matchingRate - o1.matchingRate) > 1E-6) {
					return (int) Math.signum(o2.matchingRate - o1.matchingRate);
				}
				else if (o1.oldPos - o2.oldPos != 0) {
					return o1.oldPos - o2.oldPos;
				}
				else if (o1.newPos - o2.newPos != 0) {
					return o1.newPos - o2.newPos;
				}
				else {
					return ((o2.oldParagraphs.size() + o2.newParagraphs.size()) - 
							(o1.oldParagraphs.size() + o1.newParagraphs.size()));
				}
			}
		});
		
		return paragraphEdits;
	}
	
	private static LinkedList<ParagraphEdit> getParagraphEditList(TreeMap< Integer, LinkedList<ParagraphEdit> > paraEditMap, int index) {
		LinkedList<ParagraphEdit> peList = null;
		
		if (paraEditMap.containsKey(index)) {
			peList = paraEditMap.get(index);
		}
		else {
			peList = new LinkedList<ParagraphEdit>();
		}
		
		return peList;
	}

	private static boolean paragraphsAreConsecutive(
			TreeSet<Paragraph> ps1, TreeSet<Paragraph> ps2, List<Paragraph> paragraph) {
		
		TreeSet<Paragraph> ps = new TreeSet<Paragraph>(new ParagraphPositionComparator());
		
		ps.addAll(ps1);
		ps.addAll(ps2);
		
		int prevPos = paragraph.indexOf(ps.first());
		
		for (Paragraph p : ps) {
			if (p.getPosition() != paragraph.get(prevPos).getPosition()) {
				return false;
			}
			prevPos++;
		}
		
		return true;
	}
	
	private static List<Sentence> separateSentences(List<Paragraph> paragraphs, int[] matchedArray) {
		List<Sentence> unmatchedSentences = new ArrayList<Sentence>();
		
		for (int i = 0; i < matchedArray.length; i++) {
			if (matchedArray[i] == 0) {
				List<Sentence> s = paragraphs.get(i).splitIntoSentences();
				unmatchedSentences.addAll(s);
			}
			else {
				unmatchedSentences.add(new MatchedParagraph(paragraphs.get(i), matchedArray[i]));
			}
		}
		
		for (int i = 0; i < unmatchedSentences.size(); i++) {
			unmatchedSentences.get(i).setArrayIndex(i);
		}
		
		return unmatchedSentences;
	}
	
}
