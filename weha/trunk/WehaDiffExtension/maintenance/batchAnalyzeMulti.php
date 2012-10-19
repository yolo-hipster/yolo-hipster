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
		$this->addOption( 'nproc', 'Number of analyze processes, default is 1', false, true );
		$this->addOption( 'force', 'Overwrite cached revisions result', false, false );
	}

	public function execute() {
		$this->doBatchAnalyze( $this->getOption('fromid'), $this->getOption('limit'), $this->getOption('nproc', 1), $this->hasOption('force') );
	}

	protected function doBatchAnalyze( $article_id, $limit, $nproc, $force ) {
		$dbr = wfGetDB( DB_SLAVE );
		
		// Determine pages to be updated
		$selectCond = array( 'page_namespace' => 0 ); // Only process main namespace
		if ( !$force ) {
			$selectCond[] = '(page_latest <> weha_page_analyzed_rev OR weha_page_analyzed_rev IS NULL)';
		}
		
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
			unset($latest_id);
			$latest_id = array();
			for ( $i = 0; $i < $nproc + 1; $i++ ) {
				$latest_rev[$i] = NULL;
			}
			
			$dbw->begin();
			$np = $nproc;
			for ( $j = 0; $j < count( $all_revs_arr ); $j += $nproc ) {
				$latest_rev[0] = $latest_rev[$nproc];
				
				if ( $j + $nproc >= count( $all_revs_arr ) ) {
					$np = count( $all_revs_arr ) - $j;
				}
				
				for ( $i = 1; $i < $np + 1; $i++ ) {
					$latest_id[$i] = $all_revs_arr[$j + $i - 1]['rev_id'];
					$last_id = $latest_id[$i];
					$latest_rev[$i] = Revision::loadFromId( $dbr, $latest_id[$i] );
					$diffObj[$i] = NULL;
					
					$rev_diff[ $latest_id[$i] ] = NULL;
					$rev_cached[ $latest_id[$i] ] = false;
				}
				
				if ( !$force ) {
					$rev_diff_query = $dbr->select( 'weha_revision', 
						array( 'weha_rev_id', 'weha_rev_diff' ), 
						array( 'weha_rev_id' => $latest_id ) );
					
					foreach ( $rev_diff_query as $row ) {
						$rev_diff[$row->weha_rev_id] = $row->weha_rev_diff;
					}
				}
				
				$textPairArray = array();
				for ( $i = 1; $i < $np + 1; $i++ ) {
					$id = $latest_id[$i];
					if ( array_key_exists( $id, $reverted_rev_arr ) !== false ) {
						$rev_diff[$id] = "Reverted($reverted_rev_arr[$id]);\n";
					}
					elseif ( array_key_exists( $id, $revert_rev_arr ) !== false ) {
						$rev_diff[$id] = "Revert($revert_rev_arr[$id]);\n";
					}
					elseif ( array_key_exists( $id, $empty_rev_arr ) !== false ) {
						$rev_diff[$id] = "EmptyEdit($empty_rev_arr[$id]);\n";
					}
					else
					{				
						if ( !$force && $rev_diff[$id] != NULL ) {
							$rev_cached[$id] = true;
						}
						else {
							$textPairArray[$i]['old'] = empty($latest_rev[$i-1]) ? '' : $latest_rev[$i-1]->revText();
							$textPairArray[$i]['new'] = $latest_rev[$i]->revText();
						}
					}
				}
				$diffArray = $this->runWehaDiff( $textPairArray, $np );
				if ( $diffArray === false ) {
					return;
				}
				
				for ( $i = 1; $i < $np + 1; $i++ ) {
					if ( !empty( $diffArray[$i] ) ) {
						$diffObj[$i] = json_decode( $diffArray[$i] );
						$rev_diff[ $latest_id[$i] ] = $diffObj[$i]->diffOutput;
					}
					
					if ( !$rev_cached[ $latest_id[$i] ] ) {
						wfOut("Writing revision {$latest_id[$i]}\n");
						$dbw->replace( 'weha_revision',
							array( 'weha_rev_id' ), 
							array( 'weha_rev_id' => $latest_id[$i],
								   'weha_rev_diff' => $rev_diff[ $latest_id[$i] ],
								   'weha_rev_action_count' => is_null($diffObj[$i]) ? NULL : json_encode( $diffObj[$i]->rawCount ),
								   'weha_rev_significance' => is_null($diffObj[$i]) ? NULL : $diffObj[$i]->significance ),
							__METHOD__ );
					}
				}
			}
			$dbw->commit();
			
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
			
			$dbw->begin();
			$dbw->replace( 'weha_page',
				array( 'weha_page_id' ), 
				array( 'weha_page_id' => $aid,
					   'weha_page_analyzed_rev' => $last_id,
					   'weha_page_total_significance' => $total_sig,
					   'weha_page_avg_significance' => $avg_sig ),
				__METHOD__ );
			$dbw->commit();
			
			wfOut("Analysis finished. (" . round( microtime( true ) - $startTime, 2 ) . " sec)\n\n");
		}
		
	}
	
	private function runWehaDiff( $textPairArray, $nproc ) {
		global $IP;
		
		$procCount = count( $textPairArray );
		
		# Make temporary files
		$td = wfTempDir();
		$oldtextName = array();
		$newtextName = array();
		$oldtextFile = array();
		$newtextFile = array();
		
		for ( $i = 1; $i < $nproc + 1; $i++ ) {
			if ( isset($textPairArray[$i]) ) {
				$oldtextFile[$i] = fopen( $oldtextName[$i] = tempnam( $td, 'weha-diff-old-' ), 'w' );
				$newtextFile[$i] = fopen( $newtextName[$i] = tempnam( $td, 'weha-diff-new-' ), 'w' );
				
				fwrite( $oldtextFile[$i], $textPairArray[$i]['old'] );
				fclose( $oldtextFile[$i] );
				fwrite( $newtextFile[$i], $textPairArray[$i]['new'] );
				fclose( $newtextFile[$i] );
			}
		}
		
		// Get the diff of the two files
		$cmd = array();
		$h = array();
		$procStatus = array();
		$diff = array();
		
		for ( $i = 1; $i < $nproc + 1; $i++ ) {
			if ( isset($textPairArray[$i]) ) {
				$cmd[$i] = "java -Dfile.encoding=UTF8 -classpath $IP/extensions/WehaDiff/WehaDiff.jar mo.umac.weha.summarizer.EditSignificanceCalculator" . ' ' . wfEscapeShellArg( $oldtextName[$i], $newtextName[$i] );
				$h[$i] = popen( $cmd[$i], 'r' );
				if ( $h[$i] === false ) {
					return false;
				}
				
				$procStatus[$i] = 1; // 0: finished, 1: not finished
				$diff[$i] = '';
			}
		}
		
		$procFinished = $procCount;

		do {
			for ( $i = 1; $i < $nproc + 1; $i++ ) {
				if ( isset($textPairArray[$i]) ) {
					if ( $procStatus[$i] == 0 ) {
						continue;
					}
					
					$data = fread( $h[$i], 8192 );
					if ( strlen( $data ) == 0 ) {
						$procStatus[$i] = 0;
						$procFinished--;
					}
					
					$diff[$i] .= $data;
				}
			}
		} while ( $procFinished > 0 );

		// Clean up
		for ( $i = 1; $i < $nproc + 1; $i++ ) {
			if ( isset($textPairArray[$i]) ) {
				pclose( $h[$i] );
				unlink( $oldtextName[$i] );
				unlink( $newtextName[$i] );
			}
		}
		
		return $diff;
	}
}

$maintClass = 'WehaBatchAnalyze';
require_once( RUN_MAINTENANCE_IF_MAIN );
