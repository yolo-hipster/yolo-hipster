package mo.umac.weha.summarizer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.Gson;

import mo.umac.weha.categorizer.AbstractEditAction;
import mo.umac.weha.categorizer.EditActionCategorizer;
import mo.umac.weha.data.Paragraph;
import mo.umac.weha.diff.paragraph.ParagraphDiff;
import mo.umac.weha.diff.paragraph.ParagraphEdit;
import mo.umac.weha.lexer.ParagraphSplitter;

public class EditSignificanceCalculator {
	private List<AbstractEditAction> actionList;
	private Map<String, List<AbstractEditAction>> actionMap;
	private Map<String, Integer> rawCount;

	public EditSignificanceCalculator(List<AbstractEditAction> actionList) {
		this.actionList = actionList;
		this.actionMap = new TreeMap<String, List<AbstractEditAction>>();
		this.rawCount = new TreeMap<String, Integer>();
	}
	
	public Map<String, Double> outputStatistic() {
		Map<String, Double> resultMap = new TreeMap<String, Double>();

		if (rawCount.isEmpty()) {
			this.outputRawCount();
		}

		Set<String> keySet = rawCount.keySet();
		Iterator<String> iter = keySet.iterator();

		while (iter.hasNext()) {
			String key = iter.next();
			AbstractEditAction ae;
			try {
				ae = (AbstractEditAction) Class.forName(
						"mo.umac.weha.categorizer." + key).newInstance();
				resultMap.put(key, Double.valueOf(ae.getWeight() * rawCount.get(key).doubleValue()));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		return resultMap;
	}

	public Map<String, Integer> outputRawCount() {
		if (rawCount.isEmpty()) {
			for (int i = 0; i < actionList.size(); i++) {
				AbstractEditAction ea = actionList.get(i);
				String actionName = ea.getClass().getSimpleName();

				if (rawCount.containsKey(actionName)) {
					int preValue = rawCount.get(actionName).intValue();
					rawCount.put(actionName, Integer.valueOf(preValue + ea.lengthCount()));
				} else {
					rawCount.put(actionName, Integer.valueOf(ea.lengthCount()));
				}
				
				List<AbstractEditAction> preList;
				if (actionMap.containsKey(actionName)) {
					preList = actionMap.get(actionName);
				} else {
					preList = new ArrayList<AbstractEditAction>();
				}
				preList.add(ea);
				actionMap.put(actionName, preList);
			}
		}

		return rawCount;
	}
	
	public double calculateSignificance() {
		double retVal = 0.0;

		if (rawCount.isEmpty()) {
			this.outputRawCount();
		}

		Set<String> keySet = rawCount.keySet();
		Iterator<String> iter = keySet.iterator();

		while (iter.hasNext()) {
			String key = iter.next();
			AbstractEditAction ae;
			try {
				ae = (AbstractEditAction) Class.forName(
						"mo.umac.weha.categorizer." + key).newInstance();
				retVal += ae.getWeight() * rawCount.get(key).doubleValue();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		return retVal;
	}
	
	public Map<String, List<AbstractEditAction>> getActionMap() {
		if (rawCount.isEmpty()) {
			this.outputRawCount();
		}
		
		return actionMap;
	}
	
	public static void main(String[] args) {
		String oldFilename = null;
		String newFilename = null;
		
		if (args.length == 2) {
			oldFilename = args[0];
			newFilename = args[1];
		}
		else {
			System.err.println("Usage: EditSignificanceCalculator oldText.txt newText.txt");
			System.exit(1);
		}
		
		BufferedReader oldReader = null;
		BufferedReader newReader = null;
		try {
			oldReader = new BufferedReader( new InputStreamReader(
					new FileInputStream(oldFilename), "UTF8") );
			newReader = new BufferedReader( new InputStreamReader(
					new FileInputStream(newFilename), "UTF8")  );
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		String tmp;
		StringBuilder oldString = new StringBuilder();
		StringBuilder newString = new StringBuilder();
		
		try {
			while ( (tmp = oldReader.readLine()) != null) {
				oldString.append(tmp);
				oldString.append("\n");
			}
			while ( (tmp = newReader.readLine()) != null) {
				newString.append(tmp);
				newString.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		List<Paragraph> oldParas = ParagraphSplitter.split(oldString.toString());
		List<Paragraph> newParas = ParagraphSplitter.split(newString.toString());
		
		List<ParagraphEdit> paraDiff = ParagraphDiff.diff(oldParas, newParas);		
		
		List<AbstractEditAction> actionList = EditActionCategorizer.categorizeParagraphEdits(paraDiff);
		
		EditSignificanceCalculator esCalc = new EditSignificanceCalculator(actionList);
		String diffOutput = EditSignificanceFormatter.formatAction(esCalc.getActionMap(), esCalc.outputStatistic(), paraDiff);
		
		EditSignificanceResult result = new EditSignificanceResult(esCalc, diffOutput);
		
		Gson gson = new Gson();
		System.out.println(gson.toJson(result));
	}
}
