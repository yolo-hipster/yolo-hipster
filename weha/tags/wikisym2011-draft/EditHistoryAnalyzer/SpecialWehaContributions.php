<?php
class SpecialWehaContributions extends SpecialPage {
	function __construct() {
		parent::__construct( "WehaContributions" );
	}

	function execute( $par ) {
		global $wgRequest, $wgOut, $wgUser;

		$this->setHeaders();
		$this->outputHeader();

		$wgOut->setRobotpolicy( 'noindex,nofollow' );
		$wgOut->setArticleRelated( false );
		
		if( isset( $par ) ) {
			$target = $par;
		} else {
			$target = $wgRequest->getVal( 'target' );
		}
		
		if ( !isset($target) || empty($target) )
		{
			$wgOut->setPageTitle('User\'s contributions by edit significance');
			$wgOut->addHTML($this->getForm());
			return;
		}
		
		$wgOut->setPageTitle('Contributions from "'. $target .'"');
		
		$wgOut->addScript('<script type="text/javascript" src="https://www.google.com/jsapi"></script>');
		
		$dbr = wfGetDB(DB_SLAVE);

		$querytext = "SELECT COUNT(*) AS editcount, SUM(`weha_rev_token_affected`) AS tokencount, " . 
						 "SUM(`weha_rev_significance`) AS sigsum, MAX(`weha_rev_significance`) AS sigmax, " . 
						 "AVG(`weha_rev_significance`) AS sigavg FROM `weha_revision` WHERE `weha_rev_user_text` LIKE '$target'";
		$sum_result = $dbr->query($querytext)->fetchObject();
		$sig_sum = doubleval($sum_result->sigsum);
		$sig_max = doubleval($sum_result->sigmax);
		$sig_avg = doubleval($sum_result->sigavg);
		$edit_count = intval($sum_result->editcount);	
		$token_count = intval($sum_result->tokencount);	
		
		$querytext = "SELECT COUNT(DISTINCT `weha_rev_page`) AS pagecount FROM `weha_revision` WHERE `weha_rev_user_text` LIKE '$target'";
		$sum_result = $dbr->query($querytext)->fetchObject();
		$page_count = intval($sum_result->pagecount);
		
		$datatype = $wgRequest->getText('datatype');
		
		if ($datatype == 'editcount')
		{
			$querytext = "SELECT `weha_rev_page`, COUNT(`weha_rev_id`) AS sigsum FROM `weha_revision` WHERE `weha_rev_user_text` LIKE '$target' GROUP BY `weha_rev_page` ORDER BY sigsum DESC LIMIT 0, 15";
			$sum_value = $edit_count;
		}
		elseif ($datatype == 'tokencount')
		{
			$querytext = "SELECT `weha_rev_page`, SUM(`weha_rev_token_affected`) AS sigsum FROM `weha_revision` WHERE `weha_rev_user_text` LIKE '$target' GROUP BY `weha_rev_page` ORDER BY sigsum DESC LIMIT 0, 15";
			$sum_value = $token_count;
		}
		else
		{
			$querytext = "SELECT `weha_rev_page`, SUM(`weha_rev_significance`) AS sigsum FROM `weha_revision` WHERE `weha_rev_user_text` LIKE '$target' GROUP BY `weha_rev_page` ORDER BY sigsum DESC LIMIT 0, 15";
			$sum_value = $sig_sum;
		}
		
		$result = $dbr->query($querytext);
			
		if ($datatype == 'editcount')
		{
			$haxis_style = "title: 'Edit count', logScale: false";
			$datatype_text = 'Edit count';
		}
		elseif ($datatype == 'tokencount')
		{
			$haxis_style = "title: 'Token count', logScale: false";
			$datatype_text = 'Affected token count';
		}
		else
		{
			$haxis_style = "title: 'Edit significance (log scale)', logScale: true";
			$datatype_text = 'Edit significance';
		}		

		$data_script = <<<"EOD0"
		<script type="text/javascript">
			google.load("visualization", "1", {packages:["corechart"]});
			google.setOnLoadCallback(drawBarChart);
			function drawBarChart() {
				var data = new google.visualization.DataTable();
				data.addColumn('string', 'Article');
				data.addColumn('number', '$datatype_text');

EOD0;
		$data_count = 0;
		$data_script_value = '';
		foreach( $result as $row )
		{
			if (doubleval($row->sigsum) > 1.0)
			{
				$data_script_value .= "data.setValue($data_count, 0, '" . addslashes(Title::newFromID($row->weha_rev_page)) . "');\n";
				$data_script_value .= "data.setValue($data_count, 1, " . doubleval($row->sigsum) . ");\n";
				$data_count++;
			}
		}

		$data_script .= "data.addRows($data_count);\n";
		$data_script .= $data_script_value;
		$datatype_text = strtolower($datatype_text);
		$data_script .= <<<"EOD1"
	        var barchart = new google.visualization.BarChart(document.getElementById('chart_div'));
	        barchart.draw(data, {width: 540, height: 400, title: 'Top $data_count articles by $datatype_text', legend: 'none',
	                          vAxis: {title: 'Article'}, 
	                          hAxis: {{$haxis_style}}
	                         });
	        var chart_selector = document.getElementById('chart_selector_div');
	        chart_selector.style.display = 'block';
        	var data_type_selector = document.getElementById('data_type_selector_div');
	        data_type_selector.style.display = 'block';
	        var barchart_radio = document.getElementById('barchart_radio');
	        barchart_radio.checked = true;
	        
			}
	    </script>

EOD1;

		$wgOut->addScript($data_script);
		
		$result = $dbr->query($querytext);
		
		$data_script = <<<"EOD0"
		<script type="text/javascript">
			function drawPieChart() {
				var data = new google.visualization.DataTable();
				data.addColumn('string', 'Article');
				data.addColumn('number', 'Edit Significance');

EOD0;
		$data_count = 0;
		$data_script_value = '';
		$others_value = $sum_value;
		
		foreach( $result as $row )
		{
			if (doubleval($row->sigsum) > $sum_value / 720.0)
			{
				$data_script_value .= "data.setValue($data_count, 0, '" . addslashes(Title::newFromID($row->weha_rev_page)) . "');\n";
				$data_script_value .= "data.setValue($data_count, 1, " . doubleval($row->sigsum) . ");\n";
				$others_value -= doubleval($row->sigsum);
				$data_count++;
			}
		}
		$data_script_value .= "data.setValue($data_count, 0, '(other pages)');\n";
		$data_script_value .= "data.setValue($data_count, 1, " . $sum_value . ");\n";
		$data_count++;
		
		$data_script .= "data.addRows($data_count);\n";
		$data_script .= $data_script_value;
		$data_count--;
		$data_script .= <<<"EOD1"
	        var piechart = new google.visualization.PieChart(document.getElementById('chart_div'));
	        piechart.draw(data, {width: 540, height: 400, title: 'Top $data_count articles by edit significance'});
			}
	    </script>

EOD1;
		
	    $wgOut->addScript($data_script);
	    
		$result = $dbr->query($querytext);
		
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
			if (doubleval($row->sigsum) > 1.0)
			{
				$pid = $row->weha_rev_page;
				$breakdown_arr[$pid] = array();
				$breakdown_arr[$pid]['Total'] = 0;
				
				if ($datatype == 'tokencount') 
					$rev_querytext = "SELECT `weha_rev_id`, `weha_rev_token_count` AS weha_rev_action_count FROM `weha_revision` WHERE `weha_rev_user_text` LIKE '$target' AND `weha_rev_page` = $pid;";
				else
					$rev_querytext = "SELECT `weha_rev_id`, `weha_rev_action_count` FROM `weha_revision` WHERE `weha_rev_user_text` LIKE '$target' AND `weha_rev_page` = $pid;";
				
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

						if (isset($breakdown_arr[$pid][$arr_key])) $breakdown_arr[$pid][$arr_key] += doubleval($arr_value);
						else $breakdown_arr[$pid][$arr_key] = doubleval($arr_value);
						$breakdown_arr[$pid]['Total'] += doubleval($arr_value);
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
		
		$breakdown_pid = array_keys($breakdown_arr);
		foreach($breakdown_pid as $pid)
		{
			$data_script_value .= "data.setValue($data_count, 0, '" . addslashes(Title::newFromID($pid)) . "');\n";
			$total = $breakdown_arr[$pid]['Total'];
			$cat_count = 1;
			foreach($category_arr as $cat)
			{
				if (strcmp($cat, 'Uncategorized') == 0)
					continue;
				elseif (isset($breakdown_arr[$pid][$cat]))
					$data_script_value .= "data.setValue($data_count, $cat_count, " . $breakdown_arr[$pid][$cat] / max(doubleval($total), 0.01) . ");\n";
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
	        breakdownchart.draw(data, {width: 540, height: 400, isStacked: true, 
	        						   chartArea: {width: "45%"}, 
	        						   title: 'Top $data_count articles\' break down by edit type',
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
<div id="data_type_selector_div" style="display: none;"><form action="./Special:WehaContributions" method="get">' . 
    		'<input type="radio" name="datatype" value="significance" checked="checked" /> Edit significance ' . 
    		'<input type="radio" name="datatype" value="editcount" /> Edit count ' . 
    		'<input type="radio" name="datatype" value="tokencount" /> Token count ' . 
        	'<input type="hidden" name="target" value="' . $target . '" />' . 
    		//'<input type="submit" value="Change" />' . 
        	'</form></div>
</td>
</tr>
</table>');
	    $wgOut->addHTML('<div id="chart_div" style="display: inline;"></div>');
	    $wgOut->addHTML('<div id="breakdown_chart_div" style="display: inline;"></div>');

	    $wgOut->addHTML('<div><b>Edit count:</b> ' . $edit_count . '<br />');
		$wgOut->addHTML('<b>Total affected tokens:</b> ' . $token_count . '<br />');
		$wgOut->addHTML('<b>Total edit significance:</b> ' . sprintf('%.2f', $sig_sum) . '<br />');
		$wgOut->addHTML('<b>Maximum edit significance:</b> ' . sprintf('%.2f', $sig_max) . '<br />');
		$wgOut->addHTML('<b>Average edit significance (per page):</b> ' . sprintf('%.2f', ($sig_sum / $page_count) ) . '<br />');
		$wgOut->addHTML('<b>Average edit significance (per revision):</b> ' . sprintf('%.2f', $sig_avg) . '</div><br />');
	}
	
	protected function getForm() {
		
		$usernameForm = <<<"FORM"
			<form action="./Special:WehaContributions" method="get">
			Username: <input type="text" name="target" />
			<input type="submit" value="Query user" />
			</form>

FORM;

		return $usernameForm;
	}
}
