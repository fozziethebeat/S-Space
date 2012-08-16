var map = function() {
    if (!this.entities ||
        !this.entities.hashtags) {
        return;
    }

    for (index in this.entities.hashtags) {
        emit(this.entities.hashtags[index].text.toLowerCase(), 1);
    }
};

var reduce = function(prev, current) {
    var count = 0;
    for (index in current) {
        count += current[index];
    }
    return count;
};

var keyWords = ["", "gymnastics", "tennis", "archery", "judo", "fencing"];

for (index in keyWords) {
    var keyWord = keyWords[index];
    var label;
    if (keyWord == "")
        label = "all";
    else
        label = keyWord

    var results = db.runCommand({ "mapreduce" : "posts",
                                  "map" : map,
                                  "reduce" : reduce,
                                  "query": { text: { $regex : keyWord, $options: 'i' } },
                                  "out" : "tags." + label
                                });
    printjson(results);
}
