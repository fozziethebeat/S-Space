/*
 * Copyright 2009 Keith Stevens 
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */


package edu.ucla.sspace.text;


/**
 * This is an implementation of the Porter stemmer in Java. The original paper
 * is in  Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14, no.
 * 3, pp 130-137,
 * 
 * </p> This code has been ported and heavily modified based on the original
 * implementation found <a
 * href="http://www.tartarus.org/~martin/PorterStemmer">here</a>
 *
 * @author Keith Stevens
 */
public class PorterStemmer implements Stemmer {

    private StringBuilder sb;
    private char[] b;    
    private int j, k;

    /**
     * Creates a new {@link PorterStemmer}
     */
    public PorterStemmer() { }
    
    /**
     * {@inheritDoc}
     */
    public synchronized String stem(String token) {
        // NOTE: due to the fact that this implemenation saves state per
        // stemming operation, the method needs to be synchronized in order to
        // prevent race conditions by multiple threads.
        sb = new StringBuilder(token);
        k = sb.length() - 1;
        if (k > 1) {
            step1();
            step2();
            step3();
            step4();
            step5();
            step6();
        } 
        sb.delete(k+1, sb.length());
        return sb.toString();        
    }

    /**
     * cons(i) is true <=> b[i] is a consonant.
     */
    private final boolean cons(int i) {
        switch (sb.charAt(i)) {
        case 'a':
        case 'e':
        case 'i':
        case 'o':
        case 'u':
            return false;
        case 'y':
            return (i==0) ? true : !cons(i-1);
        default:
            return true;
        }
    }

    /**
     * m() measures the number of consonant sequences between 0 and j. if c is
     *  a consonant sequence and v a vowel sequence, and <..> indicates
     *  arbitrary presence,
     *
     *  <c><v>       gives 0
     *  <c>vc<v>     gives 1
     *  <c>vcvc<v>   gives 2
     *  <c>vcvcvc<v> gives 3
     *  ....
     */
    private final int m() {
        int n = 0;
        int i = 0;
        for (; cons(i); ++i) {
            if (i > j)
                return n;
        }
        i++;
        while(i <= j) {
            for (; !cons(i); ++i) {
                if (i > j)
                    return n;
            }
            i++;
            n++;

            if (i > j)
                return n;
            for (; cons(i); ++i) {
                if (i > j)
                    return n;
            }
            i++;
        }
        return n;
    }

    /* vowelinstem() is true <=> 0,...j contains a vowel */

    private final boolean vowelinstem() {
        for (int i = 0; i <= j; i++)
            if (!cons(i))
                return true;
        return false;
    }

    /* doublec(j) is true <=> j,(j-1) contain a double consonant. */

    private final boolean doublec(int j) {
        if (j < 1)
            return false;
        if (sb.charAt(j) != sb.charAt(j-1))
            return false;
        return cons(j);
    }

    /* cvc(i) is true <=> i-2,i-1,i has the form consonant - vowel - consonant
       and also if the second c is not w,x or y. this is used when trying to
       restore an e at the end of a short word. e.g.

       cav(e), lov(e), hop(e), crim(e), but
       snow, box, tray.

    */
    private final boolean cvc(int i) {
        if (i < 2 || !cons(i) || cons(i-1) || !cons(i-2))
            return false;
        char ch = sb.charAt(i);
        return !(ch == 'w' || ch == 'x' || ch == 'y');
    }

    private final boolean ends(String s) {
        int l = s.length();
        int o = k-l+1;
        if (o < 0) 
            return false;
        for (int i = 0; i < l; i++)
            if (sb.charAt(o+i) != s.charAt(i))
                return false;
        j = k-l;
        return true;
    }

    /* setto(s) sets (j+1),...k to the characters in the string s, readjusting
       k. */

    private final void setto(String s) {
        int l = s.length(); 
        int o = j+1;
        for (int i = 0; i < l; i++)
            sb.setCharAt(o+i, s.charAt(i));
        k = j+l;
    }

    /* r(s) is used further down. */

    private final void r(String s) {
        if (m() > 0)
            setto(s);
    }

    /* step1() gets rid of plurals and -ed or -ing. e.g.

       caresses  ->  caress
       ponies    ->  poni
       ties      ->  ti
       caress    ->  caress
       cats      ->  cat

       feed      ->  feed
       agreed    ->  agree
       disabled  ->  disable

       matting   ->  mat
       mating    ->  mate
       meeting   ->  meet
       milling   ->  mill
       messing   ->  mess

       meetings  ->  meet

    */
    private final void step1() {
        if (sb.charAt(k) == 's') {
            if (ends("sses"))
                k -= 2;
            else if (ends("ies"))
                setto("i");
            else if (sb.charAt(k-1) != 's')
                k--;
        }
        if (ends("eed")) {
            if (m() > 0)
                k--;
        } else if ((ends("ed") || ends("ing")) && vowelinstem()) {
            k = j;
            if (ends("at"))
                setto("ate");
            else if (ends("bl"))
                setto("ble");
            else if (ends("iz"))
                setto("ize");
            else if (doublec(k)) {
                k--; 
                char ch = sb.charAt(k);
                if (ch == 'l' || ch == 's' || ch == 'z')
                    k++; 
            } else if (m() == 1 && cvc(k)) 
                setto("e"); 
        } 
    }

