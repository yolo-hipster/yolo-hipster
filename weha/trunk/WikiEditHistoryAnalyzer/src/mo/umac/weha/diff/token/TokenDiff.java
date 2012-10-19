package mo.umac.weha.diff.token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import mo.umac.weha.data.Token;
import mo.umac.weha.diff.engine.DiffEngine;
import mo.umac.weha.diff.engine.Match;
import mo.umac.weha.diff.engine.MatchInfo;
import mo.umac.weha.diff.sentence.MatchedSentence;
import mo.umac.weha.util.StopWords;

import org.apache.commons.lang3.StringUtils;

public class TokenDiff {
	private static DiffEngine<Token> diffEngine = new DiffEngine<Token>(3);
	private static Set<String> openMarkupSet = new HashSet<String>();
	private static Set<String> closeMarkupSet = new HashSet<String>();
	static {
		openMarkupSet.add("[[");
		openMarkupSet.add("{{");
		openMarkupSet.add("{|");
		
		closeMarkupSet.add("]]");
		closeMarkupSet.add("}}");
		closeMarkupSet.add("|}");
	}
	
	public static List<TokenEdit> diff(List<Token> oldTokens, List<Token> newTokens) {		
		List<TokenEdit> tokenEdits = new ArrayList<TokenEdit>();
		
		MatchInfo matchResult = diffEngine.doDiff(oldTokens, newTokens);
		
		int[] matchedOld = matchResult.getMatchedOld();
		int[] matchedNew = matchResult.getMatchedNew();
		List<Match> tokenMatches = new LinkedList<Match>( Arrays.asList( matchResult.getMatchesArray() ) );
		
		// Shift match boundary in the case of open or close markup
		shiftMatchBoundary(tokenMatches, oldTokens, newTokens, matchedOld, matchedNew);
		
		// Transform Match from diff engine to TokenMatch
		LinkedList<TokenMatch> matchesList = new LinkedList<TokenMatch>();
		for(Match m : tokenMatches) {
			List<Token> ot = oldTokens.subList(m.getOldIndex(), m.getOldIndex() + m.getLength());
			List<Token> nt = newTokens.subList(m.getNewIndex(), m.getNewIndex() + m.getLength());
			
			// Remove matches that contain only stop words
			boolean onlyStopWords = containsOnlyStopWords(ot);
			
			if (!onlyStopWords) {
				TokenMatch tm = new TokenMatch(m.getLength(), ot, nt);
				matchesList.add(tm);
			}
			else {
				int oldIndex = m.getOldIndex();
				int newIndex = m.getNewIndex();
				for (int i = 0; i < m.getLength(); i++) {
					matchedOld[oldIndex + i] = 0;
					matchedNew[newIndex + i] = 0;
				}
			}
		}
		
		// Marking movements
		labelMovement(matchesList, matchedOld, matchedNew);
		
		// Marking insert and delete
		List<TokenDelete> tokenDeletes = new ArrayList<TokenDelete>();
		List<TokenInsert> tokenInserts = new ArrayList<TokenInsert>();

		boolean inMatched = true;
		int unmatchedStart = 0;
		
		for (int i = 0; i < matchedOld.length; i++)
		{
			if (inMatched && matchedOld[i] == 0) {
				inMatched = false;
				unmatchedStart = i;
			}
			
			if (!inMatched && matchedOld[i] != 0) {
				inMatched = true;
				if (i > unmatchedStart) {
					tokenDeletes.add(new TokenDelete(oldTokens.subList(unmatchedStart, i)));
				}
			}
		}
		
		if (!inMatched && matchedOld.length > unmatchedStart) {
			tokenDeletes.add(new TokenDelete(oldTokens.subList(unmatchedStart, oldTokens.size())));
		}
		
		inMatched = true;
		unmatchedStart = 0;
		
		for (int i = 0; i < matchedNew.length; i++)
		{
			if (inMatched && matchedNew[i] == 0) {
				inMatched = false;
				unmatchedStart = i;
			}
			
			if (!inMatched && matchedNew[i] != 0) {
				inMatched = true;
				if (i > unmatchedStart)	{
					tokenInserts.add(new TokenInsert(newTokens.subList(unmatchedStart, i)));
				}
			}
		}
		
		if (!inMatched && matchedNew.length > unmatchedStart) {
			tokenInserts.add(new TokenInsert(newTokens.subList(unmatchedStart, newTokens.size())));
		}
		
		// Marking replacements
		List<TokenEdit> tokenReplEdits = new LinkedList<TokenEdit>();
		
		for (ListIterator<TokenDelete> tdIter = tokenDeletes.listIterator(); tdIter.hasNext(); )
		{
			TokenDelete td = tdIter.next();
			int delBegin = td.getOldIndex();
			int delEnd = delBegin + td.oldTokens.size();
			List<Token> delContent = td.oldTokens;
			
			for (ListIterator<TokenInsert> tiIter = tokenInserts.listIterator(); tiIter.hasNext(); )
			{
				TokenInsert ti = tiIter.next();
				int insBegin = ti.getNewIndex();
				int insEnd = insBegin + ti.newTokens.size();
				List<Token> insContent = ti.newTokens;
				
				if ((matchedOld[Math.max(delBegin - 1, 0)] == 
					 matchedNew[Math.max(insBegin - 1, 0)]) ||
					(matchedOld[Math.min(delEnd, matchedOld.length - 1)] == 
					 matchedNew[Math.min(insEnd, matchedNew.length - 1)]))
				{
					tokenReplEdits.add(new TokenReplace(delContent, insContent));
					tdIter.remove();
					tiIter.remove();
					break;
				}
				
			}
		}
		
		tokenEdits.addAll(tokenDeletes);
		tokenEdits.addAll(tokenInserts);
		
		// Pair tag add/remove hack: deal with wiki markup add/remove
		// e.g. Test -> [[Test]]
		tokenEdits.addAll(tokenReplEdits);
		findMatchesInReplace(tokenReplEdits, matchesList, tokenEdits);
		
		labelMovement(matchesList, matchedOld, matchedNew, false);
		tokenEdits.addAll(matchesList);
		
		return tokenEdits;
	}
	
