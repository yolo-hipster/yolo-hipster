package mo.umac.weha.summarizer;

import java.util.Map;

public class EditSignificanceResult {
	private Map<String, Integer> rawCount;
	private Map<String, Double> statMap;
	private double significance;
	private String diffOutput;
	
	public EditSignificanceResult(EditSignificanceCalculator esCalc,
			String diffOut) {
		this.rawCount = esCalc.outputRawCount();
		this.statMap = esCalc.outputStatistic();
		this.significance = esCalc.calculateSignificance();
		this.diffOutput = diffOut;
	}
	
	public Map<String, Integer> getRawCount() {
		return rawCount;
	}
	
	public Map<String, Double> getStatMap() {
		return statMap;
	}
	
	public String getDiffOutput() {
		return diffOutput;
	}
	
	public double getSignificance() {
		return significance;
	}
}
