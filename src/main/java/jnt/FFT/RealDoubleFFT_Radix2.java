package jnt.FFT;

/** Computes  FFT's of real, double precision data where n is an integral power of 2.
  * The physical layout of the mathematical data d[i] in the array data is as follows:
  *<PRE>
  *    d[i] = data[i0 + stride*i]
  *</PRE>
  * The FFT (D[i]) of real data (d[i]) is complex, but restricted by symmetry:
  *<PRE>
  *    D[n-i] = conj(D[i])
  *</PRE>
  * It turns out that there are still n `independent' values, so the transformation
  * can still be carried out in-place.
  * <A name="transformlayout">For RealDoubleFFT_Radix2, the correspondence is as follows:
  * <table>
  *<tr><th>Logical</th><td></td><th>Physical</th></tr>
  *<tr><td>Re(D[0])</td><td>=</td><td>data[0]</td></tr>
  *<tr><td>Im(D[0])</td><td>=</td><td>0</td></tr>
  *<tr><td>Re(D[1])</td><td>=</td><td>data[1]</td></tr>
  *<tr><td>Im(D[1])</td><td>=</td><td>data[n-1]</td></tr>
  *<tr><td>...</td><td></td><td>...</td></tr>
  *<tr><td>Re(D[k])</td><td>=</td><td>data[k]</td></tr>
  *<tr><td>Im(D[k])</td><td>=</td><td>data[n-k]</td></tr>
  *<tr><td>...</td><td></td><td>...</td></tr>
  *<tr><td>Re(D[n/2])</td><td>=</td><td>data[n/2]</td></tr>
  *<tr><td>Im(D[n/2])</td><td>=</td><td>0</td></tr>
  *<tr><td>...</td><td></td><td>...</td></tr>
  *<tr><td>Re(D[n-k])</td><td>=</td><td>&nbsp;data[k]</td></tr>
  *<tr><td>Im(D[n-k])</td><td>=</td><td>-data[n-k]</td></tr>
  *<tr><td>...</td><td></td><td>...</td></tr>
  *<tr><td>Re(D[n-1])</td><td>=</td><td>&nbsp;data[1]</td></tr>
  *<tr><td>Im(D[n-1])</td><td>=</td><td>-data[n-1]</td></tr>
  *</table>
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

public class RealDoubleFFT_Radix2 extends RealDoubleFFT {
  private int logn;
  
  /** Create an FFT for transforming n points of real, double precision data.
    * n must be an integral power of 2. */
  public RealDoubleFFT_Radix2(int n){
    /* make sure that n is a power of 2 */
    super(n);
    logn = Factorize.log2(n);
    if (logn < 0)
      throw new IllegalArgumentException(n+" is not a power of 2");
  }

  /** Compute the Fast Fourier Transform of data leaving the result in data.
   * See {@link <A HREF="#transformlayout"> Radix2 Transform Layout</A>} for description of
   * the resulting data layout.*/
  public void transform (double data[], int i0, int stride) {
    checkData(data,i0,stride);
    int p, p_1, q;

    if (n == 1) return;		/* identity operation */

    /* bit reverse the ordering of input data for decimation in time algorithm */
    bitreverse(data, i0, stride);

    /* apply fft recursion */
    p = 1; q = n ;
    for (int i = 1; i <= logn; i++) {
      int a, b;

      p_1 = p ;
      p = 2 * p ;
      q = q / 2 ;

      /* a = 0 */

      for (b = 0; b < q; b++) {
	double t0_real = data[i0+stride*b*p] + data[i0+stride*(b*p + p_1)];
	double t1_real = data[i0+stride*b*p] - data[i0+stride*(b*p + p_1)];
	  
	data[i0+stride*b*p] = t0_real;
	data[i0+stride*(b*p + p_1)] = t1_real;
      }

      /* a = 1 ... p_{i-1}/2 - 1 */

      {
	double w_real = 1.0;
	double w_imag = 0.0;

	double theta = - 2.0 * Math.PI / p;
	
	double s = Math.sin(theta);
	double t = Math.sin(theta / 2.0);
	double s2 = 2.0 * t * t;
	
	for (a = 1; a < (p_1)/2; a++) {
	  /* trignometric recurrence for w-> exp(i theta) w */
	    
	  {
	    double tmp_real = w_real - s * w_imag - s2 * w_real;
	    double tmp_imag = w_imag + s * w_real - s2 * w_imag;
	    w_real = tmp_real;
	    w_imag = tmp_imag;
	  }
	    
	  for (b = 0; b < q; b++) {
	    double z0_real = data[i0+stride*(b*p + a)];
	    double z0_imag = data[i0+stride*(b*p + p_1 - a)];
	    double z1_real = data[i0+stride*(b*p + p_1 + a)];
	    double z1_imag = data[i0+stride*(b*p + p - a)];
		
	    /* t0 = z0 + w * z1 */
	    data[i0+stride*(b*p + a)]    =z0_real + w_real * z1_real - w_imag * z1_imag;
	    data[i0+stride*(b*p + p - a)]=z0_imag + w_real * z1_imag + w_imag * z1_real;
	    /* t1 = -(z0 - w * z1) */
	    data[i0+stride*(b*p + p_1 - a)]=z0_real - w_real * z1_real + w_imag * z1_imag;
	    data[i0+stride*(b*p + p_1 + a)]=-(z0_imag - w_real * z1_imag - w_imag * z1_real);
	  }
	}
      }

      if (p_1 >  1) {
	for (b = 0; b < q; b++) {
	  /* a = p_{i-1}/2 */
	  data[i0+stride*(b*p + p - p_1/2)] *= -1 ;
	}}
    }
  }

  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.
   * The data must be in the same arrangement as that produced by 
   {@link #transform transform}. */
  public void backtransform (double data[], int i0, int stride) {
    checkData(data,i0,stride);
    int p, p_1, q;

    if (n == 1) return; /* identity operation */

    /* apply fft recursion */

    p = n; q = 1 ; p_1 = n/2 ;

    for (int i = 1; i <= logn; i++) {
      int a, b;

      /* a = 0 */

      for (b = 0; b < q; b++) {
	double z0 = data[i0+stride*b*p];
	double z1 = data[i0+stride*(b*p + p_1)];
	data[i0+stride*b*p]         = z0 + z1 ;
	data[i0+stride*(b*p + p_1)] = z0 - z1 ; }

      /* a = 1 ... p_{i-1}/2 - 1 */

      {
	double w_real = 1.0;
	double w_imag = 0.0;

	double theta = 2.0 * Math.PI / p;
	
	double s = Math.sin(theta);
	double t = Math.sin(theta / 2.0);
	double s2 = 2.0 * t * t;
	
	for (a = 1; a < (p_1)/2; a++) {
	  /* trignometric recurrence for w-> exp(i theta) w */
	  double tmp_real = w_real - s * w_imag - s2 * w_real;
	  double tmp_imag = w_imag + s * w_real - s2 * w_imag;
	  w_real = tmp_real;
	  w_imag = tmp_imag;

	  for (b = 0; b < q; b++) {
	    double z0_real =  data[i0+stride*(b*p + a)];
	    double z0_imag =  data[i0+stride*(b*p + p - a)];
	    double z1_real =  data[i0+stride*(b*p + p_1 - a)];
	    double z1_imag = -data[i0+stride*(b*p + p_1 + a)];
		
	    /* t0 = z0 + z1 */		
	    data[i0+stride*(b*p + a)]       = z0_real + z1_real;
	    data[i0+stride*(b*p + p_1 - a)] = z0_imag + z1_imag;

	    /* t1 = (z0 - z1) */
	    double t1_real = z0_real -  z1_real;
	    double t1_imag = z0_imag -  z1_imag;
	    data[i0+stride*(b*p + p_1 + a)] = (w_real * t1_real - w_imag * t1_imag) ;
	    data[i0+stride*(b*p + p - a)]   = (w_real * t1_imag + w_imag * t1_real) ;
	  }
	}
      }

      if (p_1 >  1) {
	for (b = 0; b < q; b++) {
	  data[i0+stride*(b*p + p_1/2)] *= 2 ;
	  data[i0+stride*(b*p + p_1 + p_1/2)] *= -2 ;
	}
      }

      p_1 = p_1 / 2 ;
      p = p / 2 ;
      q = q * 2 ;
    }

    /* bit reverse the ordering of output data for decimation in
       frequency algorithm */
    bitreverse(data, i0, stride);

  }

  /** Return data in wraparound order.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public double[] toWraparoundOrder(double data[]){
    return toWraparoundOrder(data,0,1); }

  /** Return data in wraparound order.
    * i0 and stride are used to traverse data; the new array is in 
    * packed (i0=0, stride=1) format.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public double[] toWraparoundOrder(double data[], int i0, int stride) {
    checkData(data,i0,stride);
    double newdata[] = new double[2*n];
    int nh = n/2;
    newdata[0]   = data[i0];
    newdata[1]   = 0.0;
    newdata[n]   = data[i0+stride*nh];
    newdata[n+1] = 0.0;
    for(int i=1; i<nh; i++){
      newdata[2*i]      = data[i0+stride*i];
      newdata[2*i+1]    = data[i0+stride*(n-i)];
      newdata[2*(n-i)]  = data[i0+stride*i];
      newdata[2*(n-i)+1]=-data[i0+stride*(n-i)]; }
    return newdata; }

  protected void bitreverse(double data[], int i0, int stride) {
    /* This is the Goldrader bit-reversal algorithm */

    for (int i = 0,j = 0; i < n - 1; i++) {
      int k = n / 2 ;
      if (i < j) {
	double tmp        = data[i0+stride*i];
	data[i0+stride*i] = data[i0+stride*j];
	data[i0+stride*j] = tmp; }

      while (k <= j) {
	j = j - k ;
	k = k / 2 ; }
      j += k ;
    }}
}
