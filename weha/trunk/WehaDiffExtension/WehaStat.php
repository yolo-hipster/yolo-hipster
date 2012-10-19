<?php
# Alert the user that this is not a valid entry point to MediaWiki if they try to access the special pages file directly.
if (!defined('MEDIAWIKI')) {
	echo <<<EOT
To install my extension, put the following line in LocalSettings.php:
require_once( "\$IP/extensions/WehaDiff/WehaStat.php" );
EOT;
	exit( 1 );
}

$wgExtensionCredits['other'][] = array(
	'name' => 'WEHA Statistics',
	'author' => 'Peter K. F. Fong',
	'url' => 'http://weha.sourceforge.net/',
	'description' => 'Provide statistics of edit history',
//	'descriptionmsg' => 'weha_desc',
	'version' => '0.0.1',
);

$dir = dirname(__FILE__) . '/'; # store the location of the setup file.
$wgAutoloadClasses['WehaStat'] = $dir . 'WehaStat.body.php'; # Tell MediaWiki where the extension class is.
$wgExtensionFunctions[] = 'WehaStat::setup'; # Do all initial setup here.
// $wgExtensionMessagesFiles['WehaStat'] = $dir . 'WehaStat.i18n.php';
