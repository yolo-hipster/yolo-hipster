<?php 
	include_once("../data/contentFetcher.php");
	ini_set('user_agent', 'ProjetWiki (https://github.com/yolo-hipster/yolo-hipster; rlamour2@yahoo.ca)');
	
	function QuiEditerTexteDeQui($url, $article)
	{
		$url = "http://en.wikipedia.org";
		$article = "Alfred_Poupart";
		$wikiRevs = getAllRevContent($url, $article);
		$wikiRevsObj = json_decode($wikiRevs, true);
		
		var_dump($wikiRevsObj);
		//$articleContent = $wikiRevsObj[1]["*"];
	}
	QuiEditerTexteDeQui("", "");
?>