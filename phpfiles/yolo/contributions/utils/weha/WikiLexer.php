<?php
include_once "http://localhost:8080/JavaBridge/java/Java.inc";
include_once('WikiToken.php');
include_once('WikiLexerConstants.php');

class WikiLexer {
	
	private $tokens;
	private $wikiTokens;
	private $userName;
	private $editionId;
	
	public function __construct($text) {
		//echo "1";
		$reader = new Java('java.io.StringReader', $text);
		//echo "2";
		$scanner = new Java('mo.umac.wikianalysis.lexer.MediawikiScanner', $reader);
		//echo "3";
		$scanner->tokens = new Java('java.util.ArrayList');
		//echo "4";
		$scanner->parse();
		$this->tokens = $scanner->getTokens();
	}
	
	public function getWikiTokens(&$userName = null, &$editionId = null) {
		$this->wikiTokens = array();
		
		$tokensArray = java_values($this->tokens);
		foreach($tokensArray as $tok) {
			$this->wikiTokens[] = new WikiToken(
				java_values($tok->kind), 
				java_values($tok->image), 
				java_values($tok->displayString),
				$userName,
				$editionId );
		}
		
		return $this->wikiTokens;
	}
}