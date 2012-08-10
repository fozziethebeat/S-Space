package edu.ucla.sspace

object Util {
    val rejectTags = Set("#olympics", "#olympics2012", "#olympics12",
                         "#london2012", "#olympicday", "#london")
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
                                     .map(normalize)
                                     .filter(notUser)
                                     .filter(validTag)

    def validTag(tag: String) = !rejectTags.contains(tag)
    def validToken(token: String) = !rejectSet.contains(token)

    def notUser(token: String) = !token.startsWith("@")

    def normalize(token:String) = 
        if (token.size > 1)
            token.replaceAll("^[\\.?\\-$!()&%\"\']+", "")
                 .replaceAll("[\\W]+$", "")
        else token

    /**
     * Returns true if the sequence of tokens in {@code l1} are lexicographically less than the sequence of tokens in {@code l2}.
     */
    def tokenListComparator(l1: List[String], l2: List[String]) : Boolean = {
        for ( (t1, t2) <- l1.zip(l2) )
            if (t1 < t2)
                return true
            else if (t1 > t2)
                return false
        return l1.size < l2.size
    }
}
