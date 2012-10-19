package mo.umac.weha.diff.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import mo.umac.weha.diff.MatchedUnit;

public class DiffEngine<T> {
	private final int maxMatches;
	private final int tupleSize;
	
	public DiffEngine(int tupleSize) {
		super();
		this.maxMatches = 40;
		this.tupleSize = tupleSize;
	}
	
	public MatchInfo doDiff(List<T> oldObjects, List<T> newObjects) {
		List<Match> matchesList = new ArrayList<Match>();	
		
		Hashtable<Tuple<T>,ArrayList<Integer>> indexNew;
		PriorityQueue<Match> matches;
		
		indexNew = new Hashtable<Tuple<T>,ArrayList<Integer>>();
		matches = new PriorityQueue<Match>(11, new Comparator<Match>() {
			public int compare(Match o1, Match o2) {
				int diff = o2.getLength() - o1.getLength();
				if (diff != 0)
					return diff;
				else
				{
					diff = o1.getOldIndex() - o2.getOldIndex();
					if (diff != 0)
						return diff;
					else
						return Math.abs(o1.getNewIndex() - o1.getOldIndex()) - Math.abs(o2.getNewIndex() - o2.getOldIndex()) ;
				}
			}
		});
		
		// Skip the beginning matches and ending matches to reduce complexity.
		int beginningSkip;
		for (beginningSkip = 0; beginningSkip < oldObjects.size() && beginningSkip < newObjects.size(); beginningSkip++) {
			if (!oldObjects.get(beginningSkip).equals(newObjects.get(beginningSkip)))
				break;
		}
		if (beginningSkip > 0) {
			matches.add(new Match(beginningSkip, 0, 0));
		}
		
		int endingSkip;
		for (endingSkip = 0; oldObjects.size() - endingSkip > 0 && newObjects.size() - endingSkip > 0; endingSkip++) {
			if (!oldObjects.get(oldObjects.size() - endingSkip - 1).equals(newObjects.get(newObjects.size() - endingSkip - 1)))
				break;
		}
		if (endingSkip > 0) {
			matches.add(new Match(endingSkip, oldObjects.size() - endingSkip, newObjects.size() - endingSkip));
		}
		
		// Mark matched higher level unit
		for (int i = 0; i < oldObjects.size(); i++) {
			T oldT = oldObjects.get(i);
			if (oldT instanceof MatchedUnit) {
				MatchedUnit oldMu = (MatchedUnit) oldT;
				
				for (int j = 0; j < newObjects.size(); j++) {
					T newT = newObjects.get(j);
					if (newT instanceof MatchedUnit) {
						MatchedUnit newMu = (MatchedUnit) newT;
						
						if (oldMu.getMatchId() == newMu.getMatchId() && 
							oldObjects.get(i).equals(newObjects.get(j))) {
							int len = 1;
							while ( i + len < oldObjects.size() &&
									j + len < newObjects.size() &&
									oldObjects.get(i + len).equals(newObjects.get(j + len))) {
								len++;
							}
							matches.add(new Match(len, i, j));
							break;
						}
					}
				}
				
			}
		}
		
		// Calculate the hash table for every tuple of new version token.
		for (int i = beginningSkip; i <= newObjects.size() - endingSkip - tupleSize; i++)
		{
			Tuple<T> t = new Tuple<T>(newObjects, i, tupleSize);
			
			ArrayList<Integer> newVal;
			if (indexNew.containsKey(t)) {
				newVal = indexNew.get(t);
			}
			else {
				newVal = new ArrayList<Integer>();
			}
			
			newVal.add(new Integer(i));
			indexNew.put(t, newVal);
		}
		
		// Find the match of token in old version.
		for (int i = beginningSkip; i <= oldObjects.size() - endingSkip - tupleSize; i++)
		{
			Tuple<T> t = new Tuple<T>(oldObjects, i, tupleSize);
			if (indexNew.containsKey(t))
			{
				List<Integer> index = indexNew.get(t);
				if (index.size() > maxMatches)
					indexNew.remove(t);
				else
				{
					Iterator<Integer> iter = index.iterator();
					while (iter.hasNext())
					{
						int j = iter.next().intValue();
						int len = tupleSize;
						while ( i + len < oldObjects.size() &&
								j + len < newObjects.size() &&
								oldObjects.get(i + len).equals(newObjects.get(j + len)))
							len++;
						matches.add(new Match(len, i, j));
					}
				}
			}
		}
		
		int matchId = 0;
		int[] matchedOld = new int[oldObjects.size()];
		int[] matchedNew = new int[newObjects.size()];
		
		while (!matches.isEmpty())
		{
			matchId++;
			Match m = matches.poll();
			
			if (matchedOld[m.getOldIndex()] == 0 &&
				matchedNew[m.getNewIndex()] == 0)
			{
				if (matchedOld[m.getOldIndex() + m.getLength() - 1] == 0 &&
					matchedNew[m.getNewIndex() + m.getLength() - 1] == 0)
				{
					matchesList.add(m);
					for (int i = 0; i < m.getLength(); i++)
					{
						matchedOld[m.getOldIndex() + i] = matchId;
						matchedNew[m.getNewIndex() + i] = matchId;
						
					}
				}
				else
				{
					int k = m.getLength() - 1;
					while (matchedOld[m.getOldIndex() + k] != 0 ||
						   matchedNew[m.getNewIndex() + k] != 0)
						k--;
					
					int residualLen = k + 1;
					if (residualLen > 1)
						matches.add(new Match(residualLen,
										m.getOldIndex(), m.getNewIndex()));
				}
			}
			else
			{
				if (matchedOld[m.getOldIndex() + m.getLength() - 1] == 0 &&
					matchedNew[m.getNewIndex() + m.getLength() - 1] == 0)
				{
					int j = 1;
					while (matchedOld[m.getOldIndex() + j] != 0 ||
							matchedNew[m.getNewIndex() + j] != 0)
						j++;

					int residualLen = m.getLength() - j;
					if (residualLen > 1)
						matches.add(new Match(residualLen,
										m.getOldIndex() + j, m.getNewIndex() + j));
				}
				else
				{
					int j = 1;
					while (j < m.getLength() - 1 &&
						   (matchedOld[m.getOldIndex() + j] != 0 ||
							matchedNew[m.getNewIndex() + j] != 0))
						j++;

					int k = j + 1;
					while (k < m.getLength() - 1 &&
						   !(matchedOld[m.getOldIndex() + k] != 0 ||
						   matchedNew[m.getNewIndex() + k] != 0))
						k++;

					int residualLen = k - j;
					if (residualLen > 1)
						matches.add(new Match(residualLen,
										m.getOldIndex() + j, m.getNewIndex() + j));
				}
			}
		}

		Match[] matchesArray = new Match[0];
		matchesArray = matchesList.toArray(matchesArray);
		return new MatchInfo(matchedOld, matchedNew, matchesArray);
	}
	
}
