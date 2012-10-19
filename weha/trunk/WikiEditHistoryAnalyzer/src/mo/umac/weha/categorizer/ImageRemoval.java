package mo.umac.weha.categorizer;

import java.util.HashSet;

import mo.umac.weha.diff.AbstractEdit;

public class ImageRemoval extends AbstractSingleLinkRemoval {

	{
		weight = 1.0;
	}
	
	public ImageRemoval() {
		this.basicEdits = null;
		this.prefixes = new HashSet<String>();
		this.prefixes.add("Image".toLowerCase());
		this.prefixes.add("File".toLowerCase());
	}
	
	public ImageRemoval(AbstractEdit be) {
		this.basicEdits = new AbstractEdit[1];
		this.basicEdits[0] = be;
	}
	
}