	private static void findMatchesInReplace(List<TokenEdit> tokenReplEdits, List<TokenMatch> matchesList, List<TokenEdit> tokenEdits) {
		for (ListIterator<TokenEdit> replIter = tokenReplEdits.listIterator(); replIter.hasNext(); )
		{
			TokenEdit e = replIter.next();
			
			if (e instanceof TokenReplace)
			{
				TokenReplace r = (TokenReplace) e;
				List<Token> insContent = r.newTokens;
				List<Token> delContent = r.oldTokens;
				
				// If either side of replacement contains more than 500 tokens, skip
				if (insContent.size() > 500 || delContent.size() > 500) {
					continue;
				}
				
				int maxDelIndex = -1;
				int maxInsIndex = -1;
				int maxLength = 0;
			
				if (insContent.size() <= delContent.size())	{
					for (int j = 0; j < insContent.size(); j++)	{
						int delIndex = delContent.indexOf(insContent.get(j));
					
						if (delIndex > -1) {
							int containLength = 1;
							
							for (int k = 1; j + k < insContent.size() && k + delIndex < delContent.size(); k++)	{
								if (insContent.get(j+k).equals(delContent.get(delIndex+k))) {
									containLength++;
								}
								else {
									break;
								}
							}
							
							if (containLength > maxLength) {
								List<Token> om = delContent.subList(delIndex, delIndex + containLength);
								boolean onlyStopWords = containsOnlyStopWords(om);
								
								if (!onlyStopWords || containLength > 1 || 
									 j == 0 || delIndex == 0 ||
									 j == insContent.size() - 1 ||
									 delIndex == delContent.size() - 1) {
									maxDelIndex = delIndex;
									maxInsIndex = j;
									maxLength = containLength;
								}
							}
						}
					}
				}
				else {
					for (int j = 0; j < delContent.size(); j++)	{
						int insIndex = insContent.indexOf(delContent.get(j));
					
						if (insIndex > -1) {
							int containLength = 1;
							
							for (int k = 1; j + k < delContent.size() && k + insIndex < insContent.size(); k++)	{
								if (delContent.get(j+k).equals(insContent.get(insIndex+k))) {
									containLength++;
								}
								else {
									break;
								}
							}
							
							if (containLength > maxLength) {
								List<Token> nm = insContent.subList(insIndex, insIndex + containLength);
								boolean onlyStopWords = containsOnlyStopWords(nm);
								
								if (!onlyStopWords || containLength > 1 || 
									 j == 0 || insIndex == 0 ||
									 j == delContent.size() - 1 ||
									 insIndex == insContent.size() - 1) {
									maxDelIndex = j;
									maxInsIndex = insIndex;
									maxLength = containLength;
								}
							}
						}
					}
				}
				
				if (maxLength > 0) {
					List<Token> om = delContent.subList(maxDelIndex, maxDelIndex + maxLength);
					List<Token> nm = insContent.subList(maxInsIndex, maxInsIndex + maxLength);
					matchesList.add(new TokenMatch(maxLength, om, nm));
					
					List<Token> d1 = delContent.subList(0, maxDelIndex);
					List<Token> d2 = delContent.subList(maxDelIndex + maxLength, delContent.size());
					List<Token> i1 = insContent.subList(0, maxInsIndex);
					List<Token> i2 = insContent.subList(maxInsIndex + maxLength, insContent.size());
					
					if (i1.size() > 0 && d1.size() == 0) {
						tokenEdits.add(new TokenInsert(i1));
					}
					else if (i1.size() > 0 && d1.size() > 0) {
						TokenReplace newRepl = new TokenReplace(d1, i1);
						
						replIter.add(newRepl);
						replIter.previous();
						tokenEdits.add(newRepl);
					}
					else if (i1.size() == 0 && d1.size() > 0) {
						tokenEdits.add(new TokenDelete(d1));
					}
					
					if (i2.size() > 0 && d2.size() == 0) {
						tokenEdits.add(new TokenInsert(i2));
					}
					else if (i2.size() > 0 && d2.size() > 0) {
						TokenReplace newRepl = new TokenReplace(d2, i2);
						
						replIter.add(newRepl);
						replIter.previous();
						tokenEdits.add(newRepl);
					}
					else if (i2.size() == 0 && d2.size() > 0) {
						tokenEdits.add(new TokenDelete(d2));
					}
					
					tokenEdits.remove(r);
				}
			}
		}
	}
	
