package jnt.FFT;
/** Computes FFT's of complex, single precision data where n is an integer power of 2.
  * This appears to be slower than the Radix2 method,
  * but the code is smaller and simpler, and it requires no extra storage.
  * <P>
  * See {@link ComplexFloatFFT ComplexFloatFFT} for details of data layout.
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

public class ComplexFloatFFT_Radix2 extends ComplexFloatFFT {
  static final int FORWARD = -1;
  static final int BACKWARD = +1;
  static final int DECINTIME = 0;
  static final int DECINFREQ = 1;

  private int logn;

  private int decimate=DECINTIME;
  
  public ComplexFloatFFT_Radix2(int n){
    super(n);
    /* make sure that n is a power of 2 */
    logn = Factorize.log2(n);
    if (logn < 0)
      throw new Error(n+" is not a power of 2");
  }

  /* Lousy interface, but it'll do for now... */
  public void setDecimateInTime(){
    decimate = DECINTIME; }
  public void setDecimateInFrequency(){
    decimate = DECINFREQ; }

  public void transform (float data[], int i0, int stride) {
    checkData(data,i0,stride);
    transform_internal(data, i0, stride, FORWARD); }

  public void backtransform (float data[], int i0, int stride) {
    checkData(data,i0,stride);
    transform_internal(data, i0, stride, BACKWARD);  }

  /* ______________________________________________________________________ */

  void transform_internal (float data[], int i0, int stride, int direction) {
    if (decimate==DECINFREQ) {
      transform_DIF(data,i0,stride,direction); }
    else {
      transform_DIT(data,i0,stride,direction); }}

  void transform_DIT (float data[], int i0, int stride, int direction) {
    if (n == 1) return;		// Identity operation!

    /* bit reverse the input data for decimation in time algorithm */
    bitreverse(data, i0, stride) ;

    /* apply fft recursion */
    for (int bit = 0, dual = 1; bit < logn; bit++, dual *= 2) {
      float w_real = 1.0f;
      float w_imag = 0.0f;

      double theta = 2.0 * direction * Math.PI / (2.0 * dual);
      float s = (float)Math.sin(theta);
      float t = (float)Math.sin(theta / 2.0);
      float s2 = 2.0f * t * t;

      /* a = 0 */
      for (int b = 0; b < n; b += 2 * dual) {
	int i = i0+b*stride ;
	int j = i0+(b + dual)*stride;

	float wd_real = data[j] ;
	float wd_imag = data[j+1] ;
	  
	data[j]   = data[i]   - wd_real;
	data[j+1] = data[i+1] - wd_imag;
	data[i]  += wd_real;
	data[i+1]+= wd_imag;
      }
      
      /* a = 1 .. (dual-1) */
      for (int a = 1; a < dual; a++) {
	/* trignometric recurrence for w-> exp(i theta) w */
	{
	  float tmp_real = w_real - s * w_imag - s2 * w_real;
	  float tmp_imag = w_imag + s * w_real - s2 * w_imag;
	  w_real = tmp_real;
	  w_imag = tmp_imag;
	}
	for (int b = 0; b < n; b += 2 * dual) {
	  int i = i0+(b + a)*stride;
	  int j = i0+(b + a + dual)*stride;

	  float z1_real = data[j];
	  float z1_imag = data[j+1];
	      
	  float wd_real = w_real * z1_real - w_imag * z1_imag;
	  float wd_imag = w_real * z1_imag + w_imag * z1_real;

	  data[j]   = data[i]   - wd_real;
	  data[j+1] = data[i+1] - wd_imag;
	  data[i]  += wd_real;
	  data[i+1]+= wd_imag;
	}
      }
    }
  }

  void transform_DIF(float data[], int i0, int stride, int direction) {
    if (n == 1) return;		// Identity operation!

    /* apply fft recursion */
    for (int bit = 0, dual = n / 2; bit < logn; bit++, dual /= 2) {
      float w_real = 1.0f;
      float w_imag = 0.0f;

      double theta = 2.0 * direction * Math.PI / (2 * dual);

      float s = (float)Math.sin(theta);
      float t = (float)Math.sin(theta / 2.0);
      float s2 = 2.0f * t * t;

      for (int b = 0; b < dual; b++) {
	for (int a = 0; a < n; a+= 2 * dual) {
	  int i = i0+(b + a)*stride;
	  int j = i0+(b + a + dual)*stride;
	      
	  float t1_real = data[i]   + data[j];
	  float t1_imag = data[i+1] + data[j+1];
	  float t2_real = data[i]   - data[j];
	  float t2_imag = data[i+1] - data[j+1];

	  data[i]   = t1_real;
	  data[i+1] = t1_imag;
	  data[j]   = w_real*t2_real - w_imag * t2_imag;
	  data[j+1] = w_real*t2_imag + w_imag * t2_real;
	}
	/* trignometric recurrence for w-> exp(i theta) w */
	{
	  float tmp_real = w_real - s * w_imag - s2 * w_real;
	  float tmp_imag = w_imag + s * w_real - s2 * w_imag;
	  w_real = tmp_real;
	  w_imag = tmp_imag;
	}
      }
    }
    /* bit reverse the output data for decimation in frequency algorithm */
    bitreverse(data, i0, stride);
  }

  protected void bitreverse(float data[], int i0, int stride) {
    /* This is the Goldrader bit-reversal algorithm */

    for (int i = 0, j=0; i < n - 1; i++) {
      int ii = i0+i*stride;
      int jj = i0+j*stride;
      int k = n / 2 ;
      if (i < j) {
	float tmp_real    = data[ii];
	float tmp_imag    = data[ii+1];
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








