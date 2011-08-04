package jnt.FFT;
/** Computes FFT's of complex, double precision data where n is an integer power of 2.
  * This appears to be slower than the Radix2 method,
  * but the code is smaller and simpler, and it requires little extra storage.
  * <P>
  * See {@link ComplexDoubleFFT ComplexDoubleFFT} for details of data layout.
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

public class ComplexDoubleFFT_Radix2 extends ComplexDoubleFFT {
  static final double PI = Math.PI;
  static final int FORWARD = -1;
  static final int BACKWARD = +1;
  static final int DECINTIME = 0;
  static final int DECINFREQ = 1;

  private int logn;
  private int decimate=DECINTIME;
  private double trigs[];
  
  public ComplexDoubleFFT_Radix2(int n){
    super(n);
    /* make sure that n is a power of 2 */
    int log = Factorize.log2(n);
    if (log < 0)
      throw new Error(n+" is not a power of 2");
    this.logn = log;
    trigs = new double[logn+1];
    double theta = Math.PI;
    for(int i=0; i<=logn; i++) {
      trigs[i]=Math.sin(theta);
      theta/=2.0; }
  }

  /* Lousy interface, but it'll do for now... */
  public void setDecimateInTime(){
    decimate = DECINTIME; }
  public void setDecimateInFrequency(){
    decimate = DECINFREQ; }

  public void transform (double data[], int i0, int stride) {
    checkData(data,i0,stride);
    transform_internal(data, i0, stride, FORWARD); }

  public void backtransform (double data[], int i0, int stride) {
    checkData(data,i0,stride);
    transform_internal(data, i0, stride, BACKWARD);  }

  /* ______________________________________________________________________ */

  void transform_internal (double data[], int i0, int stride, int direction) {
    if (decimate==DECINFREQ) {
      transform_DIF(data,i0,stride,direction); }
    else {
      transform_DIT(data,i0,stride,direction); }}

  void transform_DIT (double data[], int i0, int stride, int direction) {
    if (n == 1) return;		// Identity operation!

    /* bit reverse the input data for decimation in time algorithm */
    bitreverse(data, i0, stride) ;

    /* apply fft recursion */
    for (int bit = 0, dual = 1; bit < logn; bit++, dual *= 2) {
      double w_real = 1.0;
      double w_imag = 0.0;
      
      //double theta = 2.0 * direction * Math.PI / (2.0 * dual);
      //double s = Math.sin(theta);
      //double t = Math.sin(theta / 2.0);
      double s = direction*trigs[bit];
      double t = direction*trigs[bit+1];
      double s2 = 2.0 * t * t;

      /* a = 0 */
      for (int b = 0; b < n; b += 2 * dual) {
	int i = i0+b*stride ;
	int j = i0+(b + dual)*stride;

	double wd_real = data[j] ;
	double wd_imag = data[j+1] ;
	  
	data[j]   = data[i]   - wd_real;
	data[j+1] = data[i+1] - wd_imag;
	data[i]  += wd_real;
	data[i+1]+= wd_imag;
      }
      
      /* a = 1 .. (dual-1) */
      for (int a = 1; a < dual; a++) {
	/* trignometric recurrence for w-> exp(i theta) w */
	{
	  double tmp_real = w_real - s * w_imag - s2 * w_real;
	  double tmp_imag = w_imag + s * w_real - s2 * w_imag;
	  w_real = tmp_real;
	  w_imag = tmp_imag;
	}
	for (int b = 0; b < n; b += 2 * dual) {
	  int i = i0+(b + a)*stride;
	  int j = i0+(b + a + dual)*stride;

	  double z1_real = data[j];
	  double z1_imag = data[j+1];
	      
	  double wd_real = w_real * z1_real - w_imag * z1_imag;
	  double wd_imag = w_real * z1_imag + w_imag * z1_real;

	  data[j]   = data[i]   - wd_real;
	  data[j+1] = data[i+1] - wd_imag;
	  data[i]  += wd_real;
	  data[i+1]+= wd_imag;
	}
      }
    }
  }

  void transform_DIF(double data[], int i0, int stride, int direction) {
    if (n == 1) return;		// Identity operation!

    /* apply fft recursion */
    for (int bit = 0, dual = n / 2; bit < logn; bit++, dual /= 2) {
      double w_real = 1.0;
      double w_imag = 0.0;

      //double theta = 2.0 * ((int) direction) * Math.PI / ((double) (2 * dual));
      //double s = Math.sin(theta);
      //double t = Math.sin(theta / 2.0);
      double s = direction*trigs[logn-1-bit];
      double t = direction*trigs[logn-bit];

      double s2 = 2.0 * t * t;

      for (int b = 0; b < dual; b++) {
	for (int a = 0; a < n; a+= 2 * dual) {
	  int i = i0+(b + a)*stride;
	  int j = i0+(b + a + dual)*stride;
	      
	  double t1_real = data[i]   + data[j];
	  double t1_imag = data[i+1] + data[j+1];
	  double t2_real = data[i]   - data[j];
	  double t2_imag = data[i+1] - data[j+1];

	  data[i]   = t1_real;
	  data[i+1] = t1_imag;
	  data[j]   = w_real*t2_real - w_imag * t2_imag;
	  data[j+1] = w_real*t2_imag + w_imag * t2_real;
	}
	/* trignometric recurrence for w-> exp(i theta) w */
	{
	  double tmp_real = w_real - s * w_imag - s2 * w_real;
	  double tmp_imag = w_imag + s * w_real - s2 * w_imag;
	  w_real = tmp_real;
	  w_imag = tmp_imag;
	}
      }
    }
    /* bit reverse the output data for decimation in frequency algorithm */
    bitreverse(data, i0, stride);
  }

  protected void bitreverse(double data[], int i0, int stride) {
    /* This is the Goldrader bit-reversal algorithm */

    for (int i = 0, j=0; i < n - 1; i++) {
      int ii = i0+i*stride;
      int jj = i0+j*stride;
      int k = n / 2 ;
      if (i < j) {
	double tmp_real    = data[ii];
	double tmp_imag    = data[ii+1];
	data[ii]   = data[jj];
	data[ii+1] = data[jj+1];
	data[jj]   = tmp_real;
	data[jj+1] = tmp_imag; }

      while (k <= j) {
	j = j - k ;
	k = k / 2 ; }
      j += k ;
    }
  }
}








