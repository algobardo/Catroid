/* requires jsdom (npm install jsdom) */

var fs = require("fs");
var jsdom = require("jsdom");

if (process.argv.length != 4)
	console.log("Usage: node compare.js <espresso-result.html> <robotium-result.html>");

var resultEspresso = process.argv[2], domEspresso = null;
var resultRobotium = process.argv[3], domRobotium = null;
var domTemplate = null;

fs.readFile(resultEspresso, "utf8", function (err, data) {
	if (err)
		throw "Error reading file '" + resultEspresso + "'";

	// Parse file
	console.log("- Parsing file '" + resultEspresso + "'");
	jsdom.env(
		data,
		["http://code.jquery.com/jquery.js"],
		function (errors, window) {
			domEspresso = window;
			compare();
		}
	);
});

fs.readFile(resultRobotium, "utf8", function (err, data) {
	if (err)
		throw "Error reading file '" + resultRobotium + "'";

	// Parse file
	console.log("- Parsing file '" + resultRobotium + "'");
	jsdom.env(
		data,
		["http://code.jquery.com/jquery.js"],
		function (errors, window) {
			domRobotium = window;
			compare();
		}
	);
});

fs.readFile("compare-template.html", "utf8", function (err, data) {
	if (err)
		throw "Error reading file 'compare-template.html'";

	// Parse file
	console.log("- Parsing file 'compare-template.html'");
	jsdom.env(
		data,
		["http://code.jquery.com/jquery.js"],
		function (errors, window) {
			domTemplate = window;
			compare();
		}
	);
});

var readTestsFromGroupDom = function(which, dom, groupDom, tests) {
	var testsDom = groupDom.find("li.test");
	for (var i = 0; i < testsDom.length; i++) {
		var testDom = dom.$(testsDom[i]);

		// Extract data from DOM
		var name = testDom.children("span").clone().children().remove().end().text();
		var time = testDom.find("div.time").text();
		var status = testDom.find("em.status").text();
		var stderr = testDom.find("span.stderr").html();

		// Find or create test
		var test = null;
		var testIndex = -1;
		for (var j = 0; j < tests.length; j++)
			if (tests[j].name == name) {
				testIndex = j; break;
			}
		if (testIndex >= 0) {
			test = tests[testIndex];
		} else {
			tests.push(test = { name: name });
		}

		// Set data for Espresso/Robotium
		test[which] = { time: time, status: status, stderr: stderr };
	}
}

var readGroupsFromDom = function(which, dom, groups) {
	var groupsDom = dom.$("#tree > li");
	for (var i = 0; i < groupsDom.length; i++) {
		var groupDom = dom.$(groupsDom[i]);

		// Extract data from DOM
		var name = groupDom.children("span").clone().children().remove().end().text().replace("." + which, "");
		var time = groupDom.children("span").find("div.time").text();

		// Find or create group
		var group = null;
		var groupIndex = -1;
		for (var j = 0; j < groups.length; j++)
			if (groups[j].name == name) {
				groupIndex = j; break;
			}

		if (groupIndex >= 0) {
			group = groups[groupIndex];
		} else {
			groups.push(group = { name: name, tests: [] });
		}

		// Set data for Espresso/Robotium
		group[which] = { time: time };

		// Add tests to group
		readTestsFromGroupDom(which, dom, groupDom, group.tests)
	}
};

var compare = function() {
	if (!domEspresso || !domRobotium || !domTemplate)
		return;

	console.log("- Performing comparison");

	// Gather data
	var groups = [];
	readGroupsFromDom("espresso", domEspresso, groups);
	readGroupsFromDom("robotium", domRobotium, groups);

	var groupsDom = domEspresso.$("#tree > li");
	for (var i = 0; i < groupsDom.length; i++) {
		var groupDom = domEspresso.$(groupsDom[i]);

		// Extract data from DOM
		var name = groupDom.children("span").clone().children().remove().end().text().replace(".espresso", "");
		var time = groupDom.children("span").find("div.time").text();

		var group = null;
		for (var k = 0; k < groups.length; k++)
			if (groups[k].name == name) {
				group = groups[k]; break;
			}

		if (group.robotium) {
			groupDom.children("span").find("div.time").append(" / " + group.robotium.time);
		}

		var testsDom = groupDom.find("li.test");
		for (var j = 0; j < testsDom.length; j++) {
			var testDom = domEspresso.$(testsDom[j]);

			// Extract data from DOM
			var testName = testDom.children("span").clone().children().remove().end().text();

			var test = null;
			for (var k = 0; k < group.tests.length; k++)
				if (group.tests[k].name == testName) {
					test = group.tests[k]; break;
				}

			if (test.robotium) {
				testDom.find("div.time").append(" / " + test.robotium.time);

				if (test.espresso.status != test.robotium.status) {
					testDom.find("em.status").addClass("blue").append(" (" + test.robotium.status + ")");
				}
			}
		}
	}

	// Update header
	domEspresso.$("#header h1").after("<h1>" + domRobotium.$("#header h1").html() + "</h1>");
	domEspresso.$("#header .time").append(" /" + domRobotium.$("#header .time").text());

	// Custom styling
	domEspresso.$("head").append("<link href=\"compare.css\" rel=\"stylesheet\">");

	// Output
	domEspresso.$(".jsdom").remove();
	fs.writeFile("compare-result.html", domEspresso.document.innerHTML, function(err) {
		if (err)
			throw "Error writing file 'compare-result.html'";
	});
};