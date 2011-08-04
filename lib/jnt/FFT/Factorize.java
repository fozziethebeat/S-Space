package jnt.FFT;

/** Supplies static methods for factoring integers needed by various FFT classes. 
  *
  * @author Bruce R. Miller bruce.miller@nist.gov
  * @author Contribution of the National Institute of Standards and Technology,
  * @author not subject to copyright.
  * @author Derived from GSL (Gnu Scientific Library)
  * @author GSL's FFT Code by Brian Gough bjg@vvv.lanl.gov
  * @author Since GSL is released under 
  * @author <H HREF="http://www.gnu.org/copyleft/gpl.html">GPL</A>,
  * @author this package must also be.
*/
public class Factorize {

  /** Return the prime factors of n.
    * The method first extracts any factors in fromfactors, in order (which 
    * needn't actually be prime).  Remaining factors in increasing order follow. */
  public static int[] factor (int n, int fromfactors[]){
    int factors[] = new int[64]; // Cant be more than 64 factors.
    int nf = 0;
    int ntest = n;
    int factor;

    if (n <= 0)			// Error case
      throw new Error("Number ("+n+") must be positive integer");

    /* deal with the preferred factors first */
    for(int i = 0; i < fromfactors.length && ntest != 1; i++){
      factor = fromfactors[i];
      while ((ntest % factor) == 0) {
	ntest /= factor;
	factors[nf++] = factor; }}

    /* deal with any other even prime factors (there is only one) */
    factor = 2;
    while ((ntest % factor) == 0 && (ntest != 1)) {
      ntest /= factor;
      factors[nf++] = factor; }

    /* deal with any other odd prime factors */
    factor = 3;
    while (ntest != 1) {
      while ((ntest % factor) != 0) {
	factor += 2; }
      ntest /= factor;
      factors[nf++] = factor; }

    /* check that the factorization is correct */
    int product = 1;
    for (int i = 0; i < nf; i++) {
      product *= factors[i]; }
    if (product != n)
      throw new Error("factorization failed for "+n);

    /* Now, make an array of the right length containing the factors... */
    int f[] = new int[nf];
    System.arraycopy(factors,0,f,0,nf);    
    return f; }

  /** Return the integer log, base 2, of n, or -1 if n is not an integral power of 2.*/
  public static int log2 (int n){
    int log = 0;

    for(int k=1; k < n; k *= 2, log++);

    if (n != (1 << log))
      return -1 ; /* n is not a power of 2 */
    return log; }
}



