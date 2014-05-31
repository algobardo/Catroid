/* requires jsdom (npm install jsdom) */

/***** USAGE ******/

var fs = require("fs");
var utils = require("./compare-utils");

if (process.argv.length != 4)
	console.log("Usage: node compare.js <espresso-result> <robotium-result>");

var espressoLocation = process.argv[2];
var robotiumLocation = process.argv[3];

/***** LOAD FILES *****/

var espressoData, robotiumData;

utils.loadResource(espressoLocation, function(data) {
	espressoData = data;
	if (robotiumData)
		parse();
});
utils.loadResource(robotiumLocation, function(data) {
	robotiumData = data;
	if (espressoData)
		parse();
});

/***** PARSE FILES *****/

var espressoWindow, robotiumWindow;

var parse = function() {
	console.log("- Parsing files");
	utils.parseHTML(espressoData, function(window) {
		espressoWindow = window;
		if (robotiumWindow)
			joinHeaders();
	});
	utils.parseHTML(robotiumData, function(window) {
		robotiumWindow = window;
		if (espressoWindow)
			joinHeaders();
	});
};

/***** JOIN HEADERS *****/

var joinHeaders = function() {
	console.log("- Joining headers");

	var espressoHeader = espressoWindow.$("div.hero-unit p");
	var robotiumHeader = robotiumWindow.$("div.hero-unit p");

	espressoHeader.text("Espresso: " + espressoHeader.text());
	espressoHeader.after("<p>Robotium: " + robotiumHeader.text() + "</p>");

	joinDevices();
};

/***** JOIN DEVICES *****/

var joinDevices = function() {
	console.log("- Joining devices");
	
	/***** Extract devices ****/

	var espressoDevices = extractDevices(espressoWindow, espressoWindow.$);
	var robotiumDevices = extractDevices(robotiumWindow, robotiumWindow.$);

	/***** Join results device by device (modifies espressoWindow) *****/

	for (var i = 0; i < espressoDevices.length; i++) {
		var espressoDevice = espressoDevices[i];
		for (var j = 0; j < robotiumDevices.length; j++) {
			var robotiumDevice = robotiumDevices[j];
			if (espressoDevice.name == robotiumDevice.name) {
				joinConcreteDevices(espressoDevice, robotiumDevice);
				break;
			}
		}
	}

	addControls();
};

var extractDevices = function(window, $) {
	return $("tr.device").map(function(i, row) {
		row = $(row);
		return {
			name: row.find("a").text(),
			row: row
		};
	});
};

var joinConcreteDevices = function(espressoDevice, robotiumDevice) {
	console.log("    " + espressoDevice.name);

	var espressoResultRow = espressoDevice.row.next();
	var robotiumResultRow = robotiumDevice.row.next();
	espressoResultRow.after(robotiumResultRow[0].outerHTML);

	updateTooltips(espressoResultRow, espressoResultRow.next()); // .next() to get the robotiumResultRow in espressoWindow
};

