package mo.umac.weha.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mo.umac.weha.data.Paragraph;

public class ParagraphSplitter {
	
	private static final Pattern blockOpener = Pattern.compile("(<ref[^/]*?>|<gallery[^/]*?>|\\[\\[(Image|File):|\\{\\{|\\{\\|)", Pattern.CASE_INSENSITIVE);
	private static final Pattern blockOpenerWithIntLink = Pattern.compile("(<ref[^/]*?>|<gallery[^/]*?>|\\[\\[(Image|File):|\\[\\[|\\{\\{|\\{\\|)", Pattern.CASE_INSENSITIVE);
	private static final Pattern blockCloser = Pattern.compile("(</ref>|</gallery>|\\|\\}\\}|\\}\\}|\\|\\})", Pattern.CASE_INSENSITIVE);
	private static final Pattern blockCloserWithIntLink = Pattern.compile("(</ref>|</gallery>|\\]\\]|\\|\\}\\}|\\}\\}|\\|\\})", Pattern.CASE_INSENSITIVE);

	private static final Pattern oneLineBlock = Pattern.compile("^(=+.+=+|----+)(\\s*\n+)", Pattern.MULTILINE);
	private static final Pattern paragraphBreak = Pattern.compile("\n\n+|\\z");
	
	private static final Map<String, String> blockMarkupMap = new HashMap<String, String>();
	static {
		blockMarkupMap.put("<r", "</ref>");
		blockMarkupMap.put("<g", "</gallery>");
		blockMarkupMap.put("[[", "]]");
		blockMarkupMap.put("{{", "}}");
		blockMarkupMap.put("{|", "|}");
	}
	
	public static List<Paragraph> split(String text) {
		ArrayList<Paragraph> paraList = new ArrayList<Paragraph>();
		
		Matcher om = oneLineBlock.matcher(text);
		Matcher pm = paragraphBreak.matcher(text);
		
		Matcher boMatcher = blockOpener.matcher(text);
		Matcher bcMatcher = blockCloser.matcher(text);
		
		LinkedList<MatchResult> markupMatchStack = new LinkedList<MatchResult>();

		int lastMatch = 0;
		int seekPos = 0;
		while (seekPos < text.length()) {
			boolean boFound = boMatcher.find(seekPos);
			boolean bcFound = markupMatchStack.isEmpty() ? false : bcMatcher.find(seekPos);
			boolean omFound = markupMatchStack.isEmpty() ? om.find(seekPos) : false;
			boolean pmFound = markupMatchStack.isEmpty() ? pm.find(seekPos) : false;
			int skipPos, paraPos;
			
			skipPos = boFound ? boMatcher.start() : Integer.MAX_VALUE;
			skipPos = bcFound ? Math.min(skipPos, bcMatcher.start()) : skipPos;
			
			paraPos = omFound ? om.start() : Integer.MAX_VALUE;
			paraPos = pmFound ? Math.min(paraPos, pm.start()) : paraPos; 
			
			if (!markupMatchStack.isEmpty() || skipPos <= paraPos) {
				if (boFound && skipPos == boMatcher.start()) {
					markupMatchStack.push(boMatcher.toMatchResult());
					seekPos = boMatcher.end();
					
					if (boMatcher.group().length() > 2 && boMatcher.group().startsWith("[[")) {
						/* Need to keep track of internal links balance inside file or image link */
						boMatcher = blockOpenerWithIntLink.matcher(text);
						bcMatcher = blockCloserWithIntLink.matcher(text);
					}
				}
				else if (bcFound) {
					boolean matchFound = false;
					MatchResult lastOpen = markupMatchStack.peek();
					String lastOpenMarkup = lastOpen.group().toLowerCase().substring(0, 2);
					
					while (!markupMatchStack.isEmpty()) {
						if (bcMatcher.group().toLowerCase().lastIndexOf(blockMarkupMap.get(lastOpenMarkup)) >= 0) {
							markupMatchStack.pop();
							seekPos = bcMatcher.end();
							matchFound = true;
							break;
						}
						
						lastOpen = markupMatchStack.pop();
						lastOpenMarkup = lastOpen.group().toLowerCase().substring(0, 2);
					}
					
					if (!matchFound) {
						seekPos = lastOpen.end();
					}
					
					if (markupMatchStack.isEmpty()) {
						boMatcher = blockOpener.matcher(text);
						bcMatcher = blockCloser.matcher(text);
					}
				}
				else {
					if (!markupMatchStack.isEmpty()) {
						/* There is a markup balance problem. Skip the unbalanced markup(s) and retry. */
						seekPos = markupMatchStack.peekLast().end();
						markupMatchStack.clear();
					}
					else {
						break;
					}
				}
			} else {
	            if ((omFound && pmFound && om.start() < pm.start()) || 
	            	(omFound && !pmFound)) {
	            	if (lastMatch < om.start()) {
	            		String content = text.substring(lastMatch, om.start() - 1);
		                
		                Paragraph para = new Paragraph(lastMatch, content, "\n");
		                paraList.add(para);
	            	}
	            	
		            String content = om.group(1);
		            String trail = om.group(2);
		            
		            Paragraph para = new Paragraph(om.start(), content, trail);
		            paraList.add(para);
		            
		            lastMatch = om.end();
		            seekPos = lastMatch;
	            }
	            else if (pmFound) {
	                String content = text.substring(lastMatch, pm.start());
	                String trail = pm.group();
	                
	                Paragraph para = new Paragraph(lastMatch, content, trail);
	                paraList.add(para);
	                
	                lastMatch = pm.end();
	                seekPos = lastMatch;
	            }
	            else {
	            	break;
	            }
			}
			
		}
		
		if (lastMatch < text.length()) {
            String content = text.substring(lastMatch, text.length());
            
            Paragraph para = new Paragraph(lastMatch, content, "");
            paraList.add(para);
		}
		
		return paraList;
	}
	
}
