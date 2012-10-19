<?php
require_once('simple_html_dom.php');

class WehaDiff {

	public function setup() {
		new WehaDiff;
		return true;
	}

	public function __construct() {
		global $wgHooks;
		
		$wgHooks['UnknownAction'][] = $this;
		$wgHooks['PageHistoryPager::getQueryInfo'][] = 'WehaDiff::addToHistQuery';
		$wgHooks['PageHistoryBeforeList'][] = $this;
		$wgHooks['PageHistoryLineEnding'][] = $this;
	}
	
	public static function addToHistQuery( $pager, array &$queryInfo ) {
	 	$queryInfo['tables'][] = 'weha_revision';
	 	$queryInfo['fields'][] = 'weha_rev_significance';
	 	$queryInfo['fields'][] = 'weha_rev_diff';
	 	$queryInfo['join_conds']['weha_revision'] = array( 'LEFT JOIN', 'weha_rev_id = rev_id' );
 		return true;
 	} 
	
	public function onPageHistoryBeforeList( $article ) {
		global $wgOut, $wgScriptPath;
		
		$wgOut->addScript('<link rel="stylesheet" type="text/css" href="' . 
				$wgScriptPath . '/extensions/WehaDiff/PageHistory.css" />');
		
		return true;
	}
 	
	public function onPageHistoryLineEnding( $history, &$row, &$s ) {
		global $wgScriptPath;
		$url_title = $history->getArticle()->getTitle()->getPrefixedDBKey();
		$html = str_get_html($s);
		
		if ($row->rev_deleted > 0) {
			$html->find('input', 1)->outertext .= " <span class='history-deleted significance-bar-replace-text'> (deleted) </span> ";
		}
		elseif (isset($row->weha_rev_diff) && strpos($row->weha_rev_diff, "Reverted") !== false) {
			$html->find('input', 1)->outertext .= " <span class='significance-bar-replace-text'> (reverted) </span> ";
		}
		elseif (isset($row->weha_rev_diff) && strpos($row->weha_rev_diff, "Revert") !== false) {
			$html->find('input', 1)->outertext .= " <span class='significance-bar-replace-text'> (revert) </span> ";
		}
		elseif (isset($row->weha_rev_diff) && strpos($row->weha_rev_diff, "EmptyEdit") !== false) {
			$html->find('input', 1)->outertext .= " <span class='significance-bar-replace-text'> (empty edit) </span> ";
		}
		elseif (isset($row->weha_rev_significance)) {
			$esValue = $row->weha_rev_significance;
			$esPercentage = min(log($esValue + 1.0, 10) * 25.0, 100.0);
			$html->find('input', 1)->outertext .= " <a class='significance-bar-link' href='?title={$url_title}&action=wehadiff&revid={$row->rev_id}'>" . 
												  "<span class='significance-bar-outer' title='{$esValue}'>" . 
												  "<span class='significance-bar-inner' style='width: {$esPercentage}%;'>&nbsp;</span>" . 
												  "<img src='$wgScriptPath/extensions/WehaDiff/scale.png'></span></a> ";
		
		}
		else {
			$html->find('input', 1)->outertext .= " <span class='significance-bar-replace-text'> (undefined) </span> ";
		}
		
		$s = $html->save();
		
		return true;
	}
	
	public function onUnknownAction( $action, $article ) {
		global $wgOut, $wgScriptPath, $wgRequest;
		
		if ($action == 'wehadiff') {
			$wgOut->setPageTitle('Edit analysis of "'. $article->getTitle() .'"');
			$wgOut->setRobotpolicy( 'noindex,nofollow' );
			$wgOut->setArticleRelated( false );
			
			$wgOut->includeJQuery();
			
			$wgOut->addScriptFile($wgScriptPath . '/extensions/WehaDiff/jquery.scrollTo-1.4.2-min.js');
			$wgOut->addScriptFile($wgScriptPath . '/extensions/WehaDiff/jquery.localscroll-1.2.7-min.js');
			
			$wgOut->addScript('<link rel="stylesheet" type="text/css" href="' . 
				$wgScriptPath . '/extensions/WehaDiff/WehaDiff.css" />');
			$wgOut->addScriptFile($wgScriptPath . '/extensions/WehaDiff/WehaDiff.js');
				
			$dbr = wfGetDB( DB_SLAVE );
			
			// Initialize variables
			$article_id = $article->getID();
			$revid = $wgRequest->getText( 'revid', $article->getLatest() );
			$oldid = $wgRequest->getText( 'oldid', 'prev' );
			
			// Load revisions to be compared
			$new_rev = Revision::loadFromId( $dbr, $revid );

			if ( $oldid == 'prev' ) {
				$old_rev = $new_rev->getPrevious();
			}
			else {
				$old_rev = Revision::loadFromId( $dbr, $oldid );
			}
			
			// Output revision selection control
			$wgOut->addHTML( $this->revisionSelectControl($article, $new_rev, $oldid) );
			
			// Check if the diff result is cached.
			$rev_diff_query = $dbr->selectRow( array( 'weha_revision' ), 
				array( 'weha_rev_diff' ), 
				array( 'weha_rev_id' => $revid ),
				__METHOD__ );
			
			if ( !empty($rev_diff_query) ) {
				$rev_diff = $rev_diff_query->weha_rev_diff;
			}
			else {
				if ( !empty($old_rev) ) {
					$diff = $this->runWehaDiff( $old_rev->revText(), $new_rev->revText() );
				}
				else {
					$diff = $this->runWehaDiff( '', $new_rev->revText() );
				}
				$diffObj = json_decode( $diff );
				$rev_diff = $diffObj->diffOutput;
			}
			
			$wgOut->addHTML( '<br />' . $rev_diff );
			
			return false;
		}
		
		return true;
	}
	
