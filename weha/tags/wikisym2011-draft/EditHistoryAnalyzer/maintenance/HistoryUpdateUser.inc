<?php
/**
 * @file
 * @ingroup Maintenance
 */

function HistoryUpdateUser() {
	
	$dbr = wfGetDB(DB_SLAVE);	
	$dbw = wfGetDB(DB_MASTER);
	
	$all_users = $dbr->select('revision', array('DISTINCT rev_user_text AS user_name'));
	$all_users_arr = array();
	
	foreach( $all_users as $row )
		$all_users_arr[] = $row->user_name;
	
	foreach( $all_users_arr as $user_name )
	{
		if (empty($user_name)) continue;
		
		wfOut( "Total edit significance from \"{$user_name}\" = " );
		
		$user_name = addslashes($user_name);
		$user_exist = $dbr->select('weha_user', array('user_editsignificance'), "user_name = '$user_name'");
		if ($user_exist->numRows() > 0)
		{
			wfOut( "{$user_exist->fetchObject()->user_editsignificance} (cached)\n");
			continue;
		}
		else
		{
			$tbl_rev = $dbr->tableName('weha_revision');
			
			$sig_query = $dbr->select($tbl_rev, array('weha_rev_id', 'weha_rev_significance', 'weha_rev_action_count'), "weha_rev_user_text = '$user_name'");
			if ($sig_query->numRows() > 0)
			{
				$edit_count = 0;
				$total_sig = 0;
				$breakdown_arr = array();
				$category_arr = array();
				foreach ($sig_query as $sig_result)
				{
					$edit_count++;
					$total_sig += $sig_result->weha_rev_significance;
					$ac = $sig_result->weha_rev_action_count;
					
					if (is_numeric($ac))
					{
						$ac = "ContentAddition: $ac;\n";
						$dbw->begin();
						$dbw->update('weha_revision',
							array('weha_rev_action_count' => $ac), 
							array('weha_rev_id' => $sig_result->weha_rev_id));
						$dbw->commit();
					}
					
					$result_arr = explode( '\n', $ac );
					foreach( $result_arr as $element ) 
					{
						if (empty($element)) continue;
						list($arr_key, $arr_value) = explode(":", $element);
						if (array_search($arr_key, $category_arr) === false)
							$category_arr[] = $arr_key;
						if (isset($breakdown_arr[$arr_key]))
							$breakdown_arr[$arr_key] += intval($arr_value);
						else
							$breakdown_arr[$arr_key] = intval($arr_value);
					}
				}
				
				$breakdown_val = '';
				foreach($category_arr as $cat)
				{
					if (isset($breakdown_arr[$cat]))
						$breakdown_val .= "$cat: $breakdown_arr[$cat];\n";
				}
					
				$dbw->begin();
				$dbw->insert('weha_user',
					array('user_editcount' => $edit_count, 
						  'user_editsignificance' => $total_sig, 
						  'user_editbreakdown' => $breakdown_val, 
						  'user_name' => html_entity_decode(stripslashes($user_name))));
				$dbw->commit();
				
				wfOut( "$total_sig\n" );
			}
			else {
				$dbw->begin();
				$dbw->insert('weha_user',
					array('user_editcount' => 0, 
						  'user_editsignificance' => 0, 
						  'user_editbreakdown' => '',
						  'user_name' => html_entity_decode(stripslashes($user_name))));
				$dbw->commit();
				
				wfOut( "0 (no edit)\n" );
			}
		}
	}
	
}
