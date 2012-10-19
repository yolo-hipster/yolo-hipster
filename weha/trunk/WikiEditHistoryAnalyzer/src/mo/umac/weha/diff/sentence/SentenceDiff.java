package mo.umac.weha.diff.sentence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;
import mo.umac.weha.diff.engine.DiffEngine;
import mo.umac.weha.diff.engine.Match;
import mo.umac.weha.diff.engine.MatchInfo;
import mo.umac.weha.diff.movement.MovementLabeler;
import mo.umac.weha.diff.token.TokenDiff;
import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.diff.token.TokenMatch;
import mo.umac.weha.util.array.Sparse2DArray;
import mo.umac.weha.util.array.Sparse2DArrayColumnIterator;
import mo.umac.weha.util.array.Sparse2DArrayRowIterator;
import mo.umac.weha.util.array.Sparse2DCell;

public class SentenceDiff {
	private static DiffEngine<Sentence> diffEngine = new DiffEngine<Sentence>(1);
	private static MovementLabeler<SentenceEdit> movementLabeler = new MovementLabeler<SentenceEdit>();
	
	public static List<SentenceEdit> diff(List<Sentence> oldSentences, List<Sentence> newSentences) {
		List<SentenceEdit> sentenceEdits = new ArrayList<SentenceEdit>(); 
		Set<SentenceEdit> sentenceEditsSet = new HashSet<SentenceEdit>();
		
		Sparse2DArray matchingRateArray = new Sparse2DArray(oldSentences.size(), newSentences.size());

		// Sentence level exact matching
		MatchInfo matchResult = diffEngine.doDiff(oldSentences, newSentences);
		
		// Obtain exact match result
		int[] matchedOld = matchResult.getMatchedOld();
		int[] matchedNew = matchResult.getMatchedNew();
		Match[] matchesArray = matchResult.getMatchesArray();
		
		// Separate unmatched sentences into tokens
		List<Token> unmatchedOldTokens = separateTokens(oldSentences, matchedOld);
		List<Token> unmatchedNewTokens = separateTokens(newSentences, matchedNew);
		
		// Run token level differencing
		List<TokenEdit> tokenEdits = TokenDiff.diff(unmatchedOldTokens, unmatchedNewTokens);

		// Mark exact matches
		for (Match m: matchesArray) {
			int op = m.getOldIndex();
			int np = m.getNewIndex();
			int len = m.getLength();
			
			sentenceEditsSet.add(new SentenceMatch(oldSentences.subList(op, op + len), newSentences.subList(np, np + len)));
			for (int i = 0; i < len; i++) {
				matchingRateArray.set(1.0, op + i, np + i);
			}
		}
		
		// Process token level edits
		for (TokenEdit te : tokenEdits) {
			// Separate token edit into sentences.
			List<TokenEdit> steList = te.splitBySentence();
			
			for (TokenEdit ste : steList) {
				Sentence oldS = ste.getFirstOldSentence();
				Sentence newS = ste.getFirstNewSentence();
				int oldSIndex = (oldS == null) ? -1 : oldS.getArrayIndex();
				int newSIndex = (newS == null) ? -1 : newS.getArrayIndex();
				
				// If the edit is already recorded as a sentence match, ignore it
				if ((oldSIndex >= 0 && matchedOld[oldSIndex] != 0) ||
					(newSIndex >= 0 && matchedNew[newSIndex] != 0)) {
					continue;
				}
				
				// Add edits to old and new sentences, respectively.
				if (oldS != null) {
					oldS.addOldTokenEdit(ste);
				}
				
				if (newS != null) {
					newS.addNewTokenEdit(ste);
				}
				
				// Update sentence matching rate array.
				if (ste instanceof TokenMatch) {
					double mr = matchingRateArray.get(oldSIndex, newSIndex);
					mr += ste.getTokenLength() / (double) (oldS.getLength() + newS.getLength());
					assert(mr <= 1.0);
					matchingRateArray.set(mr, oldSIndex, newSIndex);
				}
			}
		}
		
		TreeMap< Integer, LinkedList<SentenceEdit> > oldSentenceEdits = new TreeMap< Integer, LinkedList<SentenceEdit> >();
		TreeMap< Integer, LinkedList<SentenceEdit> > newSentenceEdits = new TreeMap< Integer, LinkedList<SentenceEdit> >();
		
		// Mark sentence matching for each sentence in old version
		for (int i = 0; i < oldSentences.size(); i++)
		{
			if (matchedOld[i] != 0) {
				continue;
			}
			
			LinkedList<SentenceEdit> oldSentEditList = new LinkedList<SentenceEdit>();
			int prevJ = -1;
			
			Sparse2DArrayRowIterator riter = matchingRateArray.getRowIterator(i);
			
			if (!riter.hasNext()) {
				SentenceDelete edit = new SentenceDelete(oldSentences.get(i));
				oldSentEditList.addLast(edit);
			}
			
			while (riter.hasNext()) {
				Sparse2DCell tempCell = riter.next();
				int j = tempCell.getColumn();
				double mr = tempCell.getValue();
				
				SentenceEdit edit = new SentenceEdit(mr, oldSentences.get(i), newSentences.get(j));
				
				if (j != 0 && j == prevJ + 1) {
					SentenceEdit prevEdit = oldSentEditList.removeLast();
					
					edit = prevEdit.mergeWith(edit);
					oldSentEditList.addLast(edit);
				}
				else {
					oldSentEditList.addLast(edit);
				}
				
				prevJ = j;
			}
			
			oldSentenceEdits.put(i, oldSentEditList);
		}
		
		// Mark sentence matching for each sentence in new version
		for (int i = 0; i < newSentences.size(); i++)
		{
			if (matchedNew[i] != 0) {
				continue;
			}
			
			LinkedList<SentenceEdit> newSentEditList = new LinkedList<SentenceEdit>();
			int prevJ = -1;
			
			Sparse2DArrayColumnIterator citer = matchingRateArray.getColumnIterator(i);
			
			if (!citer.hasNext()) {
				SentenceInsert edit = new SentenceInsert(newSentences.get(i));
				newSentEditList.addLast(edit);
			}
			
			while (citer.hasNext()) {
				Sparse2DCell tempCell = citer.next();
				int j = tempCell.getRow();
				
				LinkedList<SentenceEdit> oldSentEditList = oldSentenceEdits.get(j);
				SentenceEdit edit = null;
				for (SentenceEdit se : oldSentEditList) {
					if (se.containsNewSentence(newSentences.get(i))) {
						edit = se;
					}
				}
				
				if (j != 0 && j == prevJ + 1) {
					SentenceEdit prevEdit = newSentEditList.removeLast();
					int editPos = oldSentEditList.indexOf(edit);
					
					oldSentEditList.remove(edit);
					edit = prevEdit.mergeWith(edit);
					newSentEditList.addLast(edit);
					oldSentEditList.add(editPos, edit);
				}
				else {
					newSentEditList.addLast(edit);
				}
				
				prevJ = j;
			}
			
			newSentenceEdits.put(i, newSentEditList);
		}
		
		for (LinkedList<SentenceEdit> seList : oldSentenceEdits.values()) {
			sentenceEditsSet.addAll(seList);
		}
		
		for (LinkedList<SentenceEdit> seList : newSentenceEdits.values()) {
			sentenceEditsSet.addAll(seList);
		}
		
		sentenceEdits.addAll(sentenceEditsSet);
		
		// Sentence move detection
		movementLabeler.labelMovement(sentenceEdits);
		
		// Sort the sentence differencing result
		Collections.sort(sentenceEdits, new Comparator<SentenceEdit>() {
			public int compare(SentenceEdit o1, SentenceEdit o2) {
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
					return ((o2.oldSentences.size() + o2.newSentences.size()) - 
							(o1.oldSentences.size() + o1.newSentences.size()));
				}
			}
		});
		
		return sentenceEdits;
	}
	
	private static List<Token> separateTokens(List<Sentence> sentences, int[] matchedArray) {
		List<Token> unmatchedTokens = new ArrayList<Token>();
		
		for (int i = 0; i < matchedArray.length; i++) {
			if (matchedArray[i] == 0) {
				List<Token> t = sentences.get(i).splitIntoTokens();
				unmatchedTokens.addAll(t);
			}
			else {
				unmatchedTokens.add(new MatchedSentence(sentences.get(i), matchedArray[i]));
			}
		}
		
		for (int i = 0; i < unmatchedTokens.size(); i++) {
			unmatchedTokens.get(i).setArrayIndex(i);
		}
		
		return unmatchedTokens;
	}
	
}
