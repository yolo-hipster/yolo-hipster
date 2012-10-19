package mo.umac.weha.categorizer;

import java.util.Arrays;
import java.util.List;

import mo.umac.weha.diff.AbstractEdit;

public abstract class AbstractEditAction {
	protected AbstractEdit[] basicEdits;
	protected double weight;
	
	public abstract List<AbstractEditAction> classify(List<AbstractEdit> diffList);
	public abstract int findAction(AbstractEdit edit);
	
	public AbstractEdit[] getBasicEdits() {
		return basicEdits;
	}
	
	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append(this.getClass().getSimpleName());
		builder.append(" [");
		if (basicEdits != null)
			builder.append("basicEdits=").append(
					Arrays.asList(basicEdits).subList(0,
							Math.min(basicEdits.length, maxLen)));
		builder.append("]\n");
		return builder.toString();
	}
	
	public double getWeight() {
		return this.weight;
	}
	
	public abstract int lengthCount();
}
