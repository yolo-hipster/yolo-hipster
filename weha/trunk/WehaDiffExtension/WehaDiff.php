<?php
# Alert the user that this is not a valid entry point to MediaWiki if they try to access the special pages file directly.
if (!defined('MEDIAWIKI')) {
	echo <<<EOT
To install my extension, put the following line in LocalSettings.php:
require_once( "\$IP/extensions/WehaDiff/WehaDiff.php" );
EOT;
	exit( 1 );
}

$wgExtensionCredits['other'][] = array(
	'name' => 'Wiki Edit History Analyzer',
	'author' => 'Peter K. F. Fong',
	'url' => 'http://weha.sourceforge.net/',
	'description' => 'Provide wiki-syntax aware analysis of edit history',
//	'descriptionmsg' => 'weha_desc',
	'version' => '0.0.1',
);

$dir = dirname(__FILE__) . '/'; # store the location of the setup file.
$wgAutoloadClasses['WehaDiff'] = $dir . 'WehaDiff.body.php'; # Tell MediaWiki where the extension class is.
$wgExtensionFunctions[] = 'WehaDiff::setup'; # Do all initial setup here.
// $wgExtensionMessagesFiles['WehaDiff'] = $dir . 'WehaDiff.i18n.php';
