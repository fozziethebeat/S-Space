package jnt.FFT;
/** EXPERIMENTAL! (till I think of something better):
  * Computes the FFT of 2 dimensional real, single precision data.
  * The data is stored in a 1-dimensional array in almost Row-Major order.
  * The number of columns MUST be even, and there must be two extra elements per row!
  * The physical layout in the real input data array, of the mathematical data d[i,j] is
  * as follows:
  *<PRE>
  *    d[i,j]) = data[i*rowspan+j]
  *</PRE>
  * where rowspan >= ncols+2.
  *<P><B>WARNING!</B> Note that rowspan must be greater than the number of columns,
  * and the next 2 values, as well as the data itself, are <b>overwritten</b> in 
  * order to store enough of the complex transformation in place.
  * (In fact, it can be done completely in place, but where one has to look for various
  * real and imaginary parts is quite complicated).
  *<P>
  * The physical layout in the transformed (complex) array data, of the
  * mathematical data D[i,j] is as follows:
  *<PRE>
  *    Re(D[i,j]) = data[2*(i*rowspan+j)]
  *    Im(D[i,j]) = data[2*(i*rowspan+j)+1]
  *</PRE>
  * <P>
  * The transformed data in each row is complex for frequencies from 
  * 0, 1/(n delta), ... 1/(2 delta), where delta is the time difference between
  * the column values.
  * <P>
  * The transformed data for columns is in `wrap-around' order; that is from
  * 0, 1/(n delta)... +/- 1/(2 delta) ... -1/(n delta)
  * @author Bruce R. Miller bruce.miller@nist.gov
  * @author Contribution of the National Institute of Standards and Technology,
  * @author not subject to copyright.
  */ 
public class RealFloat2DFFT_Even {
  int nrows;
  int ncols;
  int rowspan;
  ComplexFloatFFT rowFFT, colFFT;

  /** Create an FFT for transforming nrows*ncols points of Complex, double precision
    * data. */
  public RealFloat2DFFT_Even(int nrows, int ncols) {
    this.nrows = nrows;
    this.ncols = ncols;
    rowspan = ncols+2;
    if (ncols%2 != 0)
      throw new Error("The number of columns must be even!");
    rowFFT = new ComplexFloatFFT_Mixed(ncols/2);
    colFFT = (nrows == (ncols/2) ? rowFFT : new ComplexFloatFFT_Mixed(nrows));
  }

  protected void checkData(float data[], int rowspan){
    if (rowspan < ncols+2)
      throw new IllegalArgumentException("The row span "+rowspan+
					 "is not long enough for ncols="+ncols);
    if (nrows*rowspan > data.length)
      throw new IllegalArgumentException("The data array is too small for "+
					 nrows+"x"+rowspan+" data.length="+data.length);}

  /** Compute the Fast Fourier Transform of data leaving the result in data. */
  public void transform(float data[]) {
    transform(data,ncols+2); }

  /** Compute the Fast Fourier Transform of data leaving the result in data. */
  public void transform(float data[], int rowspan) {
    checkData(data,rowspan);
    for(int i=0; i<nrows; i++){
      // Treat rows as complex w/ half the elements.
      rowFFT.transform(data,i*rowspan,2); 
      // Now rearrange to get the positive half of the frequencies as complex
      shuffle(data,i*rowspan,+1); }
    // Now transform half the columns as if they were complex (they are!)
    int nc = ncols/2+1;
    for(int j=0; j<nc; j++){
      colFFT.transform(data,2*j,rowspan); }}

  /** Return data in wraparound order.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public float[] toWraparoundOrder(float data[], int rowspan){
    float newdata[] = new float[2*nrows*ncols];
    int nc = ncols/2;
    for(int i=0; i<nrows; i++){
      int i0 = 2*i*ncols;
      int k0 = i*rowspan;
      int r0 = (i==0 ? 0 : (nrows-i)*2*ncols);
      newdata[i0]         = data[k0];
      newdata[i0+1]       = data[k0+1];
      newdata[i0+ncols]   = data[k0+ncols];
      newdata[i0+ncols+1] = data[k0+ncols+1];
      for(int j=1; j<nc; j++){
	newdata[i0+2*j]          = data[k0+2*j];
	newdata[i0+2*j+1]        = data[k0+2*j+1];
	newdata[r0+2*(ncols-j)]  = data[k0+2*j];
	newdata[r0+2*(ncols-j)+1]=-data[k0+2*j+1]; }}
    return newdata; }

  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.*/
  public void backtransform(float data[]) {
    backtransform(data,ncols+2); }

  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.*/
  public void backtransform(float data[], int rowspan) {
    checkData(data,rowspan);
    // First, backtransform the complex columns (half ncolums)
    int nc = ncols/2+1;
    for(int j=0; j<nc; j++){
      colFFT.backtransform(data,2*j,rowspan); }
    for(int i=0; i<nrows; i++){
      // Now unshuffle the complex frequencies in each row.
      shuffle(data,i*rowspan,-1);
      // And backtransform, as if complex, to what would appear to be real values.
      rowFFT.backtransform(data,i*rowspan,2); }}

  private void shuffle(float data[], int i0, int sign){
    int nh = ncols/2;
    int nq = ncols/4;
    float c1=0.5f, c2 = -0.5f*sign;
    double theta = sign*Math.PI/nh;
    float wtemp = (float)Math.sin(0.5*theta);
    float wpr = -2.0f*wtemp*wtemp;
    float wpi = (float)-Math.sin(theta);
    float wr = 1.0f+wpr;
    float wi = wpi;
    for(int i=1; i < nq; i++){
      int i1 = i0+2*i;
      int i3 = i0+ ncols - 2*i;
      float h1r =  c1*(data[i1  ]+data[i3]);
      float h1i =  c1*(data[i1+1]-data[i3+1]);
      float h2r = -c2*(data[i1+1]+data[i3+1]);
      float h2i =  c2*(data[i1  ]-data[i3]);
      data[i1  ] = h1r+wr*h2r-wi*h2i;
      data[i1+1] = h1i+wr*h2i+wi*h2r;
      data[i3  ] = h1r-wr*h2r+wi*h2i;
      data[i3+1] =-h1i+wr*h2i+wi*h2r;
      wtemp = wr;
      wr += wtemp*wpr-wi*wpi;
      wi += wtemp*wpi+wi*wpr; }
    float d0 = data[i0];
    if (sign == 1){
      data[i0] = d0+data[i0+1];
      data[i0+ncols] = d0-data[i0+1];
      data[i0+1]=0.0f;
      data[i0+ncols+1] = 0.0f; }
    else {
      data[i0]   = c1*(d0+data[i0+ncols]);
      data[i0+1] = c1*(d0-data[i0+ncols]);
      data[i0+ncols]=0.0f;
      data[i0+ncols+1]=0.0f; }
    if (ncols%4==0)
      data[i0+nh+1] *= -1;
  }

  /** Return the normalization factor.  
   * Multiply the elements of the backtransform'ed data to get the normalized inverse.*/
  public float normalization(){
    return 2.0f/((float) nrows*ncols); }

  /** Compute the (nomalized) inverse FFT of data, leaving it in place.*/
  public void inverse(float data[]) {
    inverse(data,ncols+2); }

  /** Compute the (nomalized) inverse FFT of data, leaving it in place.*/
  public void inverse(float data[], int rowspan) {
    backtransform(data,rowspan); 
    float norm = normalization();
    for(int i=0; i<nrows; i++)
      for(int j=0; j<ncols; j++)
	data[i*rowspan+j]   *= norm; 
  }
}
