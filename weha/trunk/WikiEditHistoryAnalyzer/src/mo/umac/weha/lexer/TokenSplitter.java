package mo.umac.weha.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mo.umac.weha.data.Sentence;
import mo.umac.weha.data.Token;

public class TokenSplitter {

	private static final Pattern pattern = Pattern.compile("( [^\\S\n]+ | [^\\s\\p{IsP}\\p{Punct}]+ | \\[\\[ | \\]\\] | \\{\\{ | \\}\\} | <!--+ | --+> | <[^>]+> | ={1,6} | ^[*\\#]+ | ''''' | ''' | '' | (\\p{Alpha}\\.){2,} | .\\s* ) ((?!< \n) [^\\S\n]+)?", Pattern.COMMENTS | Pattern.MULTILINE | Pattern.DOTALL);
	
	private static final int maxTokens = 500;
	
	public static List<Token> separateToken(Sentence sent) {
		List<Token> tokens = new ArrayList<Token>();
		
		Matcher patternMatcher = pattern.matcher(sent.getContent());
		
		while (patternMatcher.find()) {
			if (tokens.size() >= maxTokens) {
				tokens.clear();
				tokens.add(new Token(0, sent.getContent(), "", sent));
				return tokens;
			}
			
			String text = patternMatcher.group();
			int wsPos = text.length();
			char[] charArray = text.toCharArray();
			for (int i = charArray.length - 1; i >= 0; i--) {
				if (!Character.isWhitespace(charArray[i])) {
					wsPos = i + 1;
					break;
				}
			}
			
			tokens.add(new Token(patternMatcher.start(), text.substring(0, wsPos), text.substring(wsPos), sent));
			
		}
		
		return tokens;
	}
	
}
