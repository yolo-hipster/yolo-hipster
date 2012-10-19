package mo.umac.weha.categorizer;

import java.util.HashSet;

import mo.umac.weha.diff.AbstractEdit;

public class AssignCategory extends AbstractSingleLinkAddition {

	{
		weight = 0.5;
	}
	
	public AssignCategory() {
		this.basicEdits = null;
		this.prefixes = new HashSet<String>();
		this.prefixes.add("Category".toLowerCase());
	}
	
	public AssignCategory(AbstractEdit be) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = be;
	}
	
}
