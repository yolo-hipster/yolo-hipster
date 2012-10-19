package mo.umac.weha.diff.formatter;

import java.util.TreeSet;

import mo.umac.weha.data.Paragraph;
import mo.umac.weha.diff.paragraph.ParagraphEdit;

public class ParagraphsPair {
	public ParagraphEdit paraEdit;
	public TreeSet<Paragraph> oldParas;
	public TreeSet<Paragraph> newParas;
	
	public int oldPos;
	public int newPos;
	
	public ParagraphsPair(ParagraphEdit pe, TreeSet<Paragraph> oldP, TreeSet<Paragraph> newP) {
		this.paraEdit = pe;
		this.oldParas = oldP;
		this.newParas = newP;
		
		this.oldPos = oldP.isEmpty() ? -1 : oldP.first().getPosition();
		this.newPos = newP.isEmpty() ? -1 : newP.first().getPosition();
	}
	
}
