<?php
/**
 * @file 
 * @ingroup Maintenance
 */

require_once "commandLine.inc";
require_once "HistoryUpdateUser.inc";

if( isset( $options['help'] ) ) {
	echo <<<TEXT
This script will run edit history analysis per user.

TEXT;
	exit( 0 );
}

HistoryUpdateUser();
