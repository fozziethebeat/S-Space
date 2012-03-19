package jnt.FFT;

/** Abstract Class representing FFT's of real, double precision data.
  * Concrete classes are typically named RealDoubleFFT_<i>method</i>, implement the
  * FFT using some particular method.
  * <P>
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
  * However, each Real FFT method tends to leave the real and imaginary parts 
  * distributed in the data array in its own unique arrangment.  
  * <P>
  * You must consult the documentation for the specific classes implementing
  * RealDoubleFFT for the details.
  * Note, however, that each class's backtransform and inverse methods understand
  * thier own unique ordering of the transformed result and can invert it correctly.
  *
  * @author Bruce R. Miller bruce.miller@nist.gov
  * @author Contribution of the National Institute of Standards and Technology,
  * @author not subject to copyright.
  */

public abstract class RealDoubleFFT {
  int n;

  /** Create an FFT for transforming n points of real, double precision data. */
  public RealDoubleFFT(int n){
    if (n <= 0)
      throw new IllegalArgumentException("The transform length must be >=0 : "+n);
    this.n = n; }

  protected void checkData(double data[], int i0, int stride){
    if (i0 < 0) 
      throw new IllegalArgumentException("The offset must be >=0 : "+i0);
    if (stride < 1)
      throw new IllegalArgumentException("The stride must be >=1 : "+stride);
    if (i0+stride*(n-1)+1 > data.length)
      throw new IllegalArgumentException("The data array is too small for "+n+":"+
					 "i0="+i0+" stride="+stride+
					 " data.length="+data.length); }

  /** Compute the Fast Fourier Transform of data leaving the result in data. */
  public void transform (double data[]) {
    transform (data, 0,1); }

  /** Compute the Fast Fourier Transform of data leaving the result in data. */
  public abstract void transform (double data[], int i0, int stride);

  /** Return data in wraparound order.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public abstract double[] toWraparoundOrder(double data[]);

  /** Return data in wraparound order.
    * i0 and stride are used to traverse data; the new array is in 
    * packed (i0=0, stride=1) format.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public abstract double[] toWraparoundOrder(double data[], int i0, int stride);

  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.*/
  public void backtransform (double data[]) {
    backtransform(data,0,1); }

  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.*/
  public abstract void backtransform (double data[], int i0, int stride);
  
  /** Return the normalization factor.  
   * Multiply the elements of the backtransform'ed data to get the normalized inverse.*/
  public double normalization(){
    return 1.0/((double) n); }

  /** Compute the (nomalized) inverse FFT of data, leaving it in place.*/
  public void inverse(double data[]) {
    inverse(data,0,1); }

  /** Compute the (nomalized) inverse FFT of data, leaving it in place.*/
  public void inverse (double data[], int i0, int stride) {
    backtransform(data, i0, stride);

  /* normalize inverse fft with 1/n */
    double norm = normalization();
    for (int i = 0; i < n; i++)
      data[i0+stride*i]   *= norm;
  }

}
