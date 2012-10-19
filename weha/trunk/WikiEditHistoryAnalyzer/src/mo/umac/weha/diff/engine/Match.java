package mo.umac.weha.diff.engine;

public class Match {
	private int length;
	private int oldIndex;
	private int newIndex;
	
	public Match(int length, int oldIndex, int newIndex) {
		this.length = length;
		this.oldIndex = oldIndex;
		this.newIndex = newIndex;
	}

	public int getLength() {
		return this.length;
	}

	public int getOldIndex() {
		return this.oldIndex;
	}

	public int getNewIndex() {
		return this.newIndex;
	}

	@Override
	public String toString() {
		return "Match [length=" + length
			+ ", oldTokens=" + oldIndex
			+ ", newTokens=" + newIndex
			+ "]\n";
	}

	public void shrink() {
		this.length--;
	}
	
	public void expand() {
		this.length++;
	}

	public void shiftUp() {
		this.oldIndex--;
		this.newIndex--;
		this.length++;
	}

	public void shiftDown() {
		this.oldIndex++;
		this.newIndex++;
		this.length--;
	}
	
}
