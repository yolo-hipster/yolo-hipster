<?php

/*
  CE FICHIER A ETE BOUGE VERS yolo/contributions/main/qui_edit.php
  Effectue une s�rie de tests.
 */

include("../classes/traitementRequetes/requeteur.class.php");
ini_set('user_agent', 'ProjetWiki (https://github.com/yolo-hipster/yolo-hipster; rlamour2@yahoo.ca)'); //Requis pour �viter une erreur 403



function afficherUtilisateurs() {
 $url = $_GET['url'];

    $wikiobject = new ArticleWiki();
    $wikiobject->createByURL($url);
    
    $requeteur = new Requeteur();

    $content = $requeteur->getUsers($wikiobject);
    $trouveMoi = "\"user\":";
    $nb = substr_count($content, $trouveMoi);
    $tableauUser = array();

    for ($i = 0; $i < $nb; $i++) {
        $position = strpos($content, $trouveMoi);
        $content = substr($content, $position + 8);
        $tailleTrouve = strpos($content, "\"");
        $user = substr($content, 0, $tailleTrouve);
        $tableauUser[$i] = $user;
        /* print $user;
          print $skipper; */
    }
    return $tableauUser;
}

$unTableau = afficherUtilisateurs();

foreach ($unTableau as $elem) {
    print $elem;
    print $skipper;
}
?>