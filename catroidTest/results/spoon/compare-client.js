jQuery(function($) {
	var diffed = false;

	$("#diff").click(function() {
		if (diffed) {
			$("tr.result .test").css("display", "table-cell");
		} else {
			$("tr.result .test").not(".diff").css("display", "none");
		}
		diffed = !diffed;
	});
});