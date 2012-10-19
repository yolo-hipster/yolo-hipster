<?php
/**
 * @file 
 * @ingroup Maintenance
 */

$optionsWithArgs = array( 'article', 'revision' );

require_once "commandLine.inc";
require_once "HistoryBatchAnalyzer.inc";

if( isset( $options['help'] ) ) {
	echo <<<TEXT
This script will run edit history analysis on specified article.

Usage:
    php HistoryBatchAnalyzer.php --article <article_id>

    --article: Specify the ID of the article.
    --revision: Specify the ID of one revision of the article, and analyze all 
    			revisions of the article, from the beginning to the end.
    --force: Force analyzer to run on cached revision.

TEXT;
	exit( 0 );
}

if ( isset($options['revision']) && !empty($options['revision']) ) {
	$dbr = wfGetDB( DB_SLAVE );
	$tbl_rev = $dbr->tableName('revision');
	$article_query = $dbr->select($tbl_rev, array('rev_page'), 'rev_id=' . $options['revision']);
	if ($article_query->numRows() > 0)
	{
		$article_row = $article_query->fetchRow();
		$options['article'] = $article_row['rev_page'];
	}
}

if ( !isset($options['article']) || empty($options['article']) ) {
	$options['article'] = false;
}

HistoryAnalysisBatch( $options['article'], isset($options['force']), isset($options['gte']) );
