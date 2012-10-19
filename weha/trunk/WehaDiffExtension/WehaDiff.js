jQuery(document).ready(function() {
	jQuery('#bodyContent').localScroll();
	
	jQuery('a.weha-diff-anchor[href]').click(function () {
		jQuery('a.weha-diff-anchor').removeClass('weha-diff-selected');
		
		var hrefString = jQuery(this).attr('href');
		jQuery(this).addClass('weha-diff-selected');
		jQuery(hrefString).addClass('weha-diff-selected');
	});
	
	jQuery('a.weha-diff-link').click(function () {
		jQuery('a.weha-diff-anchor').removeClass('weha-diff-selected');
		
		var hrefString = jQuery(this).attr('href');
		jQuery(hrefString).addClass('weha-diff-selected');
		
		var href2String = jQuery(hrefString).attr('href');
		if (typeof(href2String) != 'undefined') {
			jQuery(href2String).addClass('weha-diff-selected');
		}
	});
 });

function toggleExpand(name) {
	var obj = jQuery("#" + name);
	
	if (obj.is(":hidden")) {
		obj.slideDown('fast');
	}
	else {
		obj.slideUp('fast');
	}
}