<?php
require_once('/var/lib/tomcat6/webapps/JavaBridge/java/Java.inc');

class WikiDiffFormatter {
	
	private $output;
	private $basicEdits;
	private $sentenceEdits;
	private $tokenOld;
	private $tokenNew;
	
	public function __construct($be, $se, $to, $tn)
	{
		$this->output = '';
		$this->basicEdits = java_values($be);
		$this->sentenceEdits = $se;
		$this->tokenOld = java_values($to);
		$this->tokenNew = java_values($tn);
	}
	
	public function outputDiff()
	{		
		$sDelPosition = array();
		$sInsPosition = array();
		$sMoveOldPosition = array();
		$sMoveNewPosition = array();
		$sChangeOldPosition = array();
		$sChangeNewPosition = array();
		$sMergeOldPosition = array();
		$sMergeNewPosition = array();
		$sSplitOldPosition = array();
		$sSplitNewPosition = array();
		
		$delPosition = array();
		$insPosition = array();
		$movOldPosition = array();
		$movNewPosition = array();

		for ($i = 0; $i < java_values($this->sentenceEdits->size()); $i++)
		{
			$se = $this->sentenceEdits->get($i);
			if (java_instanceof($se, java('mo.umac.wikianalysis.diff.sentence.SentenceDel'))) {
				$sDelPosition[java_values( $se->getOldStartPos() )] = java_values( $se->getOldEndPos() );
			}			
			elseif (java_instanceof($se, java('mo.umac.wikianalysis.diff.sentence.SentenceIns'))) {
				$sInsPosition[java_values( $se->getNewStartPos() )] = java_values( $se->getNewEndPos() );				
			}
			elseif (java_instanceof($se, java('mo.umac.wikianalysis.diff.sentence.SentenceMove'))) {
				$sMoveOldPosition[java_values( $se->getOldStartPos() )] = java_values( $se->getOldEndPos() );
				$sMoveNewPosition[java_values( $se->getNewStartPos() )] = java_values( $se->getNewEndPos() );			
			}
			elseif (java_instanceof($se, java('mo.umac.wikianalysis.diff.sentence.SentenceMatch'))) {
				if (java_values( $se->getMatchingRate() ) < 1.0)
				{
					$sChangeOldPosition[java_values( $se->getOldStartPos() )] = java_values( $se->getOldEndPos() );
					$sChangeNewPosition[java_values( $se->getNewStartPos() )] = java_values( $se->getNewEndPos() );			
				}
			}
			elseif (java_instanceof($se, java('mo.umac.wikianalysis.diff.sentence.SentenceMerge'))) {
				$sMergeOldPosition[java_values( $se->getOldStartPos() )] = java_values( $se->getOldEndPos() );
				$sMergeNewPosition[java_values( $se->getNewStartPos() )] = java_values( $se->getNewEndPos() );			
			}
			elseif (java_instanceof($se, java('mo.umac.wikianalysis.diff.sentence.SentenceSplit'))) {
				$sSplitOldPosition[java_values( $se->getOldStartPos() )] = java_values( $se->getOldEndPos() );
				$sSplitNewPosition[java_values( $se->getNewStartPos() )] = java_values( $se->getNewEndPos() );			
			}
		}
		
		for ($i = 0; $i < count($this->basicEdits); $i++)
		{
			$e = $this->basicEdits[$i];
			if (java_instanceof($e, java('mo.umac.wikianalysis.diff.token.Deletion'))) {
				$delStartPos = java_values( $e->getPos() );
				$delEndPos = $delStartPos + java_values( $e->getLength() ) - 1;
				$delPosition[$delStartPos] = $delEndPos;
			} elseif (java_instanceof($e, java('mo.umac.wikianalysis.diff.token.Insertion'))) {
				$insStartPos = java_values( $e->getPos() );
				$insEndPos = $insStartPos + java_values( $e->getLength() ) - 1;
				$insPosition[$insStartPos] = $insEndPos;
			} elseif (java_instanceof($e, java('mo.umac.wikianalysis.diff.token.Replacement'))) {
				$delStartPos = java_values( $e->getOldPos() );
				$delEndPos = $delStartPos + java_values( $e->getDeletedLength() ) - 1;
				$delPosition[$delStartPos] = $delEndPos;
					
				$insStartPos = java_values( $e->getNewPos() );
				$insEndPos = $insStartPos + java_values( $e->getInsertedLength() ) - 1;
				$insPosition[$insStartPos] = $insEndPos;
			} elseif (java_instanceof($e, java('mo.umac.wikianalysis.diff.token.Movement'))) {
				$movOldStartPos = java_values( $e->getOldPos() );
				$movNewStartPos = java_values( $e->getNewPos() );
				$movLen = java_values( $e->getLength() );
				
				$movOldPosition[$movOldStartPos] = $movOldStartPos + $movLen - 1;
				$movNewPosition[$movNewStartPos] = $movNewStartPos + $movLen - 1;
			}
		}
		
		$this->output .= ("<table class='weha-diff'>\n");
		$this->output .= ("<col class='weha-diff-content' />\n");
		$this->output .= ("<col class='weha-diff-content' />\n");
		$this->output .= ("<tr><th>Old version</th><th>New version</th></tr>\n");
		$this->output .= ("<tr><td><div>\n");
		
		$delEnd = -1;
		$sDelEnd = -1;
		$firstToken = false;
		$delTagClosed = true;
		$movTagClosed = true;
		$sTagClosed = true;
		
		for($i = 0; $i < count($this->tokenOld); $i++)
		{
			$tt = $this->tokenOld[$i];
			
			if (array_key_exists($i, $sDelPosition))
			{
				if (!$delTagClosed || !$movTagClosed) $this->output .= ("</span>");
				if (!$sTagClosed) $this->output .= ("</span>");
				$this->output .= ("<span class='weha-diff-deletedsentence'>");
				$sTagClosed = false;
				$sDelEnd = $sDelPosition[$i];
				if (!$delTagClosed) $this->output .= ("<span class='weha-diff-deleted'>");
				if (!$movTagClosed) $this->output .= ("<span class='weha-diff-moved'>");
			}
			elseif (array_key_exists($i, $sMoveOldPosition))
			{
				if (!$delTagClosed || !$movTagClosed) $this->output .= ("</span>");
				if (!$sTagClosed) $this->output .= ("</span>");
				$this->output .= ("<span class='weha-diff-movedsentence'>");
				$sTagClosed = false;
				$sDelEnd = $sMoveOldPosition[$i];
				if (!$delTagClosed) $this->output .= ("<span class='weha-diff-deleted'>");
				if (!$movTagClosed) $this->output .= ("<span class='weha-diff-moved'>");
			}
			elseif (array_key_exists($i, $sChangeOldPosition))
			{
				if (!$delTagClosed || !$movTagClosed) $this->output .= ("</span>");
				if (!$sTagClosed) $this->output .= ("</span>");
				if (array_key_exists($i, $sSplitOldPosition) || array_key_exists($i, $sMergeOldPosition))
					$this->output .= ("<span class='weha-diff-mergedsentence'>");
				else
					$this->output .= ("<span class='weha-diff-changedsentence'>");
				$sTagClosed = false;
				$sDelEnd = $sChangeOldPosition[$i];
				if (!$delTagClosed) $this->output .= ("<span class='weha-diff-deleted'>");
				if (!$movTagClosed) $this->output .= ("<span class='weha-diff-moved'>");
			}
			
			if (array_key_exists($i, $delPosition))
			{
				if (!$delTagClosed || !$movTagClosed) $this->output .= ("</span>");
				$firstToken = true;
				$this->output .= ("<span class='weha-diff-deleted'>");
				$this->output .= ("<a class='first-token' name='o" . $i . "'>");
				$delTagClosed = false;
				$delEnd = $delPosition[$i];
			}
			elseif (array_key_exists($i, $movOldPosition))
			{
				if (!$delTagClosed || !$movTagClosed) $this->output .= ("</span>");
				$firstToken = true;
				$this->output .= ("<span class='weha-diff-moved'>");
				$this->output .= ("<a class='first-token' name='o" . $i . "'>");
				$movTagClosed = false;
				$delEnd = $movOldPosition[$i];
			}

			$this->output .= nl2br(htmlspecialchars($tt->toString()));
			
			if ($firstToken)
			{
				$this->output .= "</a>";
				$firstToken = false;
			}
			if ($i == $delEnd)
			{
				$this->output .= ("</span>");
				$delTagClosed = true;
				$movTagClosed = true;
			}
			if ($i == $sDelEnd)
			{
				if (!$delTagClosed || !$movTagClosed) $this->output .= ("</span>");
				$this->output .= ("</span>");
				$sTagClosed = true;
				if (!$delTagClosed) $this->output .= ("<span class='weha-diff-deleted'>");
				if (!$movTagClosed) $this->output .= ("<span class='weha-diff-moved'>");
			}
		}

		if (!$delTagClosed || !$movTagClosed) $this->output .= ("</span>");
		if (!$sTagClosed) $this->output .= ("</span>");
		
		$this->output .= ("</div></td>\n<td><div>\n");
		
		$insEnd = -1;
		$sInsEnd = -1;
		$insTagClosed = true;
		$movTagClosed = true;
		$sTagClosed = true;

		for($i = 0; $i < count($this->tokenNew); $i++)
		{
			$tt = $this->tokenNew[$i];
			
			if (array_key_exists($i, $sInsPosition))
			{
				if (!$insTagClosed || !$movTagClosed) $this->output .= ("</span>");
				if (!$sTagClosed) $this->output .= ("</span>");
				$this->output .= ("<span class='weha-diff-addedsentence'>");
				$sTagClosed = false;
				$sInsEnd = $sInsPosition[$i];
				if (!$insTagClosed) $this->output .= ("<span class='weha-diff-inserted'>");
				if (!$movTagClosed) $this->output .= ("<span class='weha-diff-moved'>");
			}
			elseif (array_key_exists($i, $sMoveNewPosition))
			{
				if (!$insTagClosed || !$movTagClosed) $this->output .= ("</span>");
				if (!$sTagClosed) $this->output .= ("</span>");
				$this->output .= ("<span class='weha-diff-movedsentence'>");
				$sTagClosed = false;
				$sInsEnd = $sMoveNewPosition[$i];
				if (!$insTagClosed) $this->output .= ("<span class='weha-diff-inserted'>");
				if (!$movTagClosed) $this->output .= ("<span class='weha-diff-moved'>");
			}
			elseif (array_key_exists($i, $sChangeNewPosition))
			{
				if (!$insTagClosed || !$movTagClosed) $this->output .= ("</span>");
				if (!$sTagClosed) $this->output .= ("</span>");
				if (array_key_exists($i, $sSplitNewPosition) || array_key_exists($i, $sMergeNewPosition))
					$this->output .= ("<span class='weha-diff-mergedsentence'>");
				else
					$this->output .= ("<span class='weha-diff-changedsentence'>");
				$sTagClosed = false;
				$sInsEnd = $sChangeNewPosition[$i];
				if (!$insTagClosed) $this->output .= ("<span class='weha-diff-inserted'>");
				if (!$movTagClosed) $this->output .= ("<span class='weha-diff-moved'>");
			}
			
			if (array_key_exists($i, $insPosition))
			{
				if (!$insTagClosed || !$movTagClosed) $this->output .= ("</span>");
				$firstToken = true;
				$this->output .= ("<span class='weha-diff-inserted'>");
				$this->output .= ("<a class='first-token' name='n" . $i . "'>");
				$insTagClosed = false;
				$insEnd = $insPosition[$i];
			}
			elseif (array_key_exists($i, $movNewPosition))
			{
				if (!$insTagClosed || !$movTagClosed) $this->output .= ("</span>");
				$firstToken = true;
				$this->output .= ("<span class='weha-diff-moved'>");
				$this->output .= ("<a class='first-token' name='n" . $i . "'>");
				$movTagClosed = false;
				$insEnd = $movNewPosition[$i];
			}
			$this->output .= nl2br(htmlspecialchars($tt->toString()));
			
			if ($firstToken)
			{
				$this->output .= "</a>";
				$firstToken = false;
			}
			if ($i == $insEnd)
			{
				$this->output .= ("</span>");
				$insTagClosed = true;
				$movTagClosed = true;
			}			
			if ($i == $sInsEnd)
			{
				if (!$insTagClosed || !$movTagClosed) $this->output .= ("</span>");
				$this->output .= ("</span>");
				$sTagClosed = true;
				if (!$insTagClosed) $this->output .= ("<span class='weha-diff-inserted'>");
				if (!$movTagClosed) $this->output .= ("<span class='weha-diff-moved'>");
			}
		}
		
		if (!$insTagClosed || !$movTagClosed) $this->output .= ("</span>");
		if (!$sTagClosed) $this->output .= ("</span>");
		$this->output .= ("</div></td>\n</table>");
		
		return $this->output;
	}
	
}
