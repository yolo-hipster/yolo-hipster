<?php
require_once('/var/lib/tomcat6/webapps/JavaBridge/java/Java.inc');
require_once('simple_html_dom.php');
require_once('WikiDiffFormatter.php');

class EditHistoryAnalyzer {
	
	public static function setup() {
		# extension does setup stuff here.
		new EditHistoryAnalyzer;
		return true;
	}

	public function __construct() {
		global $wgHooks;
		 
		$wgHooks['SkinTemplateTabs'][] = $this;
		$wgHooks['UnknownAction'][] = $this;
		$wgHooks['PageHistoryPager::getQueryInfo'][] = 'EditHistoryAnalyzer::addToHistQuery';
		$wgHooks['PageHistoryBeforeList'][] = $this;
		$wgHooks['PageHistoryLineEnding'][] = $this;
	}

	public static function addToHistQuery( $pager, array &$queryInfo ) {
	 	$queryInfo['tables'][] = 'weha_revision';
	 	$queryInfo['fields'][] = 'weha_rev_significance';
	 	$queryInfo['fields'][] = 'weha_rev_diff';
	 	$queryInfo['join_conds']['weha_revision'] = array( 'LEFT JOIN',
	 	"weha_rev_id = rev_id" );
 		return true;
 	} 
	
	public function onPageHistoryBeforeList( $article ) {
		global $wgOut, $wgScriptPath;
		
		$wgOut->addScript('<link rel="stylesheet" type="text/css" href="' . 
				$wgScriptPath . '/extensions/EditHistoryAnalyzer/PageHistory.css" />');
		
		return true;
	}
 	
	public function onPageHistoryLineEnding( $history, &$row, &$s ) {
		$url_title = $history->getArticle()->getTitle()->getPrefixedDBKey();
		$html = str_get_html($s);
		
		if (isset($row->weha_rev_diff) && strpos($row->weha_rev_diff, "Revert") !== false)
			$html->find('input', 1)->outertext .= " <div class='significance-bar-replace-text'> (revert) </div> ";
		elseif (isset($row->weha_rev_diff) && strpos($row->weha_rev_diff, "CopyVandal") !== false)
			$html->find('input', 1)->outertext .= " <div class='significance-bar-replace-text'> (reverted) </div> ";
		elseif (isset($row->weha_rev_significance)) {
			$esValue = $row->weha_rev_significance;
			$esPercentage = min(log($esValue + 1.0, 10) * 25.0, 100.0);
			$html->find('input', 1)->outertext .= " <a class='significance-bar-link' href='?title={$url_title}&action=actionanalysis&ea_revid={$row->rev_id}'>" . 
												  "<div class='significance-bar-outer' title='{$esValue}'>" . 
												  "<div class='significance-bar-inner' style='width: {$esPercentage}%;'>&nbsp;</div><img src='/scale.png'></div></a> ";
		
		}
		else
			$html->find('input', 1)->outertext .= " <div class='significance-bar-replace-text'> (undefined) </div> ";
		
		$s = $html->save();
		
		return true;
	}
	
	public function onSkinTemplateTabs( $skin, &$content_actions ) {
		global $wgTitle, $wgRequest;
		 
		/* Only add tab when the page is an article in main namespce. */
		if ( $wgTitle->getNamespace() == 0 ) {
				
			$action = $wgRequest->getText( 'action' );
			
			$content_actions['actionanalysis'] = array(
	    		'class' => ($action == 'actionanalysischart') ? 'selected' : false,
	    		'text' => wfMsg('weha_tabname'),
	    		'href' => $wgTitle->getLocalURL( 'action=actionanalysischart' )
			);

		}

		return true;
	}