var updateTooltips = function(espressoResultRow, robotiumResultRow) {
	var robotiumAnchors = robotiumResultRow.find("a"), robotiumAnchor = {};
	var j = 0;

	var nextRobotiumAnchor = function() {
		if (j < robotiumAnchors.length) {
			robotiumAnchor.anchor = espressoWindow.$(robotiumAnchors[j]).attr("data-html", true); // Allow HTML tooltips
			robotiumAnchor.title = robotiumAnchor.anchor.attr("data-original-title");
			robotiumAnchor.content = robotiumAnchor.anchor.attr("data-content");
			robotiumAnchor.time = robotiumAnchor.anchor.attr("data-time");
			robotiumAnchor.status = robotiumAnchor.anchor.parent().hasClass("pass");
		} else {
			robotiumAnchor = null;
		}
		j++;
	};
	nextRobotiumAnchor();

	espressoResultRow.find("a").each(function(i, espressoAnchor) {
		espressoAnchor = espressoWindow.$(espressoAnchor).attr("data-html", true); // Allow HTML tooltips

		var espressoTitle   = espressoAnchor.attr("data-original-title");
		var espressoContent = espressoAnchor.attr("data-content");
		var espressoTime    = espressoAnchor.attr("data-time");
		var espressoStatus  = espressoAnchor.parent().hasClass("pass");

		while (robotiumAnchor != null && (espressoTitle != robotiumAnchor.title || espressoContent != robotiumAnchor.content)) {
			nextRobotiumAnchor();
		}

		var tooltip = espressoContent;
		tooltip += "<br /><br />Espresso time: 100 ms"; // TODO: Change to time variable

		if (robotiumAnchor != null) {
			tooltip += "<br />Robotium time: 100 ms"; // TODO: Change to time variable
			espressoAnchor.attr("data-content", tooltip);
			robotiumAnchor.anchor.attr("data-content", tooltip).attr("data-visited", "true");

			if (espressoStatus != robotiumAnchor.status) {
				espressoAnchor.parent().addClass("diff");
				robotiumAnchor.anchor.parent().addClass("diff");
			}
		} else {
			espressoAnchor.attr("data-content", tooltip).parent().addClass("diff");
		}
	});

	robotiumAnchors.not("[data-visited=true]").each(function(i, robotiumAnchor) {
		robotiumAnchor = espressoWindow.$(robotiumAnchor).attr("data-html", true); // Allow HTML tooltips

		var robotiumTitle   = robotiumAnchor.attr("data-original-title");
		var robotiumContent = robotiumAnchor.attr("data-content");
		var robotiumTime    = robotiumAnchor.attr("data-time");

		var tooltip = robotiumContent;
		tooltip += "<br />Robotium time: 100 ms"; // TODO: Change to time variable
		robotiumAnchor.attr("data-content", tooltip).parent().addClass("diff");
	})
};

/***** ADD CONTROLS *****/

var addControls = function() {
	fs.readFile("compare-client.js", "utf8", function(err, data) {
		if (err)
			throw "Error reading file at 'compare-client.js'";

		espressoWindow.$("head").append("<script>" + data + "</script>");
		espressoWindow.$("table.birds-eye").before("<a href=\"javascript: void(0)\" id=\"diff\" class=\"pull-right\">Diff</a>");

		prepareOutput();
	});
};

/***** PREPARE OUTPUT *****/

var prepareOutput = function() {
	console.log("- Preparing output");

	var relativeLocation = espressoLocation.substring(0, espressoLocation.lastIndexOf("/")) + "/";
	espressoWindow.$("head link").each(function(i, link) {
		link = espressoWindow.$(link);
		if (link.attr("href") && !link.attr("href").indexOf("http://") == 0 && link.attr("href")[0] != "/")
			link.attr("href", relativeLocation + link.attr("href"));
	});
	espressoWindow.$("body a").each(function(i, anchor) {
		anchor = espressoWindow.$(anchor);
		if (anchor.attr("href") && anchor.attr("href").indexOf("http://") != 0 && anchor.attr("href").indexOf("javascript:") != 0 && anchor.attr("href")[0] != "/")
			anchor.attr("href", relativeLocation + anchor.attr("href"));
	});
	espressoWindow.$("head script").each(function(i, script) {
		script = espressoWindow.$(script);
		if (script.attr("src") && !script.attr("src").indexOf("http://") == 0 && script.attr("src")[0] != "/")
			script.attr("src", relativeLocation + script.attr("src"));
	});

	/*
	var customStyles = "<style>";
	customStyles += ".birds-eye { table-layout: auto !important; width: auto !important; }";
	customStyles += "</style>"
	espressoWindow.$("head").append(customStyles);
	*/

	espressoWindow.$(".jsdom").remove();

	output();
};

/***** OUTPUT *****/

var output = function() {
	console.log("- Outputting result to 'compare-result.html'");
	
	fs.writeFile("compare-result.html", "<!DOCTYPE html>" + espressoWindow.document.innerHTML, function(err) {
		if (err)
			throw "Error writing file 'compare-result.html'";
	});
};