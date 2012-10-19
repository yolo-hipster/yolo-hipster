package mo.umac.weha.lexer;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.data.Sentence;

public class WikiTableSplitter {
	
	private static final Pattern blockOpener = Pattern.compile("\\{\\|");
	private static final Pattern blockCloser = Pattern.compile("\\|\\}");
	
	private static final Pattern lineTerminator = Pattern.compile("^\\|[+-]|\\|}", Pattern.MULTILINE);
	
	public static List<Sentence> separateTable(int lastPos, String templateText, Paragraph para) {
		LinkedList<Sentence> sentences = new LinkedList<Sentence>();

		Matcher boMatcher = blockOpener.matcher(templateText);
		Matcher bcMatcher = blockCloser.matcher(templateText);
		
		Stack<String> markupMatchStack = new Stack<String>();
		
		Matcher ltMatcher = lineTerminator.matcher(templateText);
		
		int lastMatch = 0;
		int seekPos = 2;
		while (seekPos < templateText.length()) {
			boolean boFound = boMatcher.find(seekPos);
			boolean bcFound = markupMatchStack.empty() ? false : bcMatcher.find(seekPos);
			boolean ltFound = markupMatchStack.empty() ? ltMatcher.find(seekPos) : false;
			int skipPos, sentPos;
			
			skipPos = boFound ? boMatcher.start() : Integer.MAX_VALUE;
			skipPos = bcFound ? Math.min(skipPos, bcMatcher.start()) : skipPos;
			
			sentPos = ltFound ? ltMatcher.start() : Integer.MAX_VALUE;
			
			if (!markupMatchStack.empty() || skipPos <= sentPos) {
				if (boFound && skipPos == boMatcher.start()) {
					markupMatchStack.push(boMatcher.group());
					seekPos = boMatcher.end();
				}
				else if (bcFound) {
					markupMatchStack.pop();
					seekPos = bcMatcher.end();
				}
				else {
					break;
				}
			}
			else {
				if (ltFound && lastMatch < ltMatcher.start()){
					String content = templateText.substring(lastMatch, ltMatcher.start());
					
					Sentence s = new Sentence(lastPos + lastMatch, content, para);
					sentences.add(s);
					
					lastMatch = ltMatcher.start();
					seekPos = lastMatch + 2;
				}
				else {
					break;
				}
			}
		}
		
		if (lastMatch < templateText.length()) {
			String content = templateText.substring(lastMatch, templateText.length());
			
			Sentence s = new Sentence(lastPos + lastMatch, content, para);
			sentences.add(s);
			
			seekPos = templateText.length();
			lastMatch = seekPos;
		}
		
		return sentences;
	}
	
}
