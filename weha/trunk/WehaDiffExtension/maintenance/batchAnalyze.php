<?php

if ( getenv( 'MW_INSTALL_PATH' ) ) {
	$IP = getenv( 'MW_INSTALL_PATH' );
} else {
	$IP = dirname( __FILE__ ) . '/../../..';
}

require_once( "$IP/maintenance/Maintenance.php" );

class WehaBatchAnalyze extends Maintenance {
	public function __construct() {
		parent::__construct();
		
		$this->addOption( 'fromid', 'ID of first article to be analyzed', false, true );
		$this->addOption( 'limit', 'Number of articles to be analyzed, default is to analyze all articles (no limit)', false, true );
		$this->addOption( 'force', 'Overwrite cached revisions result', false, false );
	}

	public function execute() {
		$this->doBatchAnalyze( $this->getOption('fromid'), $this->getOption('limit'), $this->hasOption('force') );
	}

	protected function doBatchAnalyze( $article_id, $limit, $force ) {
		$dbr = wfGetDB( DB_SLAVE );
		
		// Determine pages to be updated
		$selectCond = array('page_namespace' => 0, // Only process main namespace
					  		'(page_latest <> weha_page_analyzed_rev OR weha_page_analyzed_rev IS NULL)');
		$selectOpt  = array( 'ORDER BY' => 'page_id ASC' );
		
		if ( $article_id !== null ) {
			/* Update the article with article ID greater or equals to the specified parameter.
			 * If no article ID is specified, update all articles. */
			$selectCond[] = 'page_id >= ' . intval( $article_id );
		}
		
		if ( $limit !== null ) {
			/* If limit option is specified, update articles up to number of 'limit' articles.
			 * If no limit is specified, update all articles. */
			$selectOpt['LIMIT'] = intval( $limit );
		}
		
		$all_pages = $dbr->select(
				array( 'page', 'weha_page' ),
				array( 'page_id', 'page_latest', 'weha_page_analyzed_rev' ),
				$selectCond,
				__METHOD__,
				$selectOpt,
				array( 'weha_page' => array( 'LEFT JOIN', 'page_id = weha_page_id' ) ) );
		
		$all_pages_arr = array();
		foreach ( $all_pages as $row ) {
			if ( is_null( $row->weha_page_analyzed_rev ) ) {
				$page_analyzed_rev = 0;
			}
			else {
				$page_analyzed_rev = $row->weha_page_analyzed_rev;
			}
			$all_pages_arr[] = array( 'id' => $row->page_id,
									  'last_rev' => $page_analyzed_rev,
									  'latest_rev' => $row->page_latest );
		}
		
		$dbw = &wfGetDB( DB_MASTER );
		foreach ( $all_pages_arr as $page_info ) {
			$startTime = microtime( true );
			
			$aid = $page_info['id'];
			$article = Article::newFromID( $aid );
			
			$all_revs = $dbr->select(
					'revision', 
					array( 'rev_id', 'rev_timestamp', 'rev_sha1' ),
					array( 'rev_page' => $aid, 'rev_deleted' => 0 ),
					__METHOD__,
					array( 'ORDER BY' => 'rev_timestamp ASC' ) );
			
			if ($all_revs->numRows() == 0) {
				continue;
			}
			
			wfOut( "Batch processing article \"{$article->getTitle()}\" (page_id: $aid, revisions: {$all_revs->numRows()})\n" );
			
			$all_revs_arr = array();
			foreach ( $all_revs as $row ) {
				$all_revs_arr[] = array( 'rev_id' => $row->rev_id, 
										 'rev_sha1' => $row->rev_sha1 );
			}
			
			if ( $force ) {
//				wfOut( "Delete all cached analysis result\n" );
				$dbw->deleteJoin( 'weha_revision', 'revision', 'weha_rev_id', 'rev_id', array( 'rev_page' => $aid ), __METHOD__ );
			}
			
			/* Detect SHA1 collision (pure revert and empty edit) */
			$reverted_rev_arr = array();
			$revert_rev_arr = array();
			$empty_rev_arr = array();
			for ( $i = 1; $i < count($all_revs_arr); $i++ ) {
				$latest_id = $all_revs_arr[$i]['rev_id'];
				
				if ( $all_revs_arr[$i]['rev_sha1'] === $all_revs_arr[$i-1]['rev_sha1'] ) {
					$empty_rev_arr[$latest_id] = $all_revs_arr[$i-1]['rev_id'];
					continue;
				}
				
				for ( $j = $i - 2; $j >= 0; $j-- ) {
					$prev_id = $all_revs_arr[$j]['rev_id'];
					
					if ( array_key_exists($prev_id, $reverted_rev_arr) !== false ) {
						break;
					}
					
					if ( $all_revs_arr[$i]['rev_sha1'] === $all_revs_arr[$j]['rev_sha1'] ) {
						for ( $k = $j + 1; $k < $i; $k++ ) {
							$reverted_rev = $all_revs_arr[$k]['rev_id'];
							$reverted_rev_arr[$reverted_rev] = $latest_id;
						}
						$revert_rev_arr[$latest_id] = $prev_id;
					}
				}
			}
			
			/* Analyze all revisions */
			$latest_rev = NULL;
			$dbw->begin();
			for ( $i = 0; $i < count( $all_revs_arr ); $i++ ) {
				$previous_rev = $latest_rev;
				
				$latest_id = $all_revs_arr[$i]['rev_id'];
				$latest_rev = Revision::loadFromId( $dbr, $latest_id );
				
//				wfOut("Analyzing revision $latest_id ... ");
				
				$rev_diff = NULL;
				$diffObj = NULL;
				$rev_cached = false;
				
				if ( !$force ) {
					$rev_diff_query = $dbr->selectRow( 'weha_revision', 
						array( 'weha_rev_diff' ), 
						array( 'weha_rev_id' => $latest_id ) );
					
					if ( !empty( $rev_diff_query ) ) {
						$rev_diff = $rev_diff_query->weha_rev_diff;
					}
				}
				
				if ( array_key_exists( $latest_id, $reverted_rev_arr ) !== false ) {
					$rev_diff = "Reverted($reverted_rev_arr[$latest_id]);\n";
				}
				elseif ( array_key_exists( $latest_id, $revert_rev_arr ) !== false ) {
					$rev_diff = "Revert($revert_rev_arr[$latest_id]);\n";
				}
				elseif ( array_key_exists( $latest_id, $empty_rev_arr ) !== false ) {
					$rev_diff = "EmptyEdit($empty_rev_arr[$latest_id]);\n";
				}
				else
				{				
					if ( !$force && $rev_diff != NULL ) {
						$rev_cached = true;
					}
					else {
						$diff = $this->runWehaDiff( empty($previous_rev) ? '' : $previous_rev->revText(), $latest_rev->revText() );
						$diffObj = json_decode( $diff );
						$rev_diff = $diffObj->diffOutput;
					}
				}
				
				if ( !$rev_cached ) {
					$dbw->replace( 'weha_revision',
						array( 'weha_rev_id' ), 
						array( 'weha_rev_id' => $latest_id,
							   'weha_rev_diff' => $rev_diff,
							   'weha_rev_action_count' => is_null($diffObj) ? NULL : json_encode( $diffObj->rawCount ),
							   'weha_rev_significance' => is_null($diffObj) ? NULL : $diffObj->significance ),
						__METHOD__ );
//					wfOut( "done\n" );
				}
				else {
//					wfOut( "cached\n" );
				}
			}
			
			/* Update weha_page table with info of latest processed revisions */
			$sig_query = $dbr->selectRow( array( 'weha_revision', 'revision' ), 
				array( 'sum(weha_rev_significance) AS total_sig', 
					   'avg(weha_rev_significance) AS avg_sig' ), 
				array( 'rev_page' => $aid ),
				__METHOD__,
				array(),
				array( 'revision' => array( 'LEFT JOIN', 'weha_rev_id = rev_id' ) ) );
			if ( !empty( $sig_query ) ) {
				$total_sig = $sig_query->total_sig;
				$avg_sig = $sig_query ->avg_sig;
			}
			
			$dbw->replace( 'weha_page',
				array( 'weha_page_id' ), 
				array( 'weha_page_id' => $aid,
					   'weha_page_analyzed_rev' => $latest_id,
					   'weha_page_total_significance' => $total_sig,
					   'weha_page_avg_significance' => $avg_sig ),
				__METHOD__ );
			
			$dbw->commit();
			
			wfOut("Analysis finished. (" . round( microtime( true ) - $startTime, 2 ) . " sec)\n\n");
		}
		
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
	
}

$maintClass = 'WehaBatchAnalyze';
require_once( RUN_MAINTENANCE_IF_MAIN );