	private static void shiftMatchBoundary(List<Match> tokenMatches, List<Token> oldTokens, List<Token> newTokens, int[] matchedOld, int[] matchedNew) {
		Collections.sort(tokenMatches, new Comparator<Match>() {
			public int compare(Match arg0, Match arg1) {
				return arg0.getOldIndex() - arg1.getOldIndex();
			}
		});
		for(int i = 0; i < tokenMatches.size() - 1; i++) {
			Match m1 = tokenMatches.get(i);
			Match m2 = tokenMatches.get(i+1);
			
			doShiftMatchBoundary(m1, m2, oldTokens, newTokens, matchedOld, matchedNew);
		}
		
		Collections.sort(tokenMatches, new Comparator<Match>() {
			public int compare(Match arg0, Match arg1) {
				return arg0.getNewIndex() - arg1.getNewIndex();
			}
		});
		for(int i = 0; i < tokenMatches.size() - 1; i++) {
			Match m1 = tokenMatches.get(i);
			Match m2 = tokenMatches.get(i+1);
			
			doShiftMatchBoundary(m1, m2, oldTokens, newTokens, matchedOld, matchedNew);
		}
	}
	
	private static void doShiftMatchBoundary(Match m1, Match m2, List<Token> oldTokens, List<Token> newTokens, int[] matchedOld, int[] matchedNew) {
			if (m1.getOldIndex() + m1.getLength() >= oldTokens.size() || 
				m1.getNewIndex() + m1.getLength() >= newTokens.size() || 
				m2.getOldIndex() - 1 < 0 || 
				m2.getNewIndex() - 1 < 0) {
				return;
			}
			
			Token m1Last = oldTokens.get(m1.getOldIndex() + m1.getLength() - 1);
			Token m1AfterLastOld = oldTokens.get(m1.getOldIndex() + m1.getLength());
			Token m1AfterLastNew = newTokens.get(m1.getNewIndex() + m1.getLength());
			
			Token m2First = oldTokens.get(m2.getOldIndex());
			Token m2BeforeFirstOld = oldTokens.get(m2.getOldIndex() - 1);
			Token m2BeforeFirstNew = newTokens.get(m2.getNewIndex() - 1);
			
			if (m1Last.equals(m2BeforeFirstOld) && 
				m2BeforeFirstOld.equals(m2BeforeFirstNew) &&
				openMarkupSet.contains(m1Last.getContent()) ) {
				matchedOld[m1.getOldIndex() + m1.getLength() - 1] = matchedOld[m1.getOldIndex() + m1.getLength()];
				matchedNew[m1.getNewIndex() + m1.getLength() - 1] = matchedNew[m1.getNewIndex() + m1.getLength()];
				matchedOld[m2.getOldIndex() - 1] = matchedOld[m2.getOldIndex()];
				matchedNew[m2.getNewIndex() - 1] = matchedNew[m2.getNewIndex()];
				m1.shrink();
				m2.shiftUp();
			}
			else if (m2First.equals(m1AfterLastOld) && 
					m1AfterLastOld.equals(m1AfterLastNew) &&
					closeMarkupSet.contains(m2First.getContent()) ) {
				matchedOld[m1.getOldIndex() + m1.getLength()] = matchedOld[m1.getOldIndex() + m1.getLength() - 1];
				matchedNew[m1.getNewIndex() + m1.getLength()] = matchedNew[m1.getNewIndex() + m1.getLength() - 1];
				matchedOld[m2.getOldIndex()] = matchedOld[m2.getOldIndex() - 1];
				matchedNew[m2.getNewIndex()] = matchedNew[m2.getNewIndex() - 1];
				m1.expand();
				m2.shiftDown();
			}

	}
	
