<?php
include_once("WikiDiffFormatter.php");

function ShowDiff(){

//$myFile = "NewText.txt";
/*$fh = fopen($myFile, 'r');*/
//$newText = fread($fh, filesize($myFile));
$newText = "Le poisson est bleu.";
//fclose($fh);	

//$myFile = "OldText.txt";
//$fh = fopen($myFile, 'r');
//$oldText = fread($fh, filesize($myFile));
$oldText = "Le poisson rouge contient du bleu.";
//fclose($fh);

$ac = new WikiDiffFormatter($oldText, $newText);
$result = $ac->outputDiff();

echo $result;

}

ShowDiff();