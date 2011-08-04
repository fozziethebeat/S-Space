package jnt.FFT;
/** Computes FFT's of real, double precision data when n is even, by
  * computing complex FFT.
  *
  * @author Bruce R. Miller bruce.miller@nist.gov
  * @author Derived from Numerical Methods.
  * @author Contribution of the National Institute of Standards and Technology,
  * @author not subject to copyright.
  */

public class RealDoubleFFT_Even extends RealDoubleFFT {
  ComplexDoubleFFT fft;

  /** Create an FFT for transforming n points of real, double precision data. */
  public RealDoubleFFT_Even(int n){
    super(n);
    if (n%2 != 0)
      throw new IllegalArgumentException(n+" is not even");
    fft = new ComplexDoubleFFT_Mixed(n/2);
  }

  /** Compute the Fast Fourier Transform of data leaving the result in data. */
  public void transform (double data[]) {
    fft.transform(data);
    shuffle(data,+1);
  }

  /** Return data in wraparound order.
    * i0 and stride are used to traverse data; the new array is in 
    * packed (i0=0, stride=1) format.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public double[] toWraparoundOrder(double data[]){
    double newdata[] = new double[2*n];
    int nh = n/2;
    newdata[0]   = data[0];
    newdata[1]   = 0.0;
    newdata[n]   = data[1];
    newdata[n+1] = 0.0;
    for(int i=1; i<nh; i++){
      newdata[2*i]      = data[2*i];
      newdata[2*i+1]    = data[2*i+1];
      newdata[2*(n-i)]  = data[2*i];
      newdata[2*(n-i)+1]=-data[2*i+1]; }
    return newdata; }

  /** Return data in wraparound order.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public double[] toWraparoundOrder(double data[], int i0, int stride) {
    throw new Error("Not Implemented!"); }  


  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.*/
  public void backtransform (double data[]){
    shuffle(data,-1);
    fft.backtransform(data);
  }

  private void shuffle(double data[], int sign){
    int nh = n/2;
    int nq = n/4;
    double c1=0.5, c2 = -0.5*sign;
    double theta = sign*Math.PI/nh;
    double wtemp = Math.sin(0.5*theta);
    double wpr = -2.0*wtemp*wtemp;
    double wpi = -Math.sin(theta);
    double wr = 1.0+wpr;
    double wi = wpi;
    for(int i=1; i < nq; i++){
      int i1 = 2*i;
      int i3 = n - i1;
      double h1r =  c1*(data[i1  ]+data[i3]);
      double h1i =  c1*(data[i1+1]-data[i3+1]);
      double h2r = -c2*(data[i1+1]+data[i3+1]);
      double h2i =  c2*(data[i1  ]-data[i3]);
      data[i1  ] = h1r+wr*h2r-wi*h2i;
      data[i1+1] = h1i+wr*h2i+wi*h2r;
      data[i3  ] = h1r-wr*h2r+wi*h2i;
      data[i3+1] =-h1i+wr*h2i+wi*h2r;
      wtemp = wr;
      wr += wtemp*wpr-wi*wpi;
      wi += wtemp*wpi+wi*wpr; }
    double d0 = data[0];
    if (sign == 1){
      data[0] = d0+data[1];
      data[1] = d0-data[1]; }
    else {
      data[0] = c1*(d0+data[1]);
      data[1] = c1*(d0-data[1]); }
    if (n%4==0)
      data[nh+1] *= -1;
  }

  /** Compute the Fast Fourier Transform of data leaving the result in data. */
  public void transform (double data[], int i0, int stride) {
    throw new Error("Not Implemented!"); }


  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.*/
  public void backtransform (double data[], int i0, int stride){
    throw new Error("Not Implemented!"); }  

  /** Compute the (nomalized) inverse FFT of data, leaving it in place.*/
  public void inverse (double data[], int i0, int stride){
    throw new Error("Not Implemented!"); }  

  /** Return the normalization factor.  
   * Multiply the elements of the backtransform'ed data to get the normalized inverse.*/
  public double normalization(){
    return 2.0/((double) n); }

  /** Compute the (nomalized) inverse FFT of data, leaving it in place.*/
  public void inverse (double data[]) {
    backtransform(data);
  /* normalize inverse fft with 2/n */
    double norm = normalization();
    for (int i = 0; i < n; i++)
      data[i]   *= norm;
  }

}
