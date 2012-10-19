package mo.umac.weha.diff;

public interface AbstractEdit {
	
	public int getOldPos();
	public int getNewPos();
	
	public int getMatchingLength();
	public double getMatchingRate();
	
	public void labelAsMoved();
	public boolean isMovement();
}