	private static void labelMovement(List<TokenMatch> matchesList, int[] matchedOld, int[] matchedNew) {
		labelMovement(matchesList, matchedOld, matchedNew, true);
	}
	
	private static void labelMovement(List<TokenMatch> matchesList, int[] matchedOld, int[] matchedNew, boolean countWord) {
		LinkedList<OrderedTokenMatch> orderedMatch = new LinkedList<OrderedTokenMatch>();
		for (TokenMatch tm : matchesList) {
			orderedMatch.add(new OrderedTokenMatch(tm));
		}
		
		Collections.sort(orderedMatch, new Comparator<OrderedTokenMatch>() {
			public int compare(OrderedTokenMatch o1, OrderedTokenMatch o2) {
				return o1.getOldIndex() - o2.getOldIndex();
			}
		});
		
		ListIterator<OrderedTokenMatch> li = orderedMatch.listIterator();
		for (int order = 0; li.hasNext();) {
			OrderedTokenMatch m = li.next();
			m.setOldMatchOrder(order);
			order += m.getFullLength();
		}
		
		Collections.sort(orderedMatch, new Comparator<OrderedTokenMatch>() {
			public int compare(OrderedTokenMatch o1, OrderedTokenMatch o2) {
				return o1.getNewIndex() - o2.getNewIndex();
			}
		});
		
		li = orderedMatch.listIterator();
		for (int order = 0; li.hasNext();)
		{
			OrderedTokenMatch m = li.next();
			m.setNewMatchOrder(order);
			order += m.getFullLength();
		}
		
		boolean sorted;
		do {
			sorted = true;
			li = orderedMatch.listIterator();
			for (int oldOrder = -1; li.hasNext();)	{
				int tmp = li.next().getOldMatchOrder();
				if (oldOrder >= tmp)
				{
					sorted = false;
					break;
				}
				else
					oldOrder = tmp;
			}
	
			if (sorted) break;
			
			int[] distance = new int[orderedMatch.size()];
			int maxDistance = 0, maxPos = 0;
			li = orderedMatch.listIterator();
			for (int i = 0; li.hasNext(); i++)	{
				OrderedTokenMatch m = li.next();
				distance[i] = Math.abs(m.getOldMatchOrder() - m.getNewMatchOrder());
				if (maxDistance < distance[i]) {
					maxDistance = distance[i];
					maxPos = i;
				}
			}
			
			OrderedTokenMatch movedMatch = orderedMatch.get(maxPos);
			li = orderedMatch.listIterator();
			while (li.hasNext()) {
				OrderedTokenMatch m = li.next();
				
				if (m.getOldMatchOrder() > movedMatch.getOldMatchOrder()) {
					m.setOldMatchOrder(m.getOldMatchOrder() - movedMatch.getFullLength());
				}
				
				if (m.getNewMatchOrder() > movedMatch.getNewMatchOrder()) {
					m.setNewMatchOrder(m.getNewMatchOrder() - movedMatch.getFullLength());
				}
			}

			int wordCount = 0;
			if (countWord) {
				for (int i = 0; i < movedMatch.getLength(); i++) {
					String tokenContent = movedMatch.getOldTokens().get(i).getContent();
					if (StringUtils.isAlphanumeric(tokenContent) && !StopWords.isStopWord(tokenContent)) {
						wordCount++;
					}
				}
			}
			
			if (!countWord || wordCount > 0) {
				movedMatch.getTokenMatch().labelAsMoved();
			}
			else if (!(movedMatch.getOldTokens().get(0) instanceof MatchedSentence)) {
				int oldIndex = movedMatch.getOldIndex();
				int newIndex = movedMatch.getNewIndex();
				for (int i = 0; i < movedMatch.getLength(); i++) {
					matchedOld[oldIndex + i] = 0;
					matchedNew[newIndex + i] = 0;
				}
				matchesList.remove(movedMatch.getTokenMatch());
			}
			
			orderedMatch.remove(movedMatch);
			
		} while (!sorted);

	}
	
	private static boolean containsOnlyStopWords(List<Token> tokenList) {
		for (Token tok : tokenList) {
			if (!StopWords.isStopWord(tok.getContent()) && 
				!tok.getContent().matches("[\\p{IsP}\\p{Punct}]")) {
				return false;
			}
		}
		
		return true;
	}
}
