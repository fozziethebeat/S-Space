/// Import jquery library.
var $ = require('jquery');
// Import file system library.
var fs = require('fs');

// Get the file name we want to parse from the command line and read it from
// disk, then handle it with a callback.
fs.readFile(process.argv[2], function(err, data) {
    // Throw any errors found.
    if (err) throw err;
    // Convert the raw data to text.
    var html = data.toString();
    // Navigate the html to find all div elements with the "disciplines" class
    // and the children of these elements with the "bar" class.  These elements
    // define the time of each event.
    var barElemens = $(html).find(".disciplines").children(".bar")
    // For each bar element found, extract the start time and end time.  The
    // start time is the text within the span having a ".bar-time" class and the
    // end time is the second class label of each element.
    barElemens.each(function(i) {
        // Convert the index to an element in the array.
        var elem = barElemens[i];
        var classAttr = $(elem).attr("class");
        var dataType = $(elem).attr("data-type");
        if (dataType == "") {
            // If the data-type attribute is empty, then we know that both the
            // start and end times are stored in the class attribute, albeit in
            // an utterly horrible and wretched format that uses two different
            // timezones and two different formats.
            console.log(classAttr);
        } else if (dataType.substr(-1) == "_") {
            // If the data-type ends with an underscore, we know that the start
            // time is in the text and the end time is in the class attribute.
            // So report that.
            console.log(classAttr + " " + $(elem).find(".bar-time").text());
        } else {
            // Otherwise, the data-type has the full time range for the event,
            // so just report that, even though it's in a different format.
            console.log(dataType);
        }
    });
});
