<?php 
	include_once("../classes/traitementRequetes/requeteur.class.php");
	include_once("../utils/weha/categorizer/ActionCategorizerQuiEditQui.php");
	include_once("../utils/weha/WikiDiffFormatterQuiEditQui.php");
	ini_set('user_agent', 'ProjetWiki (https://github.com/yolo-hipster/yolo-hipster; rlamour2@yahoo.ca)');
	
	QuiEditQui::QuiEditerTexteDeQui("en.wikipedia.org/wiki/Alfred_Poupart");
	
	class QuiEditQui{
	
		static function QuiEditerTexteDeQui($url)
		{
			$wikipage = ArticleWiki::createByURL($url);
			$wikiRevs = Requeteur::getAllRevisions($wikipage);
			$QuiEditQui = Array();
			$organizer = new QuiEditQuiOutPutOrganizer();
			//var_dump($wikiRevs);
			//var_dump(count($wikiRevs));
			if(!empty($wikiRevs))
			{
				$i = 1;
				$lexer = new WikiLexer($wikiRevs[0]["*"]);
				$newText = $lexer->getWikiTokens($wikiRevs[0]["user"],$i);
				$oldText = Array();
				$ac = new ActionCategorizer($oldText , $newText);
				$ac->printResult();
				$ac->categorize();
				
				//Test visuel
				//$wdf = new WikiDiffFormatter($oldText, $newText);
				//echo $wdf->outputDiff();
				//---
				
				
				//Crée la premiere ligne de inserts.
				$oldText = $newText;
				$newEdit = new QuiEditQuiElem(1, $wikiRevs[0]["user"]);
				$organizer->AddEdit($newEdit);
				foreach ($ac->getSentenceEdits() as $edit){
					$newEdit->LinesAdded++;
				}
				
				//var_dump($newEdit);
				
				$tokenMasterList = new MainTextArray($oldText);
				//var_dump($tokenMasterList);
				//var_dump($ac->getBasicEdits());
				//var_dump($ac->getSentenceEdits());
				//$be = $ac->getBasicEdits();
				//var_dump($be->newTokens[0]);
				//$currentEdit = new Edits($i, $wikiRevs[0]["user"]);
				
				//var_dump($mainText);
				for($i = 1;$i < count($wikiRevs); $i++){
					$currentEditId = $i + 1;
					$lexer = new WikiLexer($wikiRevs[$i]["*"]);
					$newText = $lexer->getWikiTokens($wikiRevs[$i]["user"],$currentEditId);
					
					//Test visuel
					 //$wdf = new WikiDiffFormatter($oldText, $newText);
					 //echo $wdf->outputDiff();
					//---
					
					$ac = new ActionCategorizer($oldText , $newText);
					$ac->printResult();
					$ac->categorize();
					
					foreach($ac->getSentenceEdits() as $edit){
						$newEdit = new QuiEditQuiElem($currentEditId, $wikiRevs[$i]["user"]);
						
						if($edit instanceof SentenceIns){
							$newEdit->LinesAdded = 1;
							$hashbefore = null;
							if($edit->getOldPos() > -1){
								$newEdit->RelatedUser = $oldText[$edit->getOldPos()]->userName;
								$newEdit->RelatedId = $oldText[$edit->getOldPos()]->editionId;
								$hashbefore = $oldText[$edit->getOldPos()]->getHash();
							} else if(count($oldText) > 0){
								$newEdit->RelatedUser = $oldText[0]->userName;
								$newEdit->RelatedId = $oldText[0]->editionId;
							}
							
							$organizer->addEdit($newEdit);
							$tempInsArr = array_slice($newText, $edit->getNewPos() + 1, $edit->getNewLength());
							$tokenMasterList->InsertTokens($tempInsArr, $hashbefore);
							//var_dump($tokenMasterList->getArray());
							
						} else if($edit instanceof SentenceDel){
							
							$newEdit->RelatedUser = $oldText[$edit->getOldPos()]->userName;
							$newEdit->RelatedId = $oldText[$edit->getOldPos()]->editionId;
							$newEdit->LinesRemoved = 1;
							$firstHash = $oldText[$edit->getOldPos() + 1]->getHash();
							$length = $edit->getOldLength() - $edit->getNewLength();
							$organizer->addEdit($newEdit);
							$tokenMasterList->DeleteTokens($firstHash, $length);
							
						} else {
							$newEdit->RelatedUser = $oldText[$edit->getOldPos()]->userName;
							$newEdit->RelatedId = $oldText[$edit->getOldPos()]->editionId;
							$newEdit->LinesModified = 1;
							//
							$organizer->addEdit($newEdit);
						}
						//var_dump($edit);
						
					} //fin boucle edits
					//var_dump($organizer->getEditList());
					$oldText = $tokenMasterList->getArray();
				} //fin boucle revisions
				
			}
			//var_dump($organizer->getEditList());
			//echo json_encode($organizer->getEditList());
			return json_encode($organizer->getEditList());
		}
	}
	
	class MainTextArray{
		private $innerList;
		
		public function __construct($array = null){
			if($array != null && is_array($array)){
				$this->innerList = $array;
			} else {
				$this->innerList = Array();
			}
		}
		
		public function InsertTokens($array, $hashbefore = null){
			$list = $this->innerList;
			$splitId = 0;
			if($hashbefore != null){
				$splitId = $this->findNode($hashbefore);
			}
			$splitId++; //for insert after the hash
			
			$firstPart = array_slice($list, 0, $splitId);
			//var_dump($firstPart);
			//var_dump($array);
			$lastPart = array_slice($list, $splitId, count($list) - $splitId);
			//var_dump($lastPart);
			
			$this->innerList = array_merge($firstPart, $array, $lastPart);
		}
		
		private function findNode($hashcode){
			$list = $this->innerList;
			reset($list);
			$nodeIndex = 0;
			//echo "CURRENT ITEM";
			//echo $hashcode;
			//var_dump(current($list));
			while(current($list) != null && current($list)->getHash() != $hashcode){
				next($list);
				$nodeIndex++;
			}
			
			if(current($list) == null){
				return -1;
			} else {
				return $nodeIndex;
			}
		}
		
		public function DeleteTokens($FirstHash, $length){
			$nodeIndex = $this->findNode($FirstHash);
			//echo "node index: " . $nodeIndex;
			if($nodeIndex >= 0){
				array_splice($this->innerList, $nodeIndex, $length);
			}
		}
		
		public function getArray(){
			return $this->innerList;
		}
	}
	
	class QuiEditQuiOutPutOrganizer{
		private $EditList;
		private $InnerEditList;
		
		public function __construct(){
			$this->EditList = Array();
			$this->InnerEditList = Array();
		}
		
		public function AddEdit(QuiEditQuiElem $elem){
			
			if(!array_key_exists($elem->Id, $this->InnerEditList)){
				$this->InnerEditList[$elem->Id] = Array();
				$this->InnerEditList[$elem->Id][$elem->RelatedId] = $elem;  
				$this->EditList[] = $elem;
			} else {
				if(!array_key_exists($elem->RelatedId, $this->InnerEditList[$elem->Id])){
					$this->InnerEditList[$elem->Id][$elem->RelatedId] = $elem;
					$this->EditList[] = $elem;
				} else {
					$RegisteredElem = $this->InnerEditList[$elem->Id][$elem->RelatedId];
					$RegisteredElem->LinesAdded += $elem->LinesAdded;
					$RegisteredElem->LinesRemoved += $elem->LinesRemoved;
					$RegisteredElem->LinesModified += $elem->LinesModified;
				}
			}
		}
		
		public function getEditList(){
			return $this->EditList;
		}
	}
	
	class QuiEditQuiElem{
		public $Id;
		public $UserName;
		public $LinesAdded = 0;
		public $LinesRemoved = 0;
		public $LinesModified = 0;
		public $RelatedId;
		public $RelatedUser;
		
		public function __construct($Id, $UserName){
			$this->Id = $Id;
			$this->UserName = $UserName;
		}
	}
?>