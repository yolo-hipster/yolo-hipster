package mo.umac.weha.lexer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.data.Sentence;

public class SentenceSplitter {

	private static final Pattern skipPattern = Pattern.compile("(<ref[^>]*?/>|<!--+|--+>|\\w\\.(\\w\\.)+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern blockOpener = Pattern.compile("(<ref[^/]*?>|<gallery[^/]*?>|\\[\\[(Image|File):|\\{\\{|\\{\\|)", Pattern.CASE_INSENSITIVE);
	private static final Pattern blockOpenerWithIntLink = Pattern.compile("(<ref[^/]*?>|<gallery[^/]*?>|\\[\\[(Image|File):|\\[\\[|\\{\\{|\\{\\|)", Pattern.CASE_INSENSITIVE);
	private static final Pattern blockCloser = Pattern.compile("(</ref>|</gallery>|\\|\\}\\}|\\}\\}|\\|\\})", Pattern.CASE_INSENSITIVE);
	private static final Pattern blockCloserWithIntLink = Pattern.compile("(</ref>|</gallery>|\\]\\]|\\|\\}\\}|\\}\\}|\\|\\})", Pattern.CASE_INSENSITIVE);
	
	private static final Pattern sentenceStarter = Pattern.compile("(^[*#].*\n)", Pattern.MULTILINE);
	private static final Pattern sentenceTerminator = Pattern.compile("([?!.](<ref[^>]*?/>)*(\\s*)(?=\\p{javaUpperCase}|\\p{Punct}|\\z))|(\\]\n+)|(\n+(?=\\p{Punct}))|(\\z)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
	
	private static final int maxSentences = 250;
	
	private static final Map<String, String> blockMarkupMap = new HashMap<String, String>();
	static {
		blockMarkupMap.put("<r", "</ref>");
		blockMarkupMap.put("<g", "</gallery>");
		blockMarkupMap.put("[[", "]]");
		blockMarkupMap.put("{{", "}}");
		blockMarkupMap.put("{|", "|}");
	}
	
	public static List<Sentence> separateSentence(Paragraph para) {
		LinkedList<Sentence> sentences = new LinkedList<Sentence>();

		String paraContent = para.getContent();
		
		Matcher skipMatcher = skipPattern.matcher(paraContent);
		Matcher boMatcher = blockOpener.matcher(paraContent);
		Matcher bcMatcher = blockCloser.matcher(paraContent);
		
		LinkedList<MatchResult> markupMatchStack = new LinkedList<MatchResult>();
		
		Matcher ssMatcher = sentenceStarter.matcher(paraContent);
		Matcher stMatcher = sentenceTerminator.matcher(paraContent);
		
		int lastMatch = 0;
		int seekPos = 0;
		while (seekPos < paraContent.length()) {
			if (sentences.size() >= maxSentences) {
				sentences.clear();
				sentences.add(new Sentence(0, paraContent, para));
				return sentences;
			}
			
			boolean skipFound = skipMatcher.find(seekPos);
			boolean boFound = boMatcher.find(seekPos);
			boolean bcFound = markupMatchStack.isEmpty() ? false : bcMatcher.find(seekPos);
			boolean ssFound = markupMatchStack.isEmpty() ? ssMatcher.find(seekPos) : false;
			boolean stFound = markupMatchStack.isEmpty() ? stMatcher.find(seekPos) : false;
			int skipPos, sentPos;
			
			skipPos = skipFound ? skipMatcher.start() : Integer.MAX_VALUE;
			skipPos = boFound ? Math.min(skipPos, boMatcher.start()) : skipPos;
			skipPos = bcFound ? Math.min(skipPos, bcMatcher.start()) : skipPos;
			
			sentPos = ssFound ? ssMatcher.start() : Integer.MAX_VALUE;
			sentPos = stFound ? Math.min(sentPos, stMatcher.start()) : sentPos; 
			
			if(markupMatchStack.isEmpty() && lastMatch < seekPos) {
				Matcher nlMatcher = Pattern.compile("^\\s*\n+").matcher(paraContent.substring(seekPos-1));
				
				if (nlMatcher.find()) {
					String content = paraContent.substring(lastMatch, seekPos);
					
					Sentence s = new Sentence(lastMatch, content, para);
					sentences.add(s);
					
					lastMatch = seekPos;
					continue;
				}
			}
			
			if (!markupMatchStack.isEmpty() || skipPos <= sentPos) {
				if (skipFound && skipPos == skipMatcher.start()) {
					seekPos = skipMatcher.end();
				}
				else if (boFound && skipPos == boMatcher.start()) {
					markupMatchStack.push(boMatcher.toMatchResult());
					seekPos = boMatcher.end();
					
					if (boMatcher.group().length() > 2 && boMatcher.group().startsWith("[[")) {
						boMatcher = blockOpenerWithIntLink.matcher(paraContent);
						bcMatcher = blockCloserWithIntLink.matcher(paraContent);
					}
				}
				else if (bcFound) {
					boolean matchFound = false;
					MatchResult lastOpen = markupMatchStack.peek();
					String lastOpenMarkup = lastOpen.group().toLowerCase().substring(0, 2);
					
					while (!markupMatchStack.isEmpty()) {
						if (bcMatcher.group().toLowerCase().lastIndexOf(blockMarkupMap.get(lastOpenMarkup)) >= 0) {
							markupMatchStack.pop();
							matchFound = true;
							
							int blockEnd = bcMatcher.end();
							while (blockEnd < paraContent.length() && Character.isWhitespace(paraContent.charAt(blockEnd))) {
								blockEnd++;
							}
							
							if (markupMatchStack.isEmpty() && lastOpenMarkup.equals("{|")) {
								int tableStart = paraContent.indexOf("{|", lastMatch);
								if (lastMatch < tableStart) {
									sentences.add(new Sentence(lastMatch, paraContent.substring(lastMatch, tableStart), para));
								}
								
								List<Sentence> s = WikiTableSplitter.separateTable(tableStart, paraContent.substring(tableStart, blockEnd), para);
								sentences.addAll(s);
								lastMatch = blockEnd;
							}
							else if (markupMatchStack.isEmpty() && paraContent.startsWith("{{", lastMatch)) {
								List<Sentence> s = WikiTemplateSplitter.separateTemplate(lastMatch, paraContent.substring(lastMatch, blockEnd), para);
								sentences.addAll(s);
								lastMatch = blockEnd;
							}
							else if (markupMatchStack.isEmpty() && paraContent.startsWith("<r", lastMatch)) {
								sentences.add(new Sentence(lastMatch, paraContent.substring(lastMatch, blockEnd), para));
								lastMatch = blockEnd;
							}
							else if (markupMatchStack.isEmpty() && lastOpenMarkup.equals("<g")) {
								String galleryContent = paraContent.substring(lastMatch, blockEnd);
								Matcher lineMatcher = Pattern.compile("(^.*)(\n+|\\z)", Pattern.MULTILINE).matcher(galleryContent);
								
								while (lineMatcher.find()) {
									sentences.add(new Sentence(lastMatch + lineMatcher.start(), lineMatcher.group(), para));
								}
								lastMatch = blockEnd;
							}
							
							seekPos = blockEnd;
							break;
						}
						
						lastOpen = markupMatchStack.pop();
						lastOpenMarkup = lastOpen.group().toLowerCase().substring(0, 2);
					}
					
					if (!matchFound) {
						seekPos = lastOpen.end();
					}
					
					if (markupMatchStack.isEmpty()) {
						boMatcher = blockOpener.matcher(paraContent);
						bcMatcher = blockCloser.matcher(paraContent);
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
			}
			else {
				if ((ssFound && stFound && ssMatcher.start() < stMatcher.start()) ||
					(ssFound && !stFound)) {
					String content = ssMatcher.group();
					
					Sentence s = new Sentence(ssMatcher.start(), content, para);
					sentences.add(s);
					
					lastMatch = ssMatcher.end();
					seekPos = lastMatch;
				}
				else if (stFound && lastMatch < stMatcher.start()){
					String content = paraContent.substring(lastMatch, stMatcher.start());
					String terminator = stMatcher.group();
					
					Sentence s = new Sentence(lastMatch, content + terminator, para);
					sentences.add(s);
					
					lastMatch = stMatcher.end();
					seekPos = lastMatch;
				}
				else if (stFound && lastMatch == stMatcher.start()){
					seekPos++;
				}
				else {
					break;
				}
			}
		}
		
		if (lastMatch < paraContent.length()) {
			String content = paraContent.substring(lastMatch, paraContent.length());
			
			Sentence s = new Sentence(lastMatch, content, para);
			sentences.add(s);
			
			seekPos = paraContent.length();
			lastMatch = seekPos;
		}
		
		return sentences;
	}
	
}

