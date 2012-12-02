<?php
	/*
		Effectue une série de tests.
	*/
	
	include("contentFetcher.php");
	ini_set('user_agent', 'ProjetWiki (https://github.com/yolo-hipster/yolo-hipster; rlamour2@yahoo.ca)'); //Requis pour éviter une erreur 403
	
	$site = "http://fr.wikipedia.org";
	$article = "Baobab%20africain";
	$skipper = "<br>";
	
	function afficherUtilisateurs(){
		global $site;
		global $article;
		global $skipper;
		
		$content = getAllUsers($site, $article);
		$trouveMoi = "\"user\":";
		$nb = substr_count($content, $trouveMoi);
		$tableauUser = array();
		
		for($i=0; $i<$nb; $i++){
			$position = strpos($content, $trouveMoi);
			$content = substr($content, $position+8);
			$tailleTrouve = strpos($content, "\"");
			$user = substr($content,0, $tailleTrouve);
			$tableauUser[$i] = $user;
			/*print $user;
			print $skipper;*/
		}
		return $tableauUser;
	}

	$unTableau = afficherUtilisateurs();
	
	foreach($unTableau as $elem){
		print $elem;
		print $skipper;
	}
	
	
?>