    /* step2() turns terminal y to i when there is another vowel in the stem. */
    private final void step2() {
        if (ends("y") && vowelinstem())
            sb.setCharAt(k, 'i');
    }

    /* step3() maps double suffices to single ones. so -ization ( = -ize plus
       -ation) maps to -ize etc. note that the string before the suffix must give
       m() > 0. */
    private final void step3() {
        if (k == 0)
            return;
        /* For Bug 1 */
        switch (sb.charAt(k-1)) { 
        case 'a':
            if (ends("ational")) 
                r("ate");
            else if (ends("tional"))
                r("tion"); 
            break;
        case 'c':
            if (ends("enci")) 
                r("ence");
            else if (ends("anci")) 
                r("ance");
            break;
        case 'e':
            if (ends("izer")) 
                r("ize");
            break;
        case 'l':
            if (ends("bli"))
                r("ble");
            else if (ends("alli"))
                r("al");  
            else if (ends("entli"))
                r("ent"); 
            else if (ends("eli"))
                r("e");
            else if (ends("ousli"))
                r("ous");
            break; 
        case 'o':
            if (ends("ization")) 
                r("ize"); 
            else if (ends("ation")) 
                r("ate"); 
            else if (ends("ator"))
                r("ate");
            break;
        case 's':
            if (ends("alism")) 
                r("al"); 
            else if (ends("iveness")) 
                r("ive");
            else if (ends("fulness")) 
                r("ful"); 
            else if (ends("ousness")) 
                r("ous"); 
            break; 
        case 't': 
            if (ends("aliti")) 
                r("al"); 
            else if (ends("iviti"))
                r("ive");
            else if (ends("biliti")) 
                r("ble");
            break;
        case 'g':
            if (ends("logi")) 
                r("log");
            break;
        }
    }

    /* step4() deals with -ic-, -full, -ness etc. similar strategy to step3. */
    private final void step4() {
        switch (sb.charAt(k)) {
        case 'e':
            if (ends("icate")) 
                r("ic");
            else if (ends("ative"))
                r("");
            else if (ends("alize")) 
                r("al");
            break;
        case 'i':
            if (ends("iciti")) 
                r("ic");
            break;
        case 'l':
            if (ends("ical")) 
                r("ic");
            else if (ends("ful")) 
                r("");
            break;
        case 's':
            if (ends("ness")) r("");
            break;
        }
    }

    /* step5() takes off -ant, -ence etc., in context <c>vcvc<v>. */

    private final void step5() {
        if (k == 0)
            return;
        /* for Bug 1 */ 
        switch (sb.charAt(k-1)) {
        case 'a':
            if (ends("al"))
                break;
            return;
        case 'c': 
            if (ends("ance"))
                break;
            if (ends("ence"))
                break;
            return;
        case 'e':
            if (ends("er"))
                break;
            return;
        case 'i':
            if (ends("ic"))
                break;
            return;
        case 'l':
            if (ends("able"))
                break;
            if (ends("ible"))
                break;
            return;
        case 'n':
            if (ends("ant"))
                break;
            if (ends("ement"))
                break;
            if (ends("ment"))
                break;
            /* element etc. not stripped before the m */
            if (ends("ent"))
                break;
            return;
        case 'o':
            if (ends("ion") && j >= 0 && (b[j] == 's' || b[j] == 't'))
                break;
            /* j >= 0 fixes Bug 2 */
            if (ends("ou"))
                break;
            return;
            /* takes care of -ous */
        case 's':
            if (ends("ism"))
                break;
            return;
        case 't': 
            if (ends("ate"))
                break;
            if (ends("iti"))
                break;
            return;
        case 'u':
            if (ends("ous"))
                break;
            return;
        case 'v':
            if (ends("ive"))
                break;
            return;
        case 'z':
            if (ends("ize"))
                break;
            return;
        default:
            return;
        }
        if (m() > 1) k = j;
    }

    /* step6() removes a final -e if m() > 1. */

    private final void step6() {
        j = k;
        if (sb.charAt(k) == 'e') {
            int a = m();
            if (a > 1 || a == 1 && !cvc(k-1))
                k--;
        }
        if (sb.charAt(k) == 'l' && doublec(k) && m() > 1)
            k--; 
    }
}