	public function onUnknownAction( $action, $article ) {
		global $wgOut, $wgScriptPath, $wgRequest;
			
		if ($action == 'actionanalysischart') {
			$wgOut->setPageTitle('Edit analysis of "'. $article->getTitle() .'"');
			$wgOut->setRobotpolicy( 'noindex,nofollow' );
			$wgOut->setArticleRelated( false );
			$wgOut->addScript('<link rel="stylesheet" type="text/css" href="' . 
				$wgScriptPath . '/extensions/EditHistoryAnalyzer/WikiDiffFormatter.css" />');
			$wgOut->addScriptFile($wgScriptPath . '/extensions/EditHistoryAnalyzer/WikiDiffFormatter.js');
			
			$article_id = $article->getID();
			$latest_id = $article->getLatest();
			$input_revid = $wgRequest->getText('ea_revid');
			$input_oldid = $wgRequest->getText('ea_oldid');

			$wgOut->addScript('<script type="text/javascript" src="https://www.google.com/jsapi"></script>');
			
			$dbr = wfGetDB( DB_SLAVE );

			$querytext = "SELECT COUNT(*) AS editcount, SUM(`weha_rev_token_affected`) AS tokencount, " . 
						 "SUM(`weha_rev_significance`) AS sigsum, MAX(`weha_rev_significance`) AS sigmax, " . 
						 "AVG(`weha_rev_significance`) AS sigavg FROM `weha_revision` WHERE `weha_rev_page` = $article_id";
			$sum_result = $dbr->query($querytext)->fetchObject();
			$sig_sum = doubleval($sum_result->sigsum);
			$sig_max = doubleval($sum_result->sigmax);
			$sig_avg = doubleval($sum_result->sigavg);
			$token_count = intval($sum_result->tokencount);
			$edit_count = intval($sum_result->editcount);
			
			$querytext = "SELECT COUNT(DISTINCT `weha_rev_user_text`) AS usercount FROM `weha_revision` WHERE `weha_rev_page` = $article_id";
			$sum_result = $dbr->query($querytext)->fetchObject();
			$user_count = intval($sum_result->usercount);
			
			$datatype = $wgRequest->getText('datatype');
			
			if ($datatype == 'editcount')
			{
				$querytext = "SELECT `weha_rev_user_text`, COUNT(`weha_rev_user_text`) AS sigsum FROM `weha_revision` WHERE `weha_rev_page` = $article_id GROUP BY `weha_rev_user_text` ORDER BY sigsum DESC LIMIT 0, 9";
				$sum_value = $edit_count;
			}
			elseif ($datatype == 'tokencount')
			{
				$querytext = "SELECT `weha_rev_user_text`, SUM(`weha_rev_token_affected`) AS sigsum FROM `weha_revision` WHERE `weha_rev_page` = $article_id GROUP BY `weha_rev_user_text` ORDER BY sigsum DESC LIMIT 0, 9";
				$sum_value = $token_count;
			}
			else
			{
				$querytext = "SELECT `weha_rev_user_text`, SUM(`weha_rev_significance`) AS sigsum FROM `weha_revision` WHERE `weha_rev_page` = $article_id GROUP BY `weha_rev_user_text` ORDER BY sigsum DESC LIMIT 0, 9";
				$sum_value = $sig_sum;
			}
			$result = $dbr->query($querytext);
			$data_script = <<<"EOD0"
			<script type="text/javascript">
				google.load("visualization", "1", {packages:["corechart"]});
				google.setOnLoadCallback(drawPieChart);
				function drawPieChart() {
					var data = new google.visualization.DataTable();
					data.addColumn('string', 'User name');
					data.addColumn('number', 'Edit significance');

EOD0;
			$data_count = 0;
			$data_script_value = '';
			$others_value = $sum_value;
			foreach( $result as $row )
			{
				if (doubleval($row->sigsum) > $sum_value / 720.0)
				{
					$data_script_value .= "data.setValue($data_count, 0, '" . addslashes($row->weha_rev_user_text) . "');\n";
					$data_script_value .= "data.setValue($data_count, 1, " .  doubleval($row->sigsum) . ");\n";
					$others_value -= doubleval($row->sigsum);
					$data_count++;
				}
			}

			$data_script_value .= "data.setValue($data_count, 0, '(other users)');\n";
			$data_script_value .= "data.setValue($data_count, 1, " .  doubleval($others_value) . ");\n";
			$data_count++;
			
			$data_script .= "data.addRows($data_count);\n";
			$data_script .= $data_script_value;
			$data_script .= <<<"EOD1"
		        var piechart = new google.visualization.PieChart(document.getElementById('chart_div'));
		        piechart.draw(data, {width: 540, height: 320, title: 'User contributions'});
			    var chart_selector = document.getElementById('chart_selector_div');
		        chart_selector.style.display = 'block';
			    var data_type_selector = document.getElementById('data_type_selector_div');
		        data_type_selector.style.display = 'block';
		        var piechart_radio = document.getElementById('piechart_radio');
		        piechart_radio.checked = true;        
			}
		    </script>

EOD1;
	    
		    $wgOut->addScript($data_script);
		    
			if ($datatype == 'editcount') $sum_value = $edit_count;
			elseif ($datatype == 'tokencount') $sum_value = $token_count;
			else $sum_value = $sig_sum;
			
			$result = $dbr->query($querytext);
			$data_script = <<<"EOD0"
			<script type="text/javascript">
				function drawBarChart() {
					var data = new google.visualization.DataTable();
					data.addColumn('string', 'User name');
					data.addColumn('number', 'Edit significance');

EOD0;
			$data_count = 0;
			$data_script_value = '';
			$others_value = $sum_value;
			foreach( $result as $row )
			{
				if (doubleval($row->sigsum) > $sum_value / 720.0)
				{
					$data_script_value .= "data.setValue($data_count, 0, '" . addslashes($row->weha_rev_user_text) . "');\n";
					$data_script_value .= "data.setValue($data_count, 1, " .  doubleval($row->sigsum) . ");\n";
					$others_value -= doubleval($row->sigsum);
					$data_count++;
				}
			}

			$data_script_value .= "data.setValue($data_count, 0, '(other users)');\n";
			$data_script_value .= "data.setValue($data_count, 1, " .  doubleval($others_value) . ");\n";
			$data_count++;
			
			$data_script .= "data.addRows($data_count);\n";
			$data_script .= $data_script_value;
			if ($datatype == 'editcount')
				$haxis_style = "title: 'Edit count', logScale: false";
			elseif ($datatype == 'tokencount')
				$haxis_style = "title: 'Token count', logScale: false";
			else
				$haxis_style = "title: 'Edit significance (log scale)', logScale: true";
			$data_script .= <<<"EOD1"
		        var barchart = new google.visualization.BarChart(document.getElementById('chart_div'));
		        barchart.draw(data, {width: 540, height: 320, title: 'User contributions', legend: 'none',
		                          vAxis: {title: 'User'}, 
		                          hAxis: {{$haxis_style}}
		                         });
		        }
		    </script>

EOD1;
	    
		    $wgOut->addScript($data_script);
		    
			$result = $dbr->query($querytext);
			
			if ($datatype == 'editcount') $sum_value = $edit_count;
			elseif ($datatype == 'tokencount') $sum_value = $token_count;
			else $sum_value = $sig_sum;
			
			$data_script = <<<"EOD0"
			<script type="text/javascript">
				google.setOnLoadCallback(drawBreakdownChart);
				function drawBreakdownChart() {
					var data = new google.visualization.DataTable();
	
EOD0;
			$data_count = 0;
			$data_script_value = '';
	
			$breakdown_arr = array();
			$category_arr = array();
			foreach( $result as $row )
			{
				if (doubleval($row->sigsum) > $sum_value / 720.0)
				{
					$uid = $row->weha_rev_user_text;
					$breakdown_arr[$uid] = array();
					$breakdown_arr[$uid]['Total'] = 0;
					$uid_as = addslashes($uid);
					
					if ($datatype == 'tokencount') 
						$rev_querytext = "SELECT `weha_rev_id`, `weha_rev_token_count` AS weha_rev_action_count FROM `weha_revision` WHERE `weha_rev_page` = $article_id AND `weha_rev_user_text` = '$uid_as'";
					else
						$rev_querytext = "SELECT `weha_rev_id`, `weha_rev_action_count` FROM `weha_revision` WHERE `weha_rev_page` = $article_id AND `weha_rev_user_text` = '$uid_as'";
					
					$rev_result = $dbr->query($rev_querytext);
					
					foreach( $rev_result as $rev_row )
					{
						$result_plaintext = $rev_row->weha_rev_action_count;

						if (is_null($result_plaintext) || empty($result_plaintext)) continue;
						
						if (!is_null($result_plaintext) && (empty($datatype) || $datatype == 'significance') ) 
						{
							$actlist = java("mo.umac.wikianalysis.summarizer.ActionCountReader")->read($result_plaintext);
							$esCalc = new Java("mo.umac.wikianalysis.summarizer.EditSignificanceCalculator", $actlist, $actlist);
							$result_plaintext = $esCalc->printStatistic();
							$result_plaintext = str_replace("\n", '\n', $result_plaintext);
						}
						
						$result_arr = explode( '\n', $result_plaintext );
						
						foreach( $result_arr as $element ) 
						{
							if (empty($element)) continue;
							list($arr_key, $arr_value) = explode(":", $element);
							if (strcmp($arr_key, 'Uncategorized') == 0) continue;

							if (isset($breakdown_arr[$uid][$arr_key])) $breakdown_arr[$uid][$arr_key] += intval($arr_value);
							else $breakdown_arr[$uid][$arr_key] = intval($arr_value);
							$breakdown_arr[$uid]['Total'] += intval($arr_value);
						}
					}
				}
			}
			
			$data_script .=	"data.addColumn('string', 'Article');\n";
			
			$category_arr[] = 'EditorialComments';
			$category_arr[] = 'Categorize';
			$category_arr[] = 'Interwiki';
			$category_arr[] = 'Wikify';
			$category_arr[] = 'Dewikify';
			$category_arr[] = 'PunctuationCorrection';
			$category_arr[] = 'ImageAttribute';
			$category_arr[] = 'TypoCorrection';
			$category_arr[] = 'ImageAddition';
			$category_arr[] = 'ImageRemoval';
			$category_arr[] = 'References';
			$category_arr[] = 'ContentSubstitution';
			$category_arr[] = 'ContentRemoval';
			$category_arr[] = 'ContentAddition';
			$category_arr[] = 'ContentMovement';
			sort($category_arr);
			
			foreach($category_arr as $cat)
			{
				if (strcmp($cat, 'Uncategorized') == 0) continue;
				$data_script .=	"data.addColumn('number', '$cat');\n";
			}
			
			$breakdown_uid = array_keys($breakdown_arr);
			foreach($breakdown_uid as $uid)
			{
				$data_script_value .= "data.setValue($data_count, 0, '" . addslashes($uid) . "');\n";
				$total = $breakdown_arr[$uid]['Total'];
				$cat_count = 1;
				foreach($category_arr as $cat)
				{
					if (strcmp($cat, 'Uncategorized') == 0)
						continue;
					elseif (isset($breakdown_arr[$uid][$cat]))
						$data_script_value .= "data.setValue($data_count, $cat_count, " . $breakdown_arr[$uid][$cat] / max(doubleval($total), 0.01) . ");\n";
					else
						$data_script_value .= "data.setValue($data_count, $cat_count, " . 0.0 . ");\n";
					$cat_count++;
				}
				$data_count++;
			}
			
			$data_script .= "data.addRows($data_count);\n";
			$data_script .= $data_script_value;
			$data_script .= <<<"EOD1"
		        var breakdownchart = new google.visualization.BarChart(document.getElementById('breakdown_chart_div'));
		        breakdownchart.draw(data, {width: 540, height: 320, isStacked: true, 
		        						   title: 'User contribution break down by edit type', 
		        						   chartArea: {width: "45%"}, 
										   hAxis: {format: '#.##%', minValue: 0.0, maxValue: 1.0}
										  });
				}
		    </script>
	
EOD1;
			
		    $wgOut->addScript($data_script);
		    
	    $wgOut->addHTML('<table cellspacing="5" border="0" cellpadding="0">
<tr valign="top" align="left">
<td width="180"><div id="chart_selector_div" style="display: none;"><form>' . 
	    			'<input type="radio" name="chart_type" id="barchart_radio" checked="checked" onClick="drawBarChart();" /> Bar Chart' . 
	    			'<input type="radio" name="chart_type" id="piechart_radio" onClick="drawPieChart();" /> Pie Chart' . 
	    			'</form></div></td>
<td width="2" bgcolor="#666666"><br /></td>
<td width="600" valign="top" align="left">
<div id="data_type_selector_div" style="display: none;"><form id="wehaDataType" action="index.php" method="get">' . 
    			'<input type="radio" name="datatype" value="significance" checked="checked" /> Edit significance ' . 
    			'<input type="radio" name="datatype" value="editcount" /> Edit count ' . 
    			'<input type="radio" name="datatype" value="tokencount" /> Token count ' . 
    	    	'<input type="hidden" name="title" value="' . $article->getTitle()->getPrefixedDBKey() . '" />' .
				'<input type="hidden" name="action" value="actionanalysischart" />' . 
    			//'<input type="submit" id="wehaDataType" value="Change" />' .
'</td>
</tr>
</table>');
    	    
			$wgOut->addHTML('<div id="chart_div" style="display:inline;"></div>');
		    $wgOut->addHTML('<div id="breakdown_chart_div" style="display:inline;"></div><br />');
			
		    $wgOut->addHTML('<div><b>Edit count:</b> ' . $edit_count . '<br />');
			$wgOut->addHTML('<b>Total affected tokens:</b> ' . $token_count . '<br />');
			$wgOut->addHTML('<b>Total edit significance:</b> ' . sprintf('%.2f', $sig_sum) . '<br />');
			$wgOut->addHTML('<b>Maximum edit significance:</b> ' . sprintf('%.2f', $sig_max) . '<br />');
			$wgOut->addHTML('<b>Average edit significance (per user):</b> ' . sprintf('%.2f', ($sig_sum / $user_count)) . '<br />');
			$wgOut->addHTML('<b>Average edit significance (per revision):</b> ' . sprintf('%.2f', $sig_avg) . '</div><br />');
			
			return false;
		}
		elseif ($action == 'actionanalysis')
		{
			$wgOut->setPageTitle('Edit analysis of "'. $article->getTitle() .'"');
			$wgOut->setRobotpolicy( 'noindex,nofollow' );
			$wgOut->setArticleRelated( false );
			$wgOut->addScript('<link rel="stylesheet" type="text/css" href="' . 
				$wgScriptPath . '/extensions/EditHistoryAnalyzer/WikiDiffFormatter.css" />');
			$wgOut->addScriptFile($wgScriptPath . '/extensions/EditHistoryAnalyzer/WikiDiffFormatter.js');
			
			$dbr = wfGetDB( DB_SLAVE );
			
			$article_id = $article->getID();
			$latest_id = $article->getLatest();
			$input_revid = $wgRequest->getText('ea_revid');
			$input_oldid = $wgRequest->getText('ea_oldid');

			$tbl_rev = $dbr->tableName('revision');

			if ( empty($input_revid) )
				$latest_rev = Revision::loadFromId($dbr, $latest_id);
			else
				$latest_rev = Revision::loadFromId($dbr, $input_revid);

			if ( empty($input_oldid) || $input_oldid == 'prev' )
				$previous_rev = Revision::loadFromId($dbr, $latest_rev->getParentId());
			else
				$previous_rev = Revision::loadFromId($dbr, $input_oldid);
			
			$next_rev = $dbr->select($tbl_rev, array('min(rev_id)'), "rev_page = $article_id AND rev_id > " . (empty($input_revid) ? $latest_id : $input_revid ) );
			$next_id = $next_rev->fetchRow();
			
			if ( !empty($previous_rev) ) {
				
				$revert_detected = false;
				
				$dbw = &wfGetDB(DB_MASTER);
				$dbw->begin();

				$current_exist = $dbw->select('weha_revision', array('weha_rev_id', 'weha_rev_md5'), 'weha_rev_id=' . $latest_rev->getId());
				if ($current_exist->numRows() == 0) {
					$current_md5 = md5( $latest_rev->revText(), true );
					$dbw->insert('weha_revision',
					array('weha_rev_id' => $latest_rev->getId(),
						  'weha_rev_page' =>  $article_id,
						  'weha_rev_user' => $latest_rev->getUser(Revision::RAW),
						  'weha_rev_user_text' => $latest_rev->getUserText(Revision::RAW),
						  'weha_rev_md5' => $current_md5));
				}
				else {
					$row = $current_exist->fetchRow();
					$current_md5 = $row['weha_rev_md5'];
				}

				$dbw->commit();

				$revert_exist = $dbr->select('weha_revision', array('max(weha_rev_id)'),  "weha_rev_page = $article_id AND weha_rev_id < " . $latest_rev->getId() . " AND weha_rev_md5 = UNHEX('" . bin2hex($current_md5) . "')");
				if ($revert_exist->numRows() > 0 ) {
					$revert_id = $revert_exist->fetchRow();
					if ( isset( $revert_id['max(weha_rev_id)'] ) && $revert_id['max(weha_rev_id)'] > 0) {
						$revert_detected = true;
						$revert_revid = $revert_id['max(weha_rev_id)'];
					}
				}
			}
			
			$wgOut->addHTML('<form action="index.php" method="get" style="display: inline;">');
			$wgOut->addHTML('<input type="hidden" name="title" value="' . $article->getTitle()->getPrefixedDBKey() . '" />');
			$wgOut->addHTML('<input type="hidden" name="action" value="actionanalysis" />');
			$wgOut->addHTML('<input type="hidden" name="ea_revid" value="' . $latest_rev->getParentId() . '" />');
			$wgOut->addHTML('<input type="hidden" name="ea_oldid" value="prev" />');
			$wgOut->addHTML('<input type="submit" value="<<" />');
			$wgOut->addHTML('</form>');

			$wgOut->addHTML('<form action="index.php" method="get" style="display: inline;">');
			$wgOut->addHTML('<input type="hidden" name="title" value="' . $article->getTitle()->getPrefixedDBKey() . '" />');
			$wgOut->addHTML('<input type="hidden" name="action" value="actionanalysis" />');

			$options = '';
			$prev_options = '';
			$all_revs = $dbr->select($tbl_rev, array('rev_id', 'rev_timestamp'), "rev_page = $article_id", '__METHOD__', array('ORDER BY' => 'rev_timestamp DESC'));
				
			$revs_count = $all_revs->numRows();
				
			foreach( $all_revs as $row ) {
				$options .= "<option value='" . $row->rev_id . "'". ($row->rev_id == $input_revid ? " selected='selected'" : ""). ">" . $revs_count . ":" . $row->rev_timestamp . "</option>\n";
				$prev_options .= "<option value='" . $row->rev_id . "'>"  . $revs_count-- . ":" . $row->rev_timestamp . "</option>\n";
			}

			$wgOut->addHTML('<select name="ea_revid">');
			$wgOut->addHTML($options);
			$wgOut->addHTML('</select>');
			$wgOut->addHTML('<select name="ea_oldid">');
			$wgOut->addHTML('<option value="prev" selected="selected">Previous revision</option>');
			$wgOut->addHTML($prev_options);
			$wgOut->addHTML('</select>');
			$wgOut->addHTML('<input type="submit" value="Analyze" />');
			$wgOut->addHTML('</form>');

			$wgOut->addHTML('<form action="index.php" method="get"  style="display: inline;">');
			$wgOut->addHTML('<input type="hidden" name="title" value="' . $article->getTitle()->getPrefixedDBKey() . '" />');
			$wgOut->addHTML('<input type="hidden" name="diff" value="' . $latest_rev->getId() . '" />');
			$wgOut->addHTML('<input type="hidden" name="oldid" value="prev" />');
			$wgOut->addHTML('<input type="submit" value="Diff" />');
			$wgOut->addHTML('</form>');
			
			$wgOut->addHTML('<form action="index.php" method="get"  style="display: inline;">');
			$wgOut->addHTML('<input type="hidden" name="title" value="' . $article->getTitle()->getPrefixedDBKey() . '" />');
			$wgOut->addHTML('<input type="hidden" name="action" value="actionanalysis" />');
			$wgOut->addHTML('<input type="hidden" name="ea_revid" value="' . $next_id['min(rev_id)'] . '" />');
			$wgOut->addHTML('<input type="hidden" name="ea_oldid" value="prev" />');
			$wgOut->addHTML('<input type="submit" value=">>" />');
			$wgOut->addHTML('</form>');
			
			if ( !empty($previous_rev) ) {
				$wgOut->addHTML('<br />Current version\'s MD5: ' . bin2hex($current_md5) . '<br />');
				
				$rev_cached = false;
				if ($revert_detected)
				{
					$wgOut->addHTML( $wgOut->parse("__NOEDITSECTION__\n" . "== Categorized Edit Action ==") );
					$wgOut->addHTML("Revert($revert_revid);");
					
					$dbw->begin();
					$dbw->update('weha_revision',
						array('weha_rev_diff' => "Revert($revert_revid);"),
						array('weha_rev_id' => $latest_rev->getId(), 
							  'weha_rev_page' => $article_id));
					$dbw->commit();
				}
				else
				{
					$rev_diff_query = $dbr->select('weha_revision', array('weha_rev_diff', 'weha_rev_bdiff', 'weha_rev_sdiff', 'weha_rev_action_count'), "weha_rev_page = $article_id AND weha_rev_id = " . (empty($input_revid) ? $latest_id : $input_revid ) );
					if ($rev_diff_query->numRows() > 0)
					{
						$rev_diff_row = $rev_diff_query->fetchRow();
						$rev_diff = $rev_diff_row['weha_rev_diff'];
						$rev_bdiff = $rev_diff_row['weha_rev_bdiff'];
						$rev_sdiff = $rev_diff_row['weha_rev_sdiff'];

						$esActionCount = $rev_diff_row['weha_rev_action_count'];
					}
					
					if ((empty($input_oldid) || $input_oldid == 'prev') && $rev_diff != null)
					{
						$rev_cached = true;
						
						$tokenOld = java("mo.umac.wikianalysis.lexer.WikitextTokenizer")->tokenize($previous_rev->revText());
						$tokenNew = java("mo.umac.wikianalysis.lexer.WikitextTokenizer")->tokenize($latest_rev->revText());
						
						$sentenceOld = java("mo.umac.wikianalysis.lexer.SentenceSplitter")->separateSentence($tokenOld);
						$sentenceNew = java("mo.umac.wikianalysis.lexer.SentenceSplitter")->separateSentence($tokenNew);
						
						$basicEdits = java("mo.umac.wikianalysis.diff.token.BasicEditReader")->read($rev_bdiff, $tokenOld, $tokenNew);
						$sentenceEdits = java("mo.umac.wikianalysis.diff.sentence.SentenceEditReader")->read($rev_sdiff, $sentenceOld, $sentenceNew);
						
						$catresult = $rev_diff;
						$catlist = java("mo.umac.wikianalysis.categorizer.ActionListReader")->read($rev_diff, $tokenOld, $tokenNew);
						
						$wgOut->addHTML('Diff retrieved from database.<br />');
						
						$esCalc = new Java("mo.umac.wikianalysis.summarizer.EditSignificanceCalculator", $catlist);
						$esActionCount = $esCalc->printRawCount();
						$esTokenCount = $esCalc->printTokenCount();
						$esTokenCountTotal = $esCalc->getTokenCountTotal();
						
						$esResult = $esCalc->printDetailStatistic();
						$esTotal = $esCalc->calculateSignificance();
					}
					else
					{			
						$ac = new Java("mo.umac.wikianalysis.categorizer.ActionCategorizer", $previous_rev->revText(), $latest_rev->revText());
						$ac->printResult();
						$basicEdits = $ac->getBasicEdits();
						$sentenceEdits = $ac->getSentenceEdits();
						
						$tokenOld = $ac->getTokenOld();
						$tokenNew = $ac->getTokenNew();
						
						$catlist = $ac->categorize();
						$catresult = $ac->printCategorize();

						$esCalc = new Java("mo.umac.wikianalysis.summarizer.EditSignificanceCalculator", $catlist);
						$esResult = $esCalc->printDetailStatistic();
						$esActionCount = $esCalc->printRawCount();
						$esTokenCount = $esCalc->printTokenCount();
						$esTokenCountTotal = $esCalc->getTokenCountTotal();

						$esTotal = $esCalc->calculateSignificance();
					}
					
					$wdf = new WikiDiffFormatter($basicEdits, $sentenceEdits, $tokenOld, $tokenNew);
					
					$result = $wdf->outputDiff();
					
					$wgOut->addHTML($wgOut->parse("__NOEDITSECTION__\n" . "== Categorized Edit Action ==") );
					$wgOut->addHTML($esResult);
					$wgOut->addHTML(nl2br("\n<p><big><strong>Total Edit Significance: " . $esTotal . "</strong></big></p>"));
					$wgOut->addHTML($wgOut->parse("__NOEDITSECTION__\n" . "== Differences ==") );
					$wgOut->addHTML($result);

					if ((empty($input_oldid) || $input_oldid == 'prev') && !$rev_cached)
					{
						$dbw->begin();
						$dbw->update('weha_revision',
							array('weha_rev_diff' => $catresult,
								  'weha_rev_bdiff' => $ac->printBasicEdits(),
								  'weha_rev_sdiff' => $ac->printSentenceEdits(),
								  'weha_rev_action_count' => $esActionCount,
								  'weha_rev_token_count' => $esTokenCount,
								  'weha_rev_token_affected' => $esTokenCountTotal,
								  'weha_rev_significance' => $esTotal), 
							array('weha_rev_id' => $latest_rev->getId(), 
								  'weha_rev_page' => $article_id));
						$dbw->commit();
					}
					elseif ((empty($input_oldid) || $input_oldid == 'prev') && !isset($esActionMap))
					{
						$dbw->begin();
						$dbw->update('weha_revision',
							array('weha_rev_action_count' => $esActionCount,
								  'weha_rev_significance' => $esTotal), 
							array('weha_rev_id' => $latest_rev->getId(), 
								  'weha_rev_page' => $article_id));
						$dbw->commit();
					}
				}
			}
			else {
				$wgOut->addHTML( 'This is the earliest version of this article.' );
			}
			
			return false;
		}
		
		return true;
	}

}
