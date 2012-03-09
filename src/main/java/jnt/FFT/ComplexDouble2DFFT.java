package jnt.FFT;
/** Computes the FFT of 2 dimensional complex, double precision data.
  * The data is stored in a 1-dimensional array in Row-Major order.
  * The physical layout in the array data, of the mathematical data d[i,j] is as follows:
  *<PRE>
  *    Re(d[i,j]) = data[i*rowspan + 2*j]
  *    Im(d[i,j]) = data[i*rowspan + 2*j + 1]
  *</PRE>
  * where <code>rowspan</code> must be at least 2*ncols (it defaults to 2*ncols).
  * The transformed data is returned in the original data array in 
  * <a href="package-summary.html#wraparound">wrap-around</A> order along each dimension.
  *
  * @author Bruce R. Miller bruce.miller@nist.gov
  * @author Contribution of the National Institute of Standards and Technology,
  * @author not subject to copyright.
  */ 
public class ComplexDouble2DFFT {
  int nrows;
  int ncols;
  ComplexDoubleFFT rowFFT, colFFT;

  /** Create an FFT for transforming nrows*ncols points of Complex, double precision
    * data. */
  public ComplexDouble2DFFT(int nrows, int ncols) {
    if ((nrows <= 0) || (ncols <= 0))
      throw new IllegalArgumentException("The array dimensions >=0 : "+nrows+","+ncols);
    this.nrows = nrows;
    this.ncols = ncols;
    rowFFT = new ComplexDoubleFFT_Mixed(ncols);
    colFFT = (nrows == ncols ? rowFFT : new ComplexDoubleFFT_Mixed(nrows));
  }

  protected void checkData(double data[], int rowspan){
    if (rowspan < 2*ncols)
      throw new IllegalArgumentException("The row span "+rowspan+
					 "is shorter than the row length "+2*ncols);
    if (nrows*rowspan > data.length)
      throw new IllegalArgumentException("The data array is too small for "+
					 nrows+"x"+rowspan+" data.length="+data.length);}

  /** Compute the Fast Fourier Transform of data leaving the result in data.
    * The array data must be dimensioned (at least) 2*nrows*ncols, consisting of
    * alternating real and imaginary parts. */
  public void transform(double data[]) {
    transform(data,2*ncols); }

  /** Compute the Fast Fourier Transform of data leaving the result in data.
    * The array data must be dimensioned (at least) 2*nrows*ncols, consisting of
    * alternating real and imaginary parts. */
  public void transform(double data[], int rowspan) {
    checkData(data,rowspan);
    for(int i=0; i<nrows; i++){
      rowFFT.transform(data,i*rowspan,2); }
    for(int j=0; j<ncols; j++){
      colFFT.transform(data,2*j,rowspan); }}

  /** Return data in wraparound order.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public double[] toWraparoundOrder(double data[]){
    return data; }

  /** Return data in wraparound order.
    * rowspan is used to traverse data; the new array is in 
    * packed (rowspan = 2*ncols) format.
    * @see <a href="package-summary.html#wraparound">wraparound format</A> */
  public double[] toWraparoundOrder(double data[], int rowspan){
    if (rowspan == 2*ncols) return data;
    double newdata[] = new double[2*nrows*ncols];
    for(int i=0; i<nrows; i++)
      for(int j=0; j<ncols; j++) {
	newdata[i*2*ncols + 2*j]=data[i*rowspan +2*j];
	newdata[i*2*ncols + 2*j+1]=data[i*rowspan +2*j+1]; }
    return newdata; }

  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.*/
  public void backtransform(double data[]) {
    backtransform(data,2*ncols); }

  /** Compute the (unnomalized) inverse FFT of data, leaving it in place.*/
  public void backtransform(double data[], int rowspan) {
    checkData(data,rowspan);
    for(int j=0; j<ncols; j++){
      colFFT.backtransform(data,2*j,rowspan); }
    for(int i=0; i<nrows; i++){
      rowFFT.backtransform(data,i*rowspan,2); }}

  /** Return the normalization factor.  
   * Multiply the elements of the backtransform'ed data to get the normalized inverse.*/
  public double normalization(){
    return 1.0/((double) nrows*ncols); }

  /** Compute the (nomalized) inverse FFT of data, leaving it in place.*/
  public void inverse(double data[]) {
    inverse(data,2*ncols); }

  /** Compute the (nomalized) inverse FFT of data, leaving it in place.*/
  public void inverse(double data[], int rowspan) {
    backtransform(data,rowspan); 
    double norm = normalization();
    for(int i=0; i<nrows; i++){
      for(int j=0; j<ncols; j++){
	data[i*rowspan + 2*j]   *= norm; 
	data[i*rowspan + 2*j+1] *= norm; }}}

}
