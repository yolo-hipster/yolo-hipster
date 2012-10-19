package mo.umac.weha.categorizer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.diff.AbstractEdit;
import mo.umac.weha.diff.paragraph.ParagraphDiff;
import mo.umac.weha.diff.paragraph.ParagraphEdit;
import mo.umac.weha.diff.sentence.SentenceEdit;
import mo.umac.weha.diff.token.TokenEdit;
import mo.umac.weha.lexer.ParagraphSplitter;

public class EditActionCategorizer {
	private static List<AbstractEditAction> paraActions;
	private static List<AbstractEditAction> sentActions;
	private static List<AbstractEditAction> tokActions;
	
	static {
		paraActions = new ArrayList<AbstractEditAction>();
		paraActions.add(new Uncategorized());

		sentActions = new ArrayList<AbstractEditAction>();
		sentActions.add(new AssignCategory());
		sentActions.add(new UnassignCategory());
		sentActions.add(new NewReference());
		sentActions.add(new Interwiki());
		sentActions.add(new ImageAddition());
		sentActions.add(new ImageRemoval());
		sentActions.add(new ContentRemoval());
		sentActions.add(new ContentAddition());
		sentActions.add(new ContentMovement());
		sentActions.add(new Uncategorized());
		
		tokActions = new ArrayList<AbstractEditAction>();
		tokActions.add(new AssignCategory());
		tokActions.add(new UnassignCategory());
		tokActions.add(new EditorialNotice());
		tokActions.add(new CiteExistingReference());
		tokActions.add(new NewReference());
		tokActions.add(new Interwiki());
		tokActions.add(new Wikify());
		tokActions.add(new Dewikify());
		tokActions.add(new PunctuationChange());
		tokActions.add(new TypoChange());
		tokActions.add(new ImageAddition());
		tokActions.add(new ImageRemoval());
		tokActions.add(new ContentRemoval());
		tokActions.add(new ContentAddition());
		tokActions.add(new ContentSubstitution());
		tokActions.add(new ContentMovement());
		tokActions.add(new Uncategorized());
	}
	
	public static List<AbstractEditAction> categorizeParagraphEdits(List<ParagraphEdit> paraDiff) {
		ArrayList<AbstractEdit> paraDiffList = new ArrayList<AbstractEdit>();
		ArrayList<SentenceEdit> sentDiffList = new ArrayList<SentenceEdit>();
		ArrayList<AbstractEditAction> ret = new ArrayList<AbstractEditAction>();
		
		paraDiffList.addAll(paraDiff);
		
		for (AbstractEditAction ae : paraActions) {
			try {
				ret.addAll(ae.classify(paraDiffList));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		
		for (ListIterator<AbstractEditAction> retIter = ret.listIterator(); retIter.hasNext(); ) {
			AbstractEditAction ea = retIter.next();
			if (ea instanceof Uncategorized) {
				AbstractEdit[] aeArray = ea.getBasicEdits();
				ParagraphEdit pe = (ParagraphEdit) aeArray[0];
				sentDiffList.addAll(pe.getSentenceEdits());
				retIter.remove();
			}
		}
		
		ret.addAll(categorizeSentenceEdits(sentDiffList));
		
		return ret;
	}
	
	private static List<AbstractEditAction> categorizeSentenceEdits(List<SentenceEdit> sentDiff) {
		ArrayList<AbstractEdit> sentDiffList = new ArrayList<AbstractEdit>();
		HashSet<TokenEdit> tokDiffSet = new HashSet<TokenEdit>();
		LinkedList<TokenEdit> tokDiffList = new LinkedList<TokenEdit>();
		ArrayList<AbstractEditAction> ret = new ArrayList<AbstractEditAction>();
		
		sentDiffList.addAll(sentDiff);
		
		for (AbstractEditAction ae : sentActions) {
			try {
				ret.addAll(ae.classify(sentDiffList));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		
		for (ListIterator<AbstractEditAction> retIter = ret.listIterator(); retIter.hasNext(); ) {
			AbstractEditAction ea = retIter.next();
			if (ea instanceof Uncategorized) {
				AbstractEdit[] aeArray = ea.getBasicEdits();
				SentenceEdit se = (SentenceEdit) aeArray[0];
				tokDiffSet.addAll(se.getTokenEdits());
				retIter.remove();
			}
		}
		
		tokDiffList.addAll(tokDiffSet);
		ret.addAll(categorizeTokenEdits(tokDiffList));

		return ret;
	}

	private static List<AbstractEditAction> categorizeTokenEdits(List<TokenEdit> tokDiff) {
		ArrayList<AbstractEdit> tokDiffList = new ArrayList<AbstractEdit>();
		ArrayList<AbstractEditAction> ret = new ArrayList<AbstractEditAction>();
		
		tokDiffList.addAll(tokDiff);
		
		for (AbstractEditAction ae : tokActions) {
			try {
				ret.addAll(ae.classify(tokDiffList));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		
		return ret;
	}

	public static void main(String[] args) {
		String oldFilename = null;
		String newFilename = null;
		
		if (args.length == 2) {
			oldFilename = args[0];
			newFilename = args[1];
		}
		else {
			System.err.println("Usage: EditActionCategorizer oldText.txt newText.txt");
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
		
		System.out.println(EditActionFormatter.formatAction(actionList, paraDiff));
	}
}
