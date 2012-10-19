<?php
include_once('/var/lib/tomcat6/webapps/JavaBridge/java/Java.inc');
include_once('WikiToken.php');
include_once('WikiLexerConstants.php');

class WikiLexer {
	
	private $tokens;
	private $wikiTokens;
	
	public function __construct($text) {
		$reader = new Java('java.io.StringReader', $text);
		$scanner = new Java('mo.umac.wikianalysis.lexer.MediawikiScanner', $reader);
		$scanner->tokens = new Java('java.util.ArrayList');
		$scanner->parse();
		$this->tokens = $scanner->getTokens();
	}
	
	public function getWikiTokens() {
		$this->wikiTokens = array();
		
		$tokensArray = java_values($this->tokens);
		foreach($tokensArray as $tok) {
			$this->wikiTokens[] = new WikiToken(
				java_values($tok->kind), 
				java_values($tok->image), 
				java_values($tok->displayString) );
		}
		
		return $this->wikiTokens;
	}
}