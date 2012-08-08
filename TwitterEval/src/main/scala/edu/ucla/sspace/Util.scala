package edu.ucla.sspace

object Util {
    val rejectTags = Set("#olympics", "#olympics2012", "#olympics12",
                         "#london2012", "#olympicday")
    val rejectSet = Set("the", "", "to", "rt", "a", "is", "if", "with", 
                        "in", "on", "i", "and", "be", "or", "for", "is",
                        "are", "am", "he", "she", "that", "this", "they", 
                        "them", "will", "it", "you", "now", "from", "so",
                        "have", "but", "just", "i'm", "come", "as", "of",
                        "we", "my", "not", "got", "an", "me", "it's", "its",
                        "him", "his", "her", "can", "don't", "has", "was",
                        "do", "here") ++ rejectTags

    def tokenize(text: String) = text.toLowerCase
                                     .split("\\s+")
                                     .filter(notUser)
                                     .map(normalize)

    def validTag(tag: String) = !rejectTags.contains(tag)
    def validToken(token: String) = !rejectSet.contains(token)

    def notUser(token: String) = !token.startsWith("@")

    def normalize(token:String) = 
        if (token.size > 1)
            token.replaceAll("^[\\.?\\-$!()&%]+", "")
                 .replaceAll("[\\W]+$", "")
        else token
}
