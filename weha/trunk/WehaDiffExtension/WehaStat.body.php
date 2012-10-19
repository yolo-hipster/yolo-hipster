<?php

class WehaStat {

	public function setup() {
		new WehaStat;
		return true;
	}

	public function __construct() {
		global $wgHooks;
		 
		$wgHooks['SkinTemplateNavigation'][] = $this;
		$wgHooks['UnknownAction'][] = $this;
	}
	
	public function onSkinTemplateNavigation( &$sktemplate, &$links ) {
		global $wgRequest;
		
		$title = $sktemplate->getTitle();
		
		/* Only add tab when the page is an article in main namespce. */
		if ( $title->getNamespace() == 0 ) {
			$action = $wgRequest->getText( 'action' );
			
			$links['actions']['wehastat'] = array(
	    		'class' => ($action == 'wehastat') ? 'selected' : false,
	    		'text' => 'Edit Statistics', //wfMsg('weha_tabname'),
	    		'href' => $title->getLocalURL( 'action=wehastat' )
			);
		}
		return true;
	}
	
	public function onUnknownAction( $action, $article ) {
		global $wgOut, $wgScriptPath, $wgRequest;
		
		if ($action == 'wehastat') {
			$wgOut->setPageTitle( 'Edit statistics of "'. $article->getTitle() .'"' );
			$wgOut->setRobotpolicy( 'noindex,nofollow' );
			$wgOut->setArticleRelated( false );
			
			$wgOut->addScript('<script type="text/javascript" src="https://www.google.com/jsapi"></script>');
			
			$article_id = $article->getID();
			
			$dbr = wfGetDB( DB_SLAVE );
			
			// Query basic statistics from DB
			$statQuery = $dbr->selectRow( array( 'weha_revision', 'revision' ),
				 array( 'COUNT(*) AS editcount', 
				 	    'SUM(weha_rev_significance) AS sigsum',
				 	    'MAX(weha_rev_significance) AS sigmax',
				 	    'AVG(weha_rev_significance) AS sigavg' ),
				 array( 'rev_page' => $article_id ),
				 __METHOD__, array(),
				 array( 'revision' => array( 'LEFT JOIN', 'weha_rev_id = rev_id' ) ) );
			
			$sig_sum = doubleval($statQuery->sigsum);
			$sig_max = doubleval($statQuery->sigmax);
			$sig_avg = doubleval($statQuery->sigavg);
			$edit_count = intval($statQuery->editcount);
			
			$userCountQuery = $dbr->selectRow( array( 'weha_revision', 'revision' ),
				array( 'COUNT(DISTINCT rev_user_text) AS usercount' ),
				array( 'rev_page' => $article_id ),
				__METHOD__, array(),
				array( 'revision' => array( 'LEFT JOIN', 'weha_rev_id = rev_id' ) ) );
			
			$user_count = intval($userCountQuery->usercount);
			
			// Query corresponding data type based on selection
			$topUserQueryCols = array( 'rev_user_text' );
			$topUserQueryCols[] = 'COUNT(rev_user_text) AS editcount';
			$topUserQueryCols[] = 'SUM(weha_rev_significance) AS sigsum';
			
			$topUserQuery = $dbr->select( array( 'weha_revision', 'revision' ),
				$topUserQueryCols, 
				array( 'rev_page' => $article_id ),
				__METHOD__,
				array( 'GROUP BY' => 'rev_user_text',
					   'ORDER BY' => 'sigsum DESC',
					   'LIMIT' => 9 ),
				array( 'revision' => array( 'LEFT JOIN', 'weha_rev_id = rev_id' ) ) );
			
			// Generate JavaScript code calling Google Chart API
			// for pie chart and bar chart creation
			$data_script = <<<"EOD0"
			<script type="text/javascript">
				google.load("visualization", "1", {packages:["corechart"]});
				google.setOnLoadCallback(initStatData);
				
				var wehaStatData, wehaEditCountData;
				
				function initStatData() {
					wehaStatData = new google.visualization.DataTable();
					wehaStatData.addColumn('string', 'User name');
					wehaStatData.addColumn('number', 'Edit significance');
					
					wehaEditCountData = new google.visualization.DataTable();
					wehaEditCountData.addColumn('string', 'User name');
					wehaEditCountData.addColumn('number', 'Edit count');

EOD0;

			$data_script_value = '';
			$others_sig_value = $sig_sum;
			$others_edit_count = $edit_count;
			
			foreach( $topUserQuery as $row ) {
				if ( doubleval($row->sigsum) > $sig_sum / 720.0 ) {
					$data_script_value .= "wehaStatData.addRow(['" . addslashes($row->rev_user_text) . "', " .  doubleval($row->sigsum) . "]);\n";
					$data_script_value .= "wehaEditCountData.addRow(['" . addslashes($row->rev_user_text) . "', " .  intval($row->editcount) . "]);\n";
					$others_sig_value -= doubleval($row->sigsum);
					$others_edit_count -= intval($row->editcount);
				}
			}
			
			$data_script_value .= "wehaStatData.addRow(['(other users)', " .  doubleval($others_value) . "]);\n";
			$data_script_value .= "wehaEditCountData.addRow(['(other users)', " .  intval($others_edit_count) . "]);\n";
			
			$data_script .= $data_script_value;
			$data_script .= "drawChart();\n";
			$data_script .= "document.getElementById('weha_table_selectors').style.display = 'block'; }\n";
			
			$data_script .= <<<"EOD1"
			function drawChart() {
				var data, hAxisStyle;
				
				if ( document.getElementById('weha_radio_significance').checked ) {
					data = wehaStatData;
					hAxisStyle = {title: 'Edit significance (log scale)', logScale: true};
				}
				else {
					data = wehaEditCountData;
					hAxisStyle = {title: 'Edit count', logScale: false};
				}
				
				if ( document.getElementById('weha_radio_barchart').checked ) {
					drawBarChart( data, hAxisStyle );
				}
				else {
					drawPieChart( data );
				}
			}
			
			function drawPieChart( data ) {
		        var piechart = new google.visualization.PieChart(document.getElementById('weha_div_chart'));
		        piechart.draw(data, {width: 520, height: 320, title: 'User contributions'});
			}
			
			function drawBarChart( data, hAxisStyle ) {
				var barchart = new google.visualization.BarChart(document.getElementById('weha_div_chart'));
		        barchart.draw(data, {width: 520, height: 320, title: 'User contributions', legend: 'none',
	                          vAxis: {title: 'User'}, 
	                          hAxis: hAxisStyle
	                         });
			}
		    </script>
EOD1;
			$wgOut->addScript($data_script);
			
		    // Query for user contribution break down chart
			$topUserQuery->rewind();
			
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
			
			foreach( $topUserQuery as $row ) {
				if ( doubleval($row->sigsum) > $sig_sum / 720.0 )	{
					$uid = $row->rev_user_text;
					$breakdown_arr[$uid] = array();
					$breakdown_arr[$uid]['Total'] = 0;
					
					$rev_result = $dbr->select( array( 'weha_revision', 'revision' ),
						array( 'weha_rev_id', 'weha_rev_action_count' ),
						array( 'rev_page' => $article_id,
							   'rev_user_text' => $uid ),
						__METHOD__, array(),
						array( 'revision' => array( 'LEFT JOIN', 'weha_rev_id = rev_id' ) )  );
					
					foreach( $rev_result as $rev_row ) {
						$result_plaintext = $rev_row->weha_rev_action_count;

						if ( is_null($result_plaintext) || empty($result_plaintext) ) {
							continue;
						}
						
						$result_arr = json_decode( $result_plaintext, TRUE );
						
						while ( list($arr_key, $arr_value) = each($result_arr) ) {
							if ( strcmp($arr_key, 'Uncategorized') == 0 ) {
								continue;
							}
							
							if ( isset($breakdown_arr[$uid][$arr_key]) ) {
								$breakdown_arr[$uid][$arr_key] += doubleval($arr_value);
							}
							else {
								$breakdown_arr[$uid][$arr_key] = doubleval($arr_value);
							}
							$breakdown_arr[$uid]['Total'] += doubleval($arr_value);
						}
					}
				}
			}
			
			$data_script .=	"data.addColumn('string', 'Article');\n";
			
			$category_arr[] = 'EditorialNotice';
			$category_arr[] = 'AssignCategory';
			$category_arr[] = 'UnassignCategory';
			$category_arr[] = 'Interwiki';
			$category_arr[] = 'Wikify';
			$category_arr[] = 'Dewikify';
			$category_arr[] = 'PunctuationChange';
			$category_arr[] = 'TypoChange';
			$category_arr[] = 'ImageAddition';
			$category_arr[] = 'ImageRemoval';
			$category_arr[] = 'CiteExistingReference';
			$category_arr[] = 'NewReference';
			$category_arr[] = 'ContentSubstitution';
			$category_arr[] = 'ContentRemoval';
			$category_arr[] = 'ContentAddition';
			$category_arr[] = 'ContentMovement';
			sort($category_arr);
			
			foreach( $category_arr as $cat ) {
				if (strcmp($cat, 'Uncategorized') == 0) continue;
				$data_script .=	"data.addColumn('number', '$cat');\n";
			}
			
			$breakdown_uid = array_keys($breakdown_arr);
			foreach( $breakdown_uid as $uid ) {
				$data_script_value .= "data.setValue($data_count, 0, '" . addslashes($uid) . "');\n";
				$total = $breakdown_arr[$uid]['Total'];
				$cat_count = 1;
				foreach( $category_arr as $cat ) {
					if ( strcmp($cat, 'Uncategorized') == 0 ) {
						continue;
					}
					elseif ( isset($breakdown_arr[$uid][$cat]) ) {
						$data_script_value .= "data.setValue($data_count, $cat_count, " . $breakdown_arr[$uid][$cat] / max(doubleval($total), 0.01) . ");\n";
						$data_script_value .= "data.setFormattedValue($data_count, $cat_count, '" . sprintf('%.1f', $breakdown_arr[$uid][$cat]) . " (" . sprintf('%.0f', $breakdown_arr[$uid][$cat] / max(doubleval($total), 0.01) * 100 ) . "%)');\n";
					}
					else {
						$data_script_value .= "data.setValue($data_count, $cat_count, 0.0);\n";
						$data_script_value .= "data.setFormattedValue($data_count, $cat_count, '0.0 (0%)');\n";
					}
					$cat_count++;
				}
				$data_count++;
			}
			
			$data_script .= "data.addRows($data_count);\n";
			$data_script .= $data_script_value;
			$data_script .= <<<"EOD1"
		        var breakdownchart = new google.visualization.BarChart(document.getElementById('weha_div_breakdown_chart'));
		        breakdownchart.draw(data, {width: 540, height: 320, isStacked: true, 
		        						   title: 'User contribution break down by edit type', 
		        						   colors: ['#000080','#00A86B','green','#007FFF','red','#FFA700','#800000','#808000','#66FF33','#FF6633','#004953','#00A86B','#FFBF00','#E30B5C','#003300','#EEEE00'],
		        						   chartArea: {width: "45%"}, 
										   hAxis: {format: '#.##%', minValue: 0.0, maxValue: 1.0}
										  });
				}
		    </script>
	
EOD1;
			$wgOut->addScript($data_script);
			
			// Add controls
			$wgOut->addHTML('<table id="weha_table_selectors" cellspacing="5" border="0" cellpadding="0" style="display: none;">
				<tr valign="top" align="left">
				<td width="180"><form>' . 
				'<input type="radio" name="weha_chart_type" id="weha_radio_barchart" onClick="drawChart();" /> Bar Chart' . 
				'<input type="radio" name="weha_chart_type" id="weha_radio_piechart" checked="checked" onClick="drawChart();" /> Pie Chart' . 
				'</form></td>
				<td width="2" bgcolor="#666666"><br /></td>
				<td width="600" valign="top" align="left"><form>' . 
				'<input type="radio" name="weha_data_type" id="weha_radio_significance" checked="checked" onClick="drawChart();" /> Edit significance ' . 
				'<input type="radio" name="weha_data_type" id="weha_radio_editcount" onClick="drawChart();" /> Edit count ' . 
				'</form></td>
				</tr>
				</table>');
			
		    // Output charts
			$wgOut->addHTML('<div id="weha_div_chart" style="display:inline-block;"></div>');
			$wgOut->addHTML('<div id="weha_div_breakdown_chart" style="display:inline-block;"></div><br />');
			
			$wgOut->addHTML('<div><b>Edit count:</b> ' . $edit_count . '<br />');
			$wgOut->addHTML('<b>Total edit significance:</b> ' . sprintf('%.2f', $sig_sum) . '<br />');
			$wgOut->addHTML('<b>Maximum edit significance:</b> ' . sprintf('%.2f', $sig_max) . '<br />');
			$wgOut->addHTML('<b>Average edit significance (per user):</b> ' . sprintf('%.2f', ($sig_sum / $user_count)) . '<br />');
			$wgOut->addHTML('<b>Average edit significance (per revision):</b> ' . sprintf('%.2f', $sig_avg) . '</div><br />');
			
			return false;
		}
		
		return true;
	}
	
}