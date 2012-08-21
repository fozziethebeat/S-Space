var map = function() {
    if (!this.user ||
        !this.user.lang) {
        return;
    }
    emit(this.user.lang, 1);
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
                                  "out" : "langs." + label
                                });
    printjson(results);
}
