var http = require("http");
var fs = require("fs");
var jsdom = require("jsdom");

var download = function(location, cb) {
	var data = "";
	
	console.log("- Downloading file at '" + location + "'");
	var request = http.get(location, function(res) {
		res.on("data", function(chunk) {
			data += chunk;
		});
		res.on("end", function() {
			cb(data);
		})
	});

	request.on("error", function(err) {
		throw "Error downloading file at '" + location + "'";
	});
};

var loadResource = function(location, callback) {
	if (location.indexOf("http://") == 0) {
		download(location, callback);
	} else {
		console.log("- Reading file at '" + location + "'");
		fs.readFile(location, "utf8", function(err, data) {
			if (err)
				throw "Error reading file at '" + location + "'";
			callback(data);
		});
	}
};

var parseHTML = function(data, callback) {
	jsdom.env(data, ["http://code.jquery.com/jquery.js"], function(errors, window) {
		callback(window);
	});
};

exports.loadResource = loadResource;
exports.parseHTML = parseHTML;