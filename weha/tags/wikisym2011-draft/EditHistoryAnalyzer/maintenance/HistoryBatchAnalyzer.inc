<?php
/**
 * @file
 * @ingroup Maintenance
 */

function HistoryAnalysisBatch( $article_id = false, $force = false, $gte = false ) {
	if ( $article_id === false )
	{
		$dbr = wfGetDB( DB_SLAVE );

		$query = "SELECT `page`.`page_id` , `page_analyzed_rev` " .
				 "FROM `page` LEFT JOIN `weha_page` " .
				 "ON ( `page`.`page_id` = `weha_page`.`page_id` ) " .
				 "WHERE `page_namespace` =0 ORDER BY `page_id` ASC;";
		$all_pages = $dbr->query($query);
		$all_pages_arr = array();

		foreach( $all_pages as $row )
		{
			if ( is_null($row->page_analyzed_rev) )
			$page_analyzed_rev = 0;
			else
			$page_analyzed_rev = $row->page_analyzed_rev;
			$all_pages_arr[] = array('id' => $row->page_id, 'last_rev' => $page_analyzed_rev);
		}
	}
	elseif ( $gte === true )
	{
		$dbr = wfGetDB( DB_SLAVE );

		$query = "SELECT `page`.`page_id` , `page_analyzed_rev` " .
				 "FROM `page` LEFT JOIN `weha_page` " . 
				 "ON ( `page`.`page_id` = `weha_page`.`page_id` ) " . 
				 "WHERE `page_namespace` = 0 AND `page_id` >= $article_id ORDER BY `page_id` ASC;";
		$all_pages = $dbr->query($query);
		$all_pages_arr = array();

		foreach( $all_pages as $row )
		{
			if ( is_null($row->page_analyzed_rev) )
			$page_analyzed_rev = 0;
			else
			$page_analyzed_rev = $row->page_analyzed_rev;
			$all_pages_arr[] = array('id' => $row->page_id, 'last_rev' => $page_analyzed_rev);
		}
	}
	else
	{
		$dbr = wfGetDB( DB_SLAVE );

		$query = "SELECT `page`.`page_id` , `page_analyzed_rev` " .
				 "FROM `page` LEFT JOIN `weha_page` " . 
				 "ON ( `page`.`page_id` = `weha_page`.`page_id` ) " . 
				 "WHERE `page_namespace` = 0 AND `page`.`page_id` = $article_id ORDER BY `page_id` ASC;";
		$all_pages = $dbr->query($query);
		$all_pages_arr = array();

		$row = $all_pages->fetchObject();
		if ( is_null($row->page_analyzed_rev) )
		$page_analyzed_rev = 0;
		else
		$page_analyzed_rev = $row->page_analyzed_rev;
		$all_pages_arr[] = array('id' => $row->page_id, 'last_rev' => $page_analyzed_rev);
	}


	foreach( $all_pages_arr as $aid_arr )
	{
		$aid = $aid_arr['id'];
		$rev_analyzed = $aid_arr['last_rev'];

		$article = Article::newFromID( $aid );

		$dbr = wfGetDB( DB_SLAVE );
		$tbl_rev = $dbr->tableName('revision');

		if ($force) $rev_analyzed = 0;
		$all_revs = $dbr->select($tbl_rev, array('rev_id', 'rev_timestamp'), "rev_page = $aid AND rev_id > $rev_analyzed", '__METHOD__', array('ORDER BY' => 'rev_timestamp ASC'));
		$all_revs_arr = array();

		if ($all_revs->numRows() == 0)
		continue;
			
		wfOut( "Batch processing article \"{$article->getTitle()}\" (page_id: $aid)\n" );

		foreach( $all_revs as $row )
		$all_revs_arr[] = $row->rev_id;

		$latest_id = $all_revs_arr[0];
		$latest_rev = Revision::loadFromId($dbr, $latest_id);

		$dbw = &wfGetDB(DB_MASTER);

		wfOut("Analyzing revision $latest_id ... ");

		$current_exist = $dbw->select('weha_revision', array('weha_rev_id', 'weha_rev_md5'), 'weha_rev_id=' . $latest_rev->getId());
		$tokenNew = java_values( java("mo.umac.wikianalysis.lexer.WikitextTokenizer")->tokenize($latest_rev->revText()) );
		if ($current_exist->numRows() == 0 || $force) {
			$current_md5 = md5( $latest_rev->revText(), true );
				
			$tokenCount = 0;
			for ($i = 0; $i < count($tokenNew); $i++)
			{
				if (java_values( $tokenNew[$i]->kind ) ==
				java_values( java("mo.umac.wikianalysis.lexer.MediawikiScannerConstants")->WORD ) )
				$tokenCount++;
			}
				
			if ($current_exist->numRows() > 0)
			{
				$dbw = wfGetDB(DB_MASTER);
				$dbw->begin();
				$dbw->update('weha_revision',
				array('weha_rev_page' =>  $aid,
					  'weha_rev_user' => $latest_rev->getUser(Revision::RAW),
					  'weha_rev_user_text' => $latest_rev->getUserText(Revision::RAW),
					  'weha_rev_md5' => $current_md5,
					  'weha_rev_diff' => "CreateArticle($tokenCount);\n",
					  'weha_rev_bdiff' => "Ins($i, 0)\n",
					  'weha_rev_sdiff' => "",
					  'weha_rev_action_count' => "ContentAddition: $tokenCount\n", 
					  'weha_rev_token_count' => "ContentAddition: " . count($tokenNew) . "\n",
					  'weha_rev_token_affected' => count($tokenNew),
					  'weha_rev_significance' => $tokenCount), 
				array('weha_rev_id' => $latest_rev->getId()));
				$dbw->commit();
			}
			else
			{
				$dbw = wfGetDB(DB_MASTER);
				$dbw->begin();
				$dbw->insert('weha_revision',
				array('weha_rev_id' => $latest_rev->getId(),
					  'weha_rev_page' =>  $aid,
					  'weha_rev_user' => $latest_rev->getUser(Revision::RAW),
					  'weha_rev_user_text' => $latest_rev->getUserText(Revision::RAW),
					  'weha_rev_md5' => $current_md5,
					  'weha_rev_diff' => "CreateArticle($tokenCount);\n",
					  'weha_rev_bdiff' => "Ins($i, 0)\n",
					  'weha_rev_sdiff' => "",
					  'weha_rev_action_count' => "ContentAddition: $tokenCount\n", 
					  'weha_rev_token_count' => "ContentAddition: " . count($tokenNew) . "\n",
					  'weha_rev_token_affected' => count($tokenNew),
					  'weha_rev_significance' => $tokenCount));
				$dbw->commit();
			}
		}
		else {
			$row = $current_exist->fetchRow();
			$current_md5 = $row['weha_rev_md5'];
		}

		wfOut("done\n");

		for ($i = 1; $i < count($all_revs_arr); $i++) {
				
			$previous_rev = $latest_rev;
				
			$latest_id = $all_revs_arr[$i];
			$latest_rev = Revision::loadFromId($dbr, $latest_id);
				
			wfOut("Analyzing revision $latest_id ... ");
				
			$revert_detected = false;
			$rev_diff = null;
			$rev_bdiff = null;
			$rev_sdiff = null;
				

			//wfOut("\t calculating MD5 ... ");
			$current_exist = $dbw->select('weha_revision', array('weha_rev_id', 'weha_rev_md5'), 'weha_rev_id=' . $latest_rev->getId());
			if ($current_exist->numRows() == 0) {
				$current_md5 = md5( $latest_rev->revText(), true );
				$dbw->begin();
				$dbw->insert('weha_revision',
				array('weha_rev_id' => $latest_rev->getId(),
					  'weha_rev_page' => $aid,
					  'weha_rev_user' => $latest_rev->getUser(Revision::RAW),
					  'weha_rev_user_text' => $latest_rev->getUserText(Revision::RAW),
					  'weha_rev_md5' => $current_md5));
				$dbw->commit();
			}
			else {
				$row = $current_exist->fetchRow();
				$current_md5 = $row['weha_rev_md5'];
			}

			//wfOut("done\n");
				
			//wfOut("\t detecting revert ... ");
			$revert_exist = $dbr->select('weha_revision', array('max(weha_rev_id)'),  "weha_rev_page = $aid AND weha_rev_id < " . $latest_rev->getId() . " AND weha_rev_md5 = UNHEX('" . bin2hex($current_md5) . "')");
			if ($revert_exist->numRows() > 0 ) {
				$revert_id = $revert_exist->fetchRow();
				if ( isset( $revert_id['max(weha_rev_id)'] ) && $revert_id['max(weha_rev_id)'] > 0) {
					$revert_detected = true;
					$revert_revid = $revert_id['max(weha_rev_id)'];
				}
			}
				
			if ( !empty($previous_rev) ) {
				$rev_cached = false;
				if ($revert_detected)
				{
					//wfOut("\t writing revert into DB ... ");
					$tokenNew = array();
					$dbw->begin();
					$dbw->update('weha_revision',
					array('weha_rev_diff' => "Revert($revert_revid);\n"),
					array('weha_rev_id' => $latest_rev->getId(),
							  'weha_rev_page' => $aid));
					$dbw->commit();
					wfOut("done (rv)\n");
				}
				elseif (strlen($previous_rev->revText()) > 1000000  ||
				strlen($latest_rev->revText()) > 1000000 ||
				strlen($previous_rev->revText()) > 100 * strlen($latest_rev->revText()) ||
				strlen($previous_rev->revText()) * 100 < strlen($latest_rev->revText()) )
				{
					//wfOut("\t writing revert into DB ... ");
					$tokenNew = array();
					$dbw->begin();
					$dbw->update('weha_revision',
					array('weha_rev_diff' => "CopyVandal();"),
					array('weha_rev_id' => $latest_rev->getId(),
							  'weha_rev_page' => $aid));
					$dbw->commit();
					wfOut("done (cv)\n");
				}
				else
				{
					if (!$force)
					{
						//wfOut("\t is analysis result cached? ");
						$rev_diff_query = $dbr->select('weha_revision', array('weha_rev_diff', 'weha_rev_bdiff', 'weha_rev_sdiff'), "weha_rev_page = $aid AND weha_rev_id = " . (empty($input_revid) ? $latest_id : $input_revid ) );
						if ($rev_diff_query->numRows() > 0)
						{
							$rev_diff_row = $rev_diff_query->fetchRow();
							$rev_diff = $rev_diff_row['weha_rev_diff'];
							$rev_bdiff = $rev_diff_row['weha_rev_bdiff'];
							$rev_sdiff = $rev_diff_row['weha_rev_sdiff'];
						}
					}
						
					if (!$force && $rev_diff != null)
					{
						$tokenNew = array();
						$rev_cached = true;
						//wfOut("yes\n");
					}
					else
					{
						//wfOut("no\n");
						//wfOut("\t doing analysis ... \n");
						if (!empty($tokenNew))
						{
							$tokenOld = $tokenNew;
							//wfOut("\t\t old token retrieved.\n");
						}
						else
						{
							$tokenOld = java("mo.umac.wikianalysis.lexer.WikitextTokenizer")->tokenize($previous_rev->revText());
							//wfOut("\t\t old token computed.\n");
						}
						//wfOut("\t\t " . strlen($latest_rev->revText()));
						$tokenNew = java("mo.umac.wikianalysis.lexer.WikitextTokenizer")->tokenize($latest_rev->revText());
						//wfOut(" new token computed.\n");

						$ac = new Java("mo.umac.wikianalysis.categorizer.ActionCategorizer", $tokenOld, $tokenNew);
						$ac->printResult();
						//wfOut("\t\t diff result computed.\n");
						$basicEdits = java_values( $ac->getBasicEdits() );
						//wfOut("\t\t basic edit retrieved.\n");
						$sentenceEdits = $ac->getSentenceEdits();
						//wfOut("\t\t sentence edit retrieved.\n");

						$ac->categorize();
						$catlist = $ac->categorize();
						$catresult = $ac->printCategorize();
						//wfOut("\t\t edit action categorized.\n");

						try {
							$esCalc = new Java("mo.umac.wikianalysis.summarizer.EditSignificanceCalculator", $catlist);
							$esActionCount = $esCalc->printRawCount();
							$esTokenCount = $esCalc->printTokenCount();
							$esTokenCountTotal = $esCalc->getTokenCountTotal();
							$esTotal = $esCalc->calculateSignificance();
						} catch(Exception $e) {
							var_dump($e->getTrace());
						}

						//wfOut("\t\t edit significance calculated.\n");

						//wfOut("\t done\n");
					}
						
					if (!$rev_cached || $force)
					{
						//wfOut("\t writing result into DB ... ");
						$dbw->begin();
						$dbw->update('weha_revision',
						array('weha_rev_diff' => $catresult,
								  'weha_rev_bdiff' => $ac->printBasicEdits(),
								  'weha_rev_sdiff' => $ac->printSentenceEdits(),
								  'weha_rev_action_count' => $esActionCount, 
								  'weha_rev_token_count' => $esTokenCount, 
								  'weha_rev_token_affected' => $esTokenCountTotal, 
								  'weha_rev_significance' => $esTotal), 
						array('weha_rev_id' => $latest_id,
								  'weha_rev_page' => $aid));
						$dbw->commit();
						wfOut( "done\n" );
					}
					else
					wfOut( "cached\n" );
				}
			}
		}


		$sig_query = $dbr->select('weha_revision', array('sum(weha_rev_significance) AS total_sig', 'avg(weha_rev_significance) AS avg_sig'), "weha_rev_page = $aid");
		if ($sig_query->numRows() > 0)
		{
			$sig_result = $sig_query->fetchObject();
			$total_sig = $sig_result->total_sig;
			$avg_sig = $sig_result ->avg_sig;
		}

		$weha_page_exist = $dbr->select('weha_page', array('page_id'), 'page_id = ' . $aid);
		if ($weha_page_exist->numRows() > 0)
		{

			$dbw->begin();
			$dbw->update('weha_page',
			array('page_analyzed_rev' => $latest_id,
							  'page_total_significance' => $total_sig, 
							  'page_avg_significance' => $avg_sig), 
			array('page_id' => $aid));
			$dbw->commit();
		}
		else
		{
			$dbw->begin();
			$dbw->insert('weha_page',
			array('page_id' => $aid,
							  'page_analyzed_rev' => $latest_id,
							  'page_total_significance' => $total_sig, 
							  'page_avg_significance' => $avg_sig));
			$dbw->commit();
		}
		wfOut("Analysis finished (page_id: $aid)\n\n");

	}

}
