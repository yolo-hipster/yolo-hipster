package mo.umac.weha.util.array;

import java.util.Iterator;

public class Sparse2DArrayRowIterator implements Iterator<Sparse2DCell> {

	private Sparse2DCell cell;
	
	public Sparse2DArrayRowIterator(Sparse2DCell sparse2dCell) {
		this.cell = sparse2dCell;
	}

	public boolean hasNext() {
		return (this.cell != null);
	}

	public Sparse2DCell next() {
		Sparse2DCell retCell = this.cell;
		this.cell = this.cell.nextRow();
		return retCell;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
