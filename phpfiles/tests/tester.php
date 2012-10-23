<?php
	/*
		Effectue une série de tests.
	*/
	
	include("contentFetcher.php");
	ini_set('user_agent', 'ProjetWiki (https://github.com/yolo-hipster/yolo-hipster; ralphdsanon@hotmail.com)'); //Requis pour éviter une erreur 403
	
	function testContentFetcher(){
		$site = "http://en.wikipedia.org";
		$article = "Celine%20Dion";
		$skipper = "<br><br>";
		$content = getAllUsers($site, $article);
		$content.= $skipper;
		$content.= getAllRevId($site, $article);
		$content.= $skipper;
		$content.= getUserRevIds($site, $article, "");
		echo $content;
	}
	
	testContentFetcher();

?>