	private function runWehaDiff( $old_text, $new_text ) {
		global $IP;
		
		# Make temporary files
		$td = wfTempDir();
		$oldtextFile = fopen( $oldtextName = tempnam( $td, 'weha-diff-old-' ), 'w' );
		$newtextFile = fopen( $newtextName = tempnam( $td, 'weha-diff-new-' ), 'w' );

		fwrite( $oldtextFile, $old_text );
		fclose( $oldtextFile );
		fwrite( $newtextFile, $new_text );
		fclose( $newtextFile );

		// Get the diff of the two files
		$cmd = "java -Dfile.encoding=UTF8 -classpath $IP/extensions/WehaDiff/WehaDiff.jar mo.umac.weha.summarizer.EditSignificanceCalculator" . ' ' . wfEscapeShellArg( $oldtextName, $newtextName );
		$h = popen( $cmd, 'r' );

		$diff = '';

		do {
				$data = fread( $h, 8192 );
				if ( strlen( $data ) == 0 ) {
						break;
				}
				$diff .= $data;
		} while ( true );

		// Clean up
		pclose( $h );
		unlink( $oldtextName );
		unlink( $newtextName );
		
		return $diff;
	}
	
	private function revisionSelectControl( $article, $new_rev, $oldid ) {
		global $wgScript;
		
		// Previous revision button
		$output  = Xml::openElement( 'form', array( 'method' => 'get', 'action' => $wgScript, 'id' => 'weha-diff-prev', 'style' => 'display: inline' ) );
		if ( !is_null( $new_rev->getPrevious() ) ) {
			$output .= Html::hidden( 'title', $article->getTitle()->getPrefixedDBKey() );
			$output .= Html::hidden( 'action', 'wehadiff' );
			$output .= Html::hidden( 'revid', $new_rev->getPrevious()->getId() );
			$output .= Html::hidden( 'oldid', 'prev' );
			$output .= Xml::submitButton( "<<" );
		}
		else {
			$output .= Xml::submitButton( "<<", array( 'disabled' => 'disabled') );
		}
		$output .= Xml::closeElement( 'form' );
		
		// Revision select drop down list
		$output .= Xml::openElement( 'form', array( 'method' => 'get', 'action' => $wgScript, 'id' => 'weha-diff-revlist', 'style' => 'display: inline' ) );
		$output .= Html::hidden( 'title', $article->getTitle()->getPrefixedDBKey() );
		$output .= Html::hidden( 'action', 'wehadiff' );
		
		$dbr = wfGetDB( DB_SLAVE );
		$tbl_rev = $dbr->tableName('revision');
		$all_revs = $dbr->select( $tbl_rev, array('rev_id', 'rev_timestamp'), 'rev_page = ' . $article->getID(), __METHOD__, array('ORDER BY' => 'rev_timestamp DESC'));

		$curr_options = '';
		$prev_options = '';
		
		$revs_count = $all_revs->numRows();
			
		foreach( $all_revs as $row ) {
			$curr_options .= Xml::option( $revs_count . ": " . wfTimestamp(TS_DB, $row->rev_timestamp), $row->rev_id, $row->rev_id == $new_rev->getId() );
			$prev_options .= Xml::option( $revs_count-- . ": " . wfTimestamp(TS_DB, $row->rev_timestamp), $row->rev_id, $row->rev_id == $oldid );
		}

		$output .= Xml::openElement( 'select', array( 'name' => 'revid', 'id' => 'revid' ) );
		$output .= $curr_options;
		$output .= Xml::closeElement( 'select' );
		
		$output .= Xml::openElement( 'select', array( 'name' => 'oldid', 'id' => 'oldid' ) );
		$output .= Xml::option( 'Previous revision', 'prev', $oldid == 'prev' );
		$output .= $prev_options;
		$output .= Xml::closeElement( 'select' );
		
		$output .= Xml::submitButton( 'Analyze' );
		$output .= Xml::closeElement( 'form' );
		
		// Display Mediawiki diff button
		$output .= Xml::openElement( 'form', array( 'method' => 'get', 'action' => $wgScript, 'id' => 'weha-diff-sysdiff', 'style' => 'display: inline' ) );
		$output .= Html::hidden( 'title', $article->getTitle()->getPrefixedDBKey() );
		$output .= Html::hidden( 'diff', $new_rev->getId() );
		$output .= Html::hidden( 'oldid', $oldid );
		$output .= Xml::submitButton( 'Diff' );
		$output .= Xml::closeElement( 'form' );
		
		// Next revision button
		$next_rev = $dbr->select( $tbl_rev, array('min(rev_id) AS next_id'), 'rev_page = ' . $article->getID() . ' AND rev_id > ' . $new_rev->getId() );
		$next_id = $next_rev->fetchObject();
		
		$output .= Xml::openElement( 'form', array( 'method' => 'get', 'action' => $wgScript, 'id' => 'weha-diff-next', 'style' => 'display: inline' ) );
		if ( !empty($next_id->next_id) ) {
			$output .= Html::hidden( 'title', $article->getTitle()->getPrefixedDBKey() );
			$output .= Html::hidden( 'action', 'wehadiff' );
			$output .= Html::hidden( 'revid', $next_id->next_id );
			$output .= Html::hidden( 'oldid', 'prev' );
			$output .= Xml::submitButton( ">>" );
		}
		else {
			$output .= Xml::submitButton( ">>", array( 'disabled' => 'disabled') );
		}
		$output .= Xml::closeElement( 'form' );
		$output .= '<br />';
		
		return $output;
	}
	
}