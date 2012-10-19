package mo.umac.weha.diff.movement;

import mo.umac.weha.diff.AbstractEdit;

public class OrderedEdit<T extends AbstractEdit> {
	
	private T edit;

	private int oldMatchOrder;
	private int newMatchOrder;
	
	public OrderedEdit(T edit) {
		this.edit = edit;
	}

	public T getEdit() {
		return edit;
	}
	
	public int getOldPos() {
		return edit.getOldPos();
	}

	public int getNewPos() {
		return edit.getNewPos();
	}

	public int getMatchingLength() {
		return edit.getMatchingLength();
	}
	
	public int getOldMatchOrder() {
		return oldMatchOrder;
	}

	public void setOldMatchOrder(int oldMatchOrder) {
		this.oldMatchOrder = oldMatchOrder;
	}

	public int getNewMatchOrder() {
		return newMatchOrder;
	}

	public void setNewMatchOrder(int newMatchOrder) {
		this.newMatchOrder = newMatchOrder;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("OrderedEdit [");
		if (edit != null)
			builder.append("edit=").append(edit).append(", ");
		builder.append("newMatchOrder=").append(newMatchOrder).append(
				", oldMatchOrder=").append(oldMatchOrder).append("]");
		return builder.toString();
	}
	
}
