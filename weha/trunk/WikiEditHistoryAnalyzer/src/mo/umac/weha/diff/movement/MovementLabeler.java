package mo.umac.weha.diff.movement;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import mo.umac.weha.diff.AbstractEdit;

public class MovementLabeler<T extends AbstractEdit> {
	
	public void labelMovement(List<T> editList) {
		LinkedList< OrderedEdit<T> > orderedMatchList = new LinkedList< OrderedEdit<T> >();
		for (T edit : editList) {
			if (edit.getMatchingRate() > 0.0) {
				orderedMatchList.add(new OrderedEdit<T>(edit));
			}
		}
		
		Collections.sort(orderedMatchList, new Comparator< OrderedEdit<T> >() {
			public int compare(OrderedEdit<T> arg0, OrderedEdit<T> arg1) {
				int oldDistance = arg0.getOldPos() - arg1.getOldPos();
				if (oldDistance != 0) {
					return oldDistance;
				}
				return arg0.getNewPos() - arg1.getNewPos();
			}
		});
		
		ListIterator< OrderedEdit<T> > li = orderedMatchList.listIterator();
		for (int order = 0; li.hasNext();) {
			OrderedEdit<T>  m = li.next();
			m.setOldMatchOrder(order);
			order += m.getMatchingLength();
		}

		Collections.sort(orderedMatchList, new Comparator< OrderedEdit<T> >() {
			public int compare(OrderedEdit<T> arg0, OrderedEdit<T> arg1) {
				int newDistance = arg0.getNewPos() - arg1.getNewPos();
				if (newDistance != 0) {
					return newDistance;
				}
				return arg0.getOldPos() - arg1.getOldPos();
			}
		});
		
		li = orderedMatchList.listIterator();
		for (int order = 0; li.hasNext();)
		{
			OrderedEdit<T>  m = li.next();
			m.setNewMatchOrder(order);
			order += m.getMatchingLength();
		}
		
		boolean sorted;
		do {
			sorted = true;
			li = orderedMatchList.listIterator();
			for (int oldOrder = -1; li.hasNext();)	{
				int tmp = li.next().getOldMatchOrder();
				if (oldOrder > tmp) {
					sorted = false;
					break;
				}
				else {
					oldOrder = tmp;
				}
			}
			
			if (sorted) break;
			
			int[] distance = new int[orderedMatchList.size()];
			int maxDistance = 0, maxPos = 0;
			li = orderedMatchList.listIterator();
			for (int i = 0; li.hasNext(); i++)	{
				OrderedEdit<T>  m = li.next();
				distance[i] = Math.abs(m.getOldMatchOrder() - m.getNewMatchOrder());
				if (maxDistance < distance[i]) {
					maxDistance = distance[i];
					maxPos = i;
				}
			}
			
			OrderedEdit<T> movedMatch = orderedMatchList.get(maxPos);
			li = orderedMatchList.listIterator();
			while (li.hasNext()) {
				OrderedEdit<T>  m = li.next();
				
				if (m.getOldMatchOrder() > movedMatch.getOldMatchOrder()) {
					m.setOldMatchOrder(m.getOldMatchOrder() - movedMatch.getMatchingLength());
				}
				
				if (m.getNewMatchOrder() > movedMatch.getNewMatchOrder()) {
					m.setNewMatchOrder(m.getNewMatchOrder() - movedMatch.getMatchingLength());
				}
			}
			
			movedMatch.getEdit().labelAsMoved();
			orderedMatchList.remove(movedMatch);
					
		} while (!sorted);
	}
	
}
