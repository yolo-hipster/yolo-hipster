<?php
/**
 * @file
 * @ingroup Maintenance
 */

function HistoryUpdateTotal( $article_id = false, $force = false ) {
	if ( $article_id === false )
	{
		$dbr = wfGetDB( DB_SLAVE );

		$prev_page_query = $dbr->select('weha_page', array('MAX(page_id) AS previd'), array('page_total_significance is not null'));
		if ($prev_page_query->numRows() > 0)
		{
			$prev_page_id = $prev_page_query->fetchObject()->previd;
			if (is_null($prev_page_id)) 
				$prev_page_id = 0;
		}
		else 
			$prev_page_id = 0;
		
		$all_pages = $dbr->select('weha_page', array('page_id'), "page_id > $prev_page_id", '__METHOD__', array('ORDER BY' => 'page_id ASC'));
		$all_pages_arr = array();
		
		foreach( $all_pages as $row )
			$all_pages_arr[] = $row->page_id;
	}
	else
	{
		$all_pages_arr = array();
		$all_pages_arr[] = $article_id;
	}
	
	foreach( $all_pages_arr as $aid )
	{
		$article = Article::newFromID( $aid );
		
		wfOut( "Total edit significance of article \"{$article->getTitle()}\" (page_id: $aid) = " );
		
		$dbr = wfGetDB( DB_SLAVE );
		$tbl_rev = $dbr->tableName('weha_revision');
		
		$sig_query = $dbr->select($tbl_rev, array('sum(weha_rev_significance) AS total_sig', 'avg(weha_rev_significance) AS avg_sig'), "weha_rev_page = $aid");
		if ($sig_query->numRows() > 0)
		{
			$sig_result = $sig_query->fetchObject();
			$total_sig = $sig_result->total_sig;
			$avg_sig = $sig_result ->avg_sig;
		}
		
		$dbw = &wfGetDB(DB_MASTER);
	
		$dbw->begin();
		$dbw->update('weha_page',
			array('page_total_significance' => $total_sig, 
				  'page_avg_significance' => $avg_sig), 
			array('page_id' => $aid));
		$dbw->commit();
		wfOut( "$total_sig\n" );
	}
	
}
