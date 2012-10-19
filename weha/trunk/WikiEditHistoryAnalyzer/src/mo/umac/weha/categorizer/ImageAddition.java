package mo.umac.weha.categorizer;

import java.util.HashSet;

import mo.umac.weha.diff.AbstractEdit;

public class ImageAddition extends AbstractSingleLinkAddition {

	{
		weight = 5.0;
	}
	
	public ImageAddition() {
		this.basicEdits = null;
		this.prefixes = new HashSet<String>();
		this.prefixes.add("Image".toLowerCase());
		this.prefixes.add("File".toLowerCase());
	}
	
	public ImageAddition(AbstractEdit be) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = be;
	}
	
}
