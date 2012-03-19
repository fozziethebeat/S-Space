package jnt.FFT;
/** Abstract Class representing FFT's of complex, single precision data.
  * Concrete classes are typically named ComplexFloatFFT_<i>method</i>, implement the
  * FFT using some particular method.
  * <P>
  * Complex data is represented by 2 double values in sequence: the real and imaginary
  * parts.  Thus, in the default case (i0=0, stride=2), N data points is represented
  * by a double array dimensioned to 2*N.  To support 2D (and higher) transforms,
  * an offset, i0 (where the first element starts) and stride (the distance from the
  * real part of one value, to the next: at least 2 for complex values) can be supplied.
  * The physical layout in the array data, of the mathematical data d[i] is as follows:
  *<PRE>
  *    Re(d[i]) = data[i0 + stride*i]
  *    Im(d[i]) = data[i0 + stride*i+1]
  *</PRE>
  * The transformed data is returned in the original data array in
  * <a href="package-summary.html#wraparound">wrap-around</A> order.
  *
  * @author Bruce R. Miller bruce.miller@nist.gov
  * @author Contribution of the National Institute of Standards and Technology,
  * @author not subject to copyright.
  */

public abstract class ComplexFloatFFT {
    
  int n;

  /** Create an FFT for transforming n points of Complex, single precision data. */
  public ComplexFloatFFT(int n){
    if (n <= 0)
      throw new IllegalArgumentException("The transform length must be >=0 : "+n);
    this.n = n; }

  /** Creates an instance of a subclass of ComplexFloatFFT appropriate for data
    * of n elements.*/
  public ComplexFloatFFT getInstance(int n){
    return new ComplexFloatFFT_Mixed(n); }

  protected void checkData(float data[], int i0, int stride){
    if (i0 < 0) 
      throw new IllegalArgumentException("The offset must be >=0 : "+i0);
    if (stride < 2)
      throw new IllegalArgumentException("The stride must be >=2 : "+stride);
    if (i0+stride*(n-1)+2 > data.length)
      throw new IllegalArgumentException("The data array is too small for "+n+":"+
					 "i0="+i0+" stride="+stride+
					 " data.length="+data.length); }

  /** Compute the Fast Fourier Transform of data leaving the result in data.
    * The array data must be dimensioned (at least) 2*n, consisting of alternating
    * real and imaginary parts. */
  public void transform (float data[]) {
    transform (data, 0,2); }

  /** Return data in wraparound order.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public float[] toWraparoundOrder(float data[]){
    return data; }

  /** Return data in wraparound order.
    * i0 and stride are used to traverse data; the new array is in 
    * packed (i0=0, stride=2) format.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public float[] toWraparoundOrder(float data[], int i0, int stride) {
    if ((i0==0)&&(stride==2)) return data;
    float newdata[] = new float[2*n];
    for(int i=0; i<n; i++){
      newdata[2*i]   = data[i0+stride*i];
      newdata[2*i+1] = data[i0+stride*i+1]; }
    return newdata; }

  /** Compute the Fast Fourier Transform of data leaving the result in data.
    * The array data must contain the data points in the following locations:
    *<PRE>
    *    Re(d[i]) = data[i0 + stride*i]
    *    Im(d[i]) = data[i0 + stride*i+1]
    *</PRE>
    */
  public abstract void transform (float data[], int i0, int stride);

  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.*/
  public void backtransform (float data[]) {
    backtransform(data,0,2); }

  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.
    * The frequency domain data must be in wrap-around order, and be stored
    * in the following locations:
    *<PRE>
    *    Re(D[i]) = data[i0 + stride*i]
    *    Im(D[i]) = data[i0 + stride*i+1]
    *</PRE>
    */
  public abstract void backtransform (float data[], int i0, int stride);
  
  /** Return the normalization factor.  
   * Multiply the elements of the backtransform'ed data to get the normalized inverse.*/
  public float normalization(){
    return 1.0f/((float) n); }

  /** Compute the (nomalized) inverse FFT of data, leaving it in place.*/
  public void inverse(float data[]) {
    inverse(data,0,2); }

  /** Compute the (nomalized) inverse FFT of data, leaving it in place.
    * The frequency domain data must be in wrap-around order, and be stored
    * in the following locations:
    *<PRE>
    *    Re(D[i]) = data[i0 + stride*i]
    *    Im(D[i]) = data[i0 + stride*i+1]
    *</PRE>
    */
  public void inverse (float data[], int i0, int stride) {
    backtransform(data, i0, stride);

  /* normalize inverse fft with 1/n */
    float norm = normalization();
    for (int i = 0; i < n; i++) {
      data[i0+stride*i]   *= norm;
      data[i0+stride*i+1] *= norm; }}
}
