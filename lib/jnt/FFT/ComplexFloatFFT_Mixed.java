package jnt.FFT;

/** Computes FFT's of complex, single precision data of arbitrary length n.
  * This class uses the Mixed Radix method; it has special methods to handle
  * factors 2, 3, 4, 5, 6 and 7, as well as a general factor.
  * <P>
  * This method appears to be faster than the Radix2 method, when both methods apply,
  * but requires extra storage (which ComplexDoubleFFT_Mixed manages itself).
  *
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

public class ComplexFloatFFT_Mixed extends ComplexFloatFFT{
  static final double PI = (float) Math.PI;
  static final int FORWARD = -1;
  static final int BACKWARD = +1;

  public ComplexFloatFFT_Mixed(int n){
    super(n);
    setup_wavetable(n);
  }

  public void transform(float data[], int i0, int stride) {
    checkData(data,i0,stride);
    transform_internal(data, i0, stride, FORWARD); }

  public void backtransform (float data[], int i0, int stride){
    checkData(data,i0,stride);
    transform_internal(data, i0, stride, BACKWARD); }

  /*______________________________________________________________________
   Setting up the Wavetable */

  private int factors[];
  // Reversed the last 2 levels of the twiddle array compared to what the C version had.
  private float twiddle[][][];
  private int available_factors[]={7, 6, 5, 4, 3, 2};

  void setup_wavetable(int n){

    if (n <= 0)
      throw new Error("length must be positive integer : "+n);
    this.n = n;

    factors = Factorize.factor(n, available_factors);

    double d_theta = -2.0 * PI / ((double) n);
    int product = 1;
    twiddle = new float[factors.length][][];
    for (int i = 0; i < factors.length; i++) {
      int factor = factors[i];
      int product_1 = product;	/* product_1 = p_(i-1) */
      product *= factor;
      int q = n / product;

      twiddle[i] = new float[q+1][2*(factor-1)];
      float twid[][] = twiddle[i];
      for(int j=1; j<factor; j++){
	twid[0][2*(j-1)]   = 1.0f;
	twid[0][2*(j-1)+1] = 0.0f; }
      for (int k = 1; k <= q; k++) {
	int m = 0;
	for(int j=1; j<factor; j++){
	  //	  int m = (k*j*product_1) % n;
	  m += k*product_1;
	  m %= n;
	  double theta = d_theta * m;	/*  d_theta*j*k*p_(i-1) */
	  twid[k][2*(j-1)]   = (float) Math.cos(theta);
	  twid[k][2*(j-1)+1] = (float) Math.sin(theta); }}
    }
  }

  /*______________________________________________________________________
    The main transformation driver */
  void transform_internal(float data[], int i0, int stride, int sign){

    if (n == 1) return;		/* FFT of 1 data point is the identity */

    float scratch[] = new float[2*n];
    int product = 1;
    int state = 0;
    float in[], out[];
    int istride, ostride;
    int in0, out0;

    for (int i = 0; i < factors.length; i++) {
      int factor = factors[i];
      product *= factor;

      if (state == 0) {
	in = data;
	in0 = i0;
	istride = stride;
	out = scratch;
	out0 = 0;
	ostride = 2;
	state = 1; }
      else {
	in = scratch;
	in0 = 0;
	istride = 2;
	out = data;
	out0 = i0;
	ostride = stride;
	state = 0; }

      switch(factor){
      case 2: pass_2(i,in, in0, istride, out, out0, ostride, sign, product); break;
      case 3: pass_3(i,in, in0, istride, out, out0, ostride, sign, product); break;
      case 4: pass_4(i,in, in0, istride, out, out0, ostride, sign, product); break;
      case 5: pass_5(i,in, in0, istride, out, out0, ostride, sign, product); break;
      case 6: pass_6(i,in, in0, istride, out, out0, ostride, sign, product); break;
      case 7: pass_7(i,in, in0, istride, out, out0, ostride, sign, product); break;
      default:pass_n(i,in, in0, istride, out, out0, ostride, sign, factor, product);  }
    }
    if (state == 1){	/* copy results back from scratch to data */
      for (int i = 0; i < n; i++) {
	data[i0+stride*i]   = scratch[2*i];
	data[i0+stride*i+1] = scratch[2*i+1]; }}
  }

  /*______________________________________________________________________*/

  void pass_2(int fi, 
	      float in[],  int in0, int istride,
	      float out[], int out0, int ostride,
	      int sign, int product) {
    int k, k1;

    int factor = 2;
    int m = n / factor;
    int q = n / product;
    int product_1 = product / factor;

    int di = istride * m;
    int dj = ostride * product_1;
    int i = in0, j = out0;
    float x_real, x_imag;
    for (k = 0; k < q; k++) {
      float twids[] = twiddle[fi][k];
      float w_real =       twids[0];
      float w_imag = -sign*twids[1];

      for (k1 = 0; k1 < product_1; k1++) {
	float z0_real = in[i];
	float z0_imag = in[i+1];
	float z1_real = in[i+di];
	float z1_imag = in[i+di+1];
	i += istride;

	/* compute x = W(2) z */

	/* apply twiddle factors */
	  
	/* out0 = 1 * (z0 + z1) */
	out[j]   = z0_real + z1_real;
	out[j+1] = z0_imag + z1_imag;
	  
	/* out1 = w * (z0 - z1) */
	x_real = z0_real - z1_real;
	x_imag = z0_imag - z1_imag;
	out[j+dj]   = w_real * x_real - w_imag * x_imag;
	out[j+dj+1] = w_real * x_imag + w_imag * x_real;
	  
	j += ostride;
      }
      j += (factor-1)*dj;
    }}
  /*______________________________________________________________________*/

  void pass_3(int fi, 
	      float in[],  int in0, int istride,
	      float out[], int out0, int ostride,
	      int sign, int product) {
      int k, k1;

      int factor = 3;
      int m = n / factor;
      int q = n / product;
      int product_1 = product / factor;
      int jump = (factor - 1) * product_1;

      float tau = (float)(sign * Math.sqrt(3.0) / 2.0);
      int di = istride * m;
      int dj = ostride * product_1;
      int i = in0, j = out0;
      float x_real, x_imag;
      for (k = 0; k < q; k++) {
	float twids[] = twiddle[fi][k];
	float w1_real =       twids[0];
	float w1_imag = -sign*twids[1];
	float w2_real =       twids[2];
	float w2_imag = -sign*twids[3];

	for (k1 = 0; k1 < product_1; k1++) {
	  float z0_real = in[i];
	  float z0_imag = in[i+1];
	  float z1_real = in[i+di];
	  float z1_imag = in[i+di+1];
	  float z2_real = in[i+2*di];
	  float z2_imag = in[i+2*di+1];
	  i += istride;

	  /* compute x = W(3) z */

	  /* t1 = z1 + z2 */
	  float t1_real = z1_real + z2_real;
	  float t1_imag = z1_imag + z2_imag;
	  
	  /* t2 = z0 - t1/2 */
	  float t2_real = z0_real - t1_real / 2.0f;
	  float t2_imag = z0_imag - t1_imag / 2.0f;
	  
	  /* t3 = (+/-) sin(pi/3)*(z1 - z2) */
	  float t3_real = tau * (z1_real - z2_real);
	  float t3_imag = tau * (z1_imag - z2_imag);
	  
  	  /* apply twiddle factors */

	  /* out0 = 1 * (z0 + t1) */
	  out[j]   = z0_real + t1_real;
	  out[j+1] = z0_imag + t1_imag;
	  
	  /* out1 = w1 * (t2 + i t3) */
	  x_real = t2_real - t3_imag;
	  x_imag = t2_imag + t3_real;
	  out[j+dj]   = w1_real * x_real - w1_imag * x_imag;
	  out[j+dj+1] = w1_real * x_imag + w1_imag * x_real;
	  
	  /* out2 = w2 * (t2 - i t3) */
	  x_real = t2_real + t3_imag;
	  x_imag = t2_imag - t3_real;
	  out[j+2*dj]   = w2_real * x_real - w2_imag * x_imag;
	  out[j+2*dj+1] = w2_real * x_imag + w2_imag * x_real;
  
	  j += ostride;
	}
	j += (factor-1) * dj;
      }}
  /*______________________________________________________________________*/

  void pass_4(int fi, 
	      float in[],  int in0, int istride,
	      float out[], int out0, int ostride,
	      int sign, int product) {
    int k, k1;

    int factor = 4;
    int m = n / factor;
    int q = n / product;
    int p_1 = product / factor;
    int jump = (factor - 1) * p_1;

    int i = in0, j = out0;
    int di = istride * m;
    int dj = ostride * p_1;
    float x_real, x_imag;
    for (k = 0; k < q; k++) {
      float twids[] = twiddle[fi][k];
      float w1_real =       twids[0];
      float w1_imag = -sign*twids[1];
      float w2_real =       twids[2];
      float w2_imag = -sign*twids[3];
      float w3_real =       twids[4];
      float w3_imag = -sign*twids[5];

      for (k1 = 0; k1 < p_1; k1++) {
	float z0_real = in[i];
	float z0_imag = in[i+1];
	float z1_real = in[i+di];
	float z1_imag = in[i+di+1];
	float z2_real = in[i+2*di];
	float z2_imag = in[i+2*di+1];
	float z3_real = in[i+3*di];
	float z3_imag = in[i+3*di+1];
	i += istride;

	/* compute x = W(4) z */
	  
	/* t1 = z0 + z2 */
	float t1_real = z0_real + z2_real;
	float t1_imag = z0_imag + z2_imag;
	  
	/* t2 = z1 + z3 */
	float t2_real = z1_real + z3_real;
	float t2_imag = z1_imag + z3_imag;
	  
	/* t3 = z0 - z2 */
	float t3_real = z0_real - z2_real;
	float t3_imag = z0_imag - z2_imag;
	  
	/* t4 = (+/-) (z1 - z3) */
	float t4_real = sign * (z1_real - z3_real);
	float t4_imag = sign * (z1_imag - z3_imag);

	/* apply twiddle factors */

	/* out0 = 1 * (t1 + t2) */
	out[j]   = t1_real + t2_real;
	out[j+1] = t1_imag + t2_imag;

	/* out1 = w1 * (t3 + i t4) */
	x_real = t3_real - t4_imag;
	x_imag = t3_imag + t4_real;
	out[j + dj]   = w1_real * x_real - w1_imag * x_imag;
	out[j + dj+1] = w1_real * x_imag + w1_imag * x_real;
	  
	/* out2 = w2 * (t1 - t2) */
	x_real = t1_real - t2_real;
	x_imag = t1_imag - t2_imag;
	out[j + 2 * dj]   = w2_real * x_real - w2_imag * x_imag;
	out[j + 2 * dj+1] = w2_real * x_imag + w2_imag * x_real;
	  
	/* out3 = w3 * (t3 - i t4) */
	x_real = t3_real + t4_imag;
	x_imag = t3_imag - t4_real;
	out[j + 3 * dj]   = w3_real * x_real - w3_imag * x_imag;
	out[j + 3 * dj+1] = w3_real * x_imag + w3_imag * x_real;
	  
	j += ostride;
      }
      j += (factor - 1)*dj;
    }}
  /*______________________________________________________________________*/

  void pass_5(int fi, 
	      float in[],  int in0, int istride,
	      float out[], int out0, int ostride,
	      int sign, int product) {
    int k, k1;

    int factor = 5;
    int m = n / factor;
    int q = n / product;
    int p_1 = product / factor;
    int jump = (factor - 1) * p_1;

    float tau = (float)(Math.sqrt (5.0) / 4.0);
    float sin_2pi_by_5 =  (float)(sign * Math.sin (2.0 * PI / 5.0));
    float sin_2pi_by_10 = (float)(sign * Math.sin (2.0 * PI / 10.0));
    int i = in0, j = out0;
    int di = istride * m;
    int dj = ostride * p_1;
    float x_real, x_imag;
    for (k = 0; k < q; k++) {
      float twids[] = twiddle[fi][k];
      float w1_real =       twids[0];
      float w1_imag = -sign*twids[1];
      float w2_real =       twids[2];
      float w2_imag = -sign*twids[3];
      float w3_real =       twids[4];
      float w3_imag = -sign*twids[5];
      float w4_real =       twids[6];
      float w4_imag = -sign*twids[7];

      for (k1 = 0; k1 < p_1; k1++) {
	float z0_real = in[i];
	float z0_imag = in[i+1];
	float z1_real = in[i + di];
	float z1_imag = in[i + di+1];
	float z2_real = in[i + 2*di];
	float z2_imag = in[i + 2*di+1];
	float z3_real = in[i + 3*di];
	float z3_imag = in[i + 3*di+1];
	float z4_real = in[i + 4*di];
	float z4_imag = in[i + 4*di+1];
	i += istride;

	/* compute x = W(5) z */

	/* t1 = z1 + z4 */
	float t1_real = z1_real + z4_real;
	float t1_imag = z1_imag + z4_imag;
	  
	/* t2 = z2 + z3 */
	float t2_real = z2_real + z3_real;
	float t2_imag = z2_imag + z3_imag;
	  
	/* t3 = z1 - z4 */
	float t3_real = z1_real - z4_real;
	float t3_imag = z1_imag - z4_imag;
	  
	/* t4 = z2 - z3 */
	float t4_real = z2_real - z3_real;
	float t4_imag = z2_imag - z3_imag;
	  
	/* t5 = t1 + t2 */
	float t5_real = t1_real + t2_real;
	float t5_imag = t1_imag + t2_imag;
	  
	/* t6 = (sqrt(5)/4)(t1 - t2) */
	float t6_real = tau * (t1_real - t2_real);
	float t6_imag = tau * (t1_imag - t2_imag);
	  
	/* t7 = z0 - ((t5)/4) */
	float t7_real = z0_real - t5_real / 4.0f;
	float t7_imag = z0_imag - t5_imag / 4.0f;
	  
	/* t8 = t7 + t6 */
	float t8_real = t7_real + t6_real;
	float t8_imag = t7_imag + t6_imag;
	  
	/* t9 = t7 - t6 */
	float t9_real = t7_real - t6_real;
	float t9_imag = t7_imag - t6_imag;
	  
	/* t10 = sin(2 pi/5) t3 + sin(2 pi/10) t4 */
	float t10_real = sin_2pi_by_5 * t3_real + sin_2pi_by_10 * t4_real;
	float t10_imag = sin_2pi_by_5 * t3_imag + sin_2pi_by_10 * t4_imag;
	  
	/* t11 = sin(2 pi/10) t3 - sin(2 pi/5) t4 */
	float t11_real = sin_2pi_by_10 * t3_real - sin_2pi_by_5 * t4_real;
	float t11_imag = sin_2pi_by_10 * t3_imag - sin_2pi_by_5 * t4_imag;
      
	/* apply twiddle factors */
	  
	/* out0 = 1 * (z0 + t5) */
	out[j]   = z0_real + t5_real;
	out[j+1] = z0_imag + t5_imag;
	  
	/* out1 = w1 * (t8 + i t10) */
	x_real = t8_real - t10_imag;
	x_imag = t8_imag + t10_real;
	out[j + dj]   = w1_real * x_real - w1_imag * x_imag;
	out[j + dj+1] = w1_real * x_imag + w1_imag * x_real;
	  
	/* out2 = w2 * (t9 + i t11) */
	x_real = t9_real - t11_imag;
	x_imag = t9_imag + t11_real;
	out[j+2*dj]   = w2_real * x_real - w2_imag * x_imag;
	out[j+2*dj+1] = w2_real * x_imag + w2_imag * x_real;
	  
	/* out3 = w3 * (t9 - i t11) */
	x_real = t9_real + t11_imag;
	x_imag = t9_imag - t11_real;
	out[j+3*dj]   = w3_real * x_real - w3_imag * x_imag;
	out[j+3*dj+1] = w3_real * x_imag + w3_imag * x_real;
	  
	/* out4 = w4 * (t8 - i t10) */
	x_real = t8_real + t10_imag;
	x_imag = t8_imag - t10_real;
	out[j+4*dj]   = w4_real * x_real - w4_imag * x_imag;
	out[j+4*dj+1] = w4_real * x_imag + w4_imag * x_real;
	  
	j += ostride;
      }
      j += (factor - 1)*dj;
    }}
  /*______________________________________________________________________*/

  void pass_6(int fi, 
	      float in[],  int in0, int istride,
	      float out[], int out0, int ostride,
	      int sign, int product) {

    int k, k1;

    int factor = 6;
    int m = n / factor;
    int q = n / product;
    int p_1 = product / factor;
    int jump = (factor - 1) * p_1;

    float tau = (float)(sign * Math.sqrt (3.0) / 2.0);
    int i = in0, j = out0;
    int di = istride * m;
    int dj = ostride * p_1;
    float x_real, x_imag;
    for (k = 0; k < q; k++) {
      float twids[] = twiddle[fi][k];
      float w1_real =       twids[0];
      float w1_imag = -sign*twids[1];
      float w2_real =       twids[2];
      float w2_imag = -sign*twids[3];
      float w3_real =       twids[4];
      float w3_imag = -sign*twids[5];
      float w4_real =       twids[6];
      float w4_imag = -sign*twids[7];
      float w5_real =       twids[8];
      float w5_imag = -sign*twids[9];

      for (k1 = 0; k1 < p_1; k1++) {
	float z0_real = in[i];
	float z0_imag = in[i+1];
	float z1_real = in[i+di];
	float z1_imag = in[i+di+1];
	float z2_real = in[i+2*di];
	float z2_imag = in[i+2*di+1];
	float z3_real = in[i+3*di];
	float z3_imag = in[i+3*di+1];
	float z4_real = in[i+4*di];
	float z4_imag = in[i+4*di+1];
	float z5_real = in[i+5*di];
	float z5_imag = in[i+5*di+1];
	i += istride;

	/* compute x = W(6) z */

	/* W(6) is a combination of sums and differences of W(3) acting
	   on the even and odd elements of z */
	  
	/* ta1 = z2 + z4 */
	float ta1_real = z2_real + z4_real;
	float ta1_imag = z2_imag + z4_imag;
	  
	/* ta2 = z0 - ta1/2 */
	float ta2_real = z0_real - ta1_real / 2;
	float ta2_imag = z0_imag - ta1_imag / 2;
	  
	/* ta3 = (+/-) sin(pi/3)*(z2 - z4) */
	float ta3_real = tau * (z2_real - z4_real);
	float ta3_imag = tau * (z2_imag - z4_imag);
	  
	/* a0 = z0 + ta1 */
	float a0_real = z0_real + ta1_real;
	float a0_imag = z0_imag + ta1_imag;
	  
	/* a1 = ta2 + i ta3 */
	float a1_real = ta2_real - ta3_imag;
	float a1_imag = ta2_imag + ta3_real;
	  
	/* a2 = ta2 - i ta3 */
	float a2_real = ta2_real + ta3_imag;
	float a2_imag = ta2_imag - ta3_real;
	  
	/* tb1 = z5 + z1 */
	float tb1_real = z5_real + z1_real;
	float tb1_imag = z5_imag + z1_imag;
	  
	/* tb2 = z3 - tb1/2 */
	float tb2_real = z3_real - tb1_real / 2;
	float tb2_imag = z3_imag - tb1_imag / 2;
	  
	/* tb3 = (+/-) sin(pi/3)*(z5 - z1) */
	float tb3_real = tau * (z5_real - z1_real);
	float tb3_imag = tau * (z5_imag - z1_imag);
	  
	/* b0 = z3 + tb1 */
	float b0_real = z3_real + tb1_real;
	float b0_imag = z3_imag + tb1_imag;
	  
	/* b1 = tb2 + i tb3 */
	float b1_real = tb2_real - tb3_imag;
	float b1_imag = tb2_imag + tb3_real;
	  
	/* b2 = tb2 - i tb3 */
	float b2_real = tb2_real + tb3_imag;
	float b2_imag = tb2_imag - tb3_real;

	/* apply twiddle factors */
	  
	/* out0 = 1 * (a0 + b0) */
	out[j]   = a0_real + b0_real;
	out[j+1] = a0_imag + b0_imag;
	  
	/* out1 = w1 * (a1 - b1) */
	x_real = a1_real - b1_real;
	x_imag = a1_imag - b1_imag;
	out[j+dj]   = w1_real * x_real - w1_imag * x_imag;
	out[j+dj+1] = w1_real * x_imag + w1_imag * x_real;
	  
	/* out2 = w2 * (a2 + b2) */
	x_real = a2_real + b2_real;
	x_imag = a2_imag + b2_imag;
	out[j+2*dj]   = w2_real * x_real - w2_imag * x_imag;
	out[j+2*dj+1] = w2_real * x_imag + w2_imag * x_real;
	  
	/* out3 = w3 * (a0 - b0) */
	x_real = a0_real - b0_real;
	x_imag = a0_imag - b0_imag;
	out[j+3*dj]   = w3_real * x_real - w3_imag * x_imag;
	out[j+3*dj+1] = w3_real * x_imag + w3_imag * x_real;
	  
	/* out4 = w4 * (a1 + b1) */
	x_real = a1_real + b1_real;
	x_imag = a1_imag + b1_imag;
	out[j+4*dj]   = w4_real * x_real - w4_imag * x_imag;
	out[j+4*dj+1] = w4_real * x_imag + w4_imag * x_real;
	  
	/* out5 = w5 * (a2 - b2) */
	x_real = a2_real - b2_real;
	x_imag = a2_imag - b2_imag;
	out[j+5*dj]   = w5_real * x_real - w5_imag * x_imag;
	out[j+5*dj+1] = w5_real * x_imag + w5_imag * x_real;

	j += ostride;
      }
      j += (factor - 1)*dj;
    }}
  /*______________________________________________________________________*/

  void pass_7(int fi, 
	      float in[],  int in0, int istride,
	      float out[], int out0, int ostride,
	      int sign, int product){

    int k, k1;

    int factor = 7;
    int m = n / factor;
    int q = n / product;
    int p_1 = product / factor;
    int jump = (factor - 1) * p_1;

    float c1 = (float)Math.cos(1.0 * 2.0 * PI / 7.0);
    float c2 = (float)Math.cos(2.0 * 2.0 * PI / 7.0);
    float c3 = (float)Math.cos(3.0 * 2.0 * PI / 7.0);

    float s1 = (float)((-sign)*Math.sin(1.0 * 2.0 * PI / 7.0));
    float s2 = (float)((-sign)*Math.sin(2.0 * 2.0 * PI / 7.0));
    float s3 = (float)((-sign)*Math.sin(3.0 * 2.0 * PI / 7.0));
    int i = in0, j = out0;
    int di = istride * m;
    int dj = ostride * p_1;
    float x_real, x_imag;
    for (k = 0; k < q; k++) {
      float twids[] = twiddle[fi][k];
      float w1_real =       twids[0];
      float w1_imag = -sign*twids[1];
      float w2_real =       twids[2];
      float w2_imag = -sign*twids[3];
      float w3_real =       twids[4];
      float w3_imag = -sign*twids[5];
      float w4_real =       twids[6];
      float w4_imag = -sign*twids[7];
      float w5_real =       twids[8];
      float w5_imag = -sign*twids[9];
      float w6_real =       twids[10];
      float w6_imag = -sign*twids[11];

      for (k1 = 0; k1 < p_1; k1++) {
	float z0_real = in[i];
	float z0_imag = in[i+1];
	float z1_real = in[i+di];
	float z1_imag = in[i+di+1];
	float z2_real = in[i+2*di];
	float z2_imag = in[i+2*di+1];
	float z3_real = in[i+3*di];
	float z3_imag = in[i+3*di+1];
	float z4_real = in[i+4*di];
	float z4_imag = in[i+4*di+1];
	float z5_real = in[i+5*di];
	float z5_imag = in[i+5*di+1];
	float z6_real = in[i+6*di];
	float z6_imag = in[i+6*di+1];
	i += istride;

	/* compute x = W(7) z */
	  
	/* t0 = z1 + z6 */
	float t0_real = z1_real + z6_real;
	float t0_imag = z1_imag + z6_imag; 
	  
	/* t1 = z1 - z6 */
	float t1_real = z1_real - z6_real;
	float t1_imag = z1_imag - z6_imag; 
	  
	/* t2 = z2 + z5 */
	float t2_real = z2_real + z5_real;
	float t2_imag = z2_imag + z5_imag; 
	  
	/* t3 = z2 - z5 */
	float t3_real = z2_real - z5_real;
	float t3_imag = z2_imag - z5_imag; 
	  
	/* t4 = z4 + z3 */
	float t4_real = z4_real + z3_real;
	float t4_imag = z4_imag + z3_imag; 
	  
	/* t5 = z4 - z3 */
	float t5_real = z4_real - z3_real;
	float t5_imag = z4_imag - z3_imag; 
	  
	/* t6 = t2 + t0 */
	float t6_real = t2_real + t0_real;
	float t6_imag = t2_imag + t0_imag;
	  
	/* t7 = t5 + t3 */
	float t7_real = t5_real + t3_real;
	float t7_imag = t5_imag + t3_imag;
	  
	/* b0 = z0 + t6 + t4 */
	float b0_real = z0_real + t6_real + t4_real;
	float b0_imag = z0_imag + t6_imag + t4_imag;
	  
	/* b1 = ((cos(2pi/7) + cos(4pi/7) + cos(6pi/7))/3-1) (t6 + t4) */
	float b1_real = (((c1 + c2 + c3)/3.0f - 1.0f) * (t6_real + t4_real));
	float b1_imag = (((c1 + c2 + c3)/3.0f - 1.0f) * (t6_imag + t4_imag));
	  
	/* b2 = ((2*cos(2pi/7) - cos(4pi/7) - cos(6pi/7))/3) (t0 - t4) */
	float b2_real = (((2.0f * c1 - c2 - c3)/3.0f) * (t0_real - t4_real));
	float b2_imag = (((2.0f * c1 - c2 - c3)/3.0f) * (t0_imag - t4_imag));
	  
	/* b3 = ((cos(2pi/7) - 2*cos(4pi/7) + cos(6pi/7))/3) (t4 - t2) */
	float b3_real = (((c1 - 2.0f*c2 + c3)/3.0f) * (t4_real - t2_real));
	float b3_imag = (((c1 - 2.0f*c2 + c3)/3.0f) * (t4_imag - t2_imag));
	  
	/* b4 = ((cos(2pi/7) + cos(4pi/7) - 2*cos(6pi/7))/3) (t2 - t0) */
	float b4_real = (((c1 + c2 - 2.0f * c3)/3.0f) * (t2_real - t0_real));
	float b4_imag = (((c1 + c2 - 2.0f * c3)/3.0f) * (t2_imag - t0_imag));
	  
	/* b5 = sign * ((sin(2pi/7) + sin(4pi/7) - sin(6pi/7))/3) (t7 + t1) */
	float b5_real = ((s1 + s2 - s3)/3.0f) * (t7_real + t1_real);
	float b5_imag = ((s1 + s2 - s3)/3.0f) * (t7_imag + t1_imag);
	  
	/* b6 = sign * ((2sin(2pi/7) - sin(4pi/7) + sin(6pi/7))/3) (t1 - t5) */
	float b6_real = ((2.0f * s1 - s2 + s3)/3.0f) * (t1_real - t5_real);
	float b6_imag = ((2.0f * s1 - s2 + s3)/3.0f) * (t1_imag - t5_imag);
	  
	/* b7 = sign * ((sin(2pi/7) - 2sin(4pi/7) - sin(6pi/7))/3) (t5 - t3) */
	float b7_real = ((s1 - 2.0f * s2 - s3)/3.0f) * (t5_real - t3_real);
	float b7_imag = ((s1 - 2.0f * s2 - s3)/3.0f) * (t5_imag - t3_imag);
	  
	/* b8 = sign * ((sin(2pi/7) + sin(4pi/7) + 2sin(6pi/7))/3) (t3 - t1) */
	float b8_real = ((s1 + s2 + 2.0f * s3)/3.0f) * (t3_real - t1_real);
	float b8_imag = ((s1 + s2 + 2.0f * s3)/3.0f) * (t3_imag - t1_imag);
	  
	  
	/* T0 = b0 + b1 */
	float T0_real = b0_real + b1_real;
	float T0_imag = b0_imag + b1_imag;
	  
	/* T1 = b2 + b3 */
	float T1_real = b2_real + b3_real;
	float T1_imag = b2_imag + b3_imag;
	  
	/* T2 = b4 - b3 */
	float T2_real = b4_real - b3_real;
	float T2_imag = b4_imag - b3_imag;
	  
	/* T3 = -b2 - b4 */
	float T3_real = -b2_real - b4_real;
	float T3_imag = -b2_imag - b4_imag;
	  
	/* T4 = b6 + b7 */
	float T4_real = b6_real + b7_real;
	float T4_imag = b6_imag + b7_imag;
	  
	/* T5 = b8 - b7 */
	float T5_real = b8_real - b7_real;
	float T5_imag = b8_imag - b7_imag;
	  
	/* T6 = -b8 - b6 */
	float T6_real = -b8_real - b6_real;
	float T6_imag = -b8_imag - b6_imag;
	  
	/* T7 = T0 + T1 */
	float T7_real = T0_real + T1_real;
	float T7_imag = T0_imag + T1_imag;
	  
	/* T8 = T0 + T2 */
	float T8_real = T0_real + T2_real;
	float T8_imag = T0_imag + T2_imag;
	  
	/* T9 = T0 + T3 */
	float T9_real = T0_real + T3_real;
	float T9_imag = T0_imag + T3_imag;
	  
	/* T10 = T4 + b5 */
	float T10_real = T4_real + b5_real;
	float T10_imag = T4_imag + b5_imag;
	  
	/* T11 = T5 + b5 */
	float T11_real = T5_real + b5_real;
	float T11_imag = T5_imag + b5_imag;
	  
	/* T12 = T6 + b5 */
	float T12_real = T6_real + b5_real;
	float T12_imag = T6_imag + b5_imag;
	  
	/* apply twiddle factors */
	  
	/* out0 = 1 * b0 */
	out[j]   = b0_real;
	out[j+1] = b0_imag;
	  
	/* out1 = w1 * (T7 - i T10) */
	x_real = T7_real + T10_imag;
	x_imag = T7_imag - T10_real;
	out[j+dj]   = w1_real * x_real - w1_imag * x_imag;
	out[j+dj+1] = w1_real * x_imag + w1_imag * x_real;

	/* out2 = w2 * (T9 - i T12) */
	x_real = T9_real + T12_imag;
	x_imag = T9_imag - T12_real;
	out[j+2*dj]   = w2_real * x_real - w2_imag * x_imag;
	out[j+2*dj+1] = w2_real * x_imag + w2_imag * x_real;

	/* out3 = w3 * (T8 + i T11) */
	x_real = T8_real - T11_imag;
	x_imag = T8_imag + T11_real;
	out[j+3*dj]   = w3_real * x_real - w3_imag * x_imag;
	out[j+3*dj+1] = w3_real * x_imag + w3_imag * x_real;

	/* out4 = w4 * (T8 - i T11) */
	x_real = T8_real + T11_imag;
	x_imag = T8_imag - T11_real;
	out[j+4*dj]   = w4_real * x_real - w4_imag * x_imag;
	out[j+4*dj+1] = w4_real * x_imag + w4_imag * x_real;

	/* out5 = w5 * (T9 + i T12) */
	x_real = T9_real - T12_imag;
	x_imag = T9_imag + T12_real;
	out[j+5*dj]   = w5_real * x_real - w5_imag * x_imag;
	out[j+5*dj+1] = w5_real * x_imag + w5_imag * x_real;
	  
	/* out6 = w6 * (T7 + i T10) */
	x_real = T7_real - T10_imag;
	x_imag = T7_imag + T10_real;
	out[j+6*dj]   = w6_real * x_real - w6_imag * x_imag;
	out[j+6*dj+1] = w6_real * x_imag + w6_imag * x_real;
	  
	j += ostride;
      }
      j += (factor - 1)*dj;
    }}
  /*______________________________________________________________________*/

  void pass_n(int fi, 
	      float in[],  int in0, int istride,
	      float out[], int out0, int ostride,
	      int sign, int factor, int product){
    int i = 0, j = 0;
    int k, k1;

    int m = n / factor;
    int q = n / product;
    int p_1 = product / factor;
    int jump = (factor - 1) * p_1;

    int e, e1;

    for (i = 0; i < m; i++) {
      out[out0+ostride*i]   = in[in0+istride*i];
      out[out0+ostride*i+1] = in[in0+istride*i+1]; }

    for (e = 1; e < (factor - 1) / 2 + 1; e++) {
      for (i = 0; i < m; i++) {
	int idx = i + e * m;
	int idxc = i + (factor - e) * m;
	out[out0+ostride*idx]    = in[in0+istride*idx]   + in[in0+istride*idxc];
	out[out0+ostride*idx+1]  = in[in0+istride*idx+1] + in[in0+istride*idxc+1];
	out[out0+ostride*idxc]   = in[in0+istride*idx]   - in[in0+istride*idxc];
	out[out0+ostride*idxc+1] = in[in0+istride*idx+1] - in[in0+istride*idxc+1];  }}

    /* e = 0 */

    for (i=0; i<m; i++) {
      in[in0+istride*i]   = out[out0+ostride*i];
      in[in0+istride*i+1] = out[out0+ostride*i+1]; }

    for (e1 = 1; e1 < (factor - 1) / 2 + 1; e1++){
      for (i = 0; i < m; i++){
	in[in0+istride*i]   += out[out0+ostride*(i + e1*m)];
	in[in0+istride*i+1] += out[out0+ostride*(i + e1*m)+1]; }}

    float twiddl[] = twiddle[fi][q];

    for (e = 1; e < (factor-1)/2 + 1; e++){
      int idx = e;
      float w_real, w_imag;

      int em = e * m;
      int ecm = (factor - e) * m;

      for (i = 0; i < m; i++) {
	in[in0+istride*(i+em)]    = out[out0+ostride*i];
	in[in0+istride*(i+em)+1]  = out[out0+ostride*i+1];
	in[in0+istride*(i+ecm)]   = out[out0+ostride*i];
	in[in0+istride*(i+ecm)+1] = out[out0+ostride*i+1];
      }

      for (e1 = 1; e1 < (factor - 1) / 2 + 1; e1++) {
	if (idx == 0) {
	  w_real = 1;
	  w_imag = 0; }
	else {
	  w_real =       twiddl[2*(idx-1)];
	  w_imag = -sign*twiddl[2*(idx-1)+1]; }
	for (i = 0; i < m; i++) {
	  float ap = w_real * out[out0+ostride*(i + e1 * m)];
	  float am = w_imag * out[out0+ostride*(i + (factor - e1) *m)+1];

	  float bp = w_real * out[out0+ostride*(i + e1 * m)+1];
	  float bm = w_imag * out[out0+ostride*(i + (factor - e1) *m)];

	  in[in0+istride*(i + em)]    += (ap - am);
	  in[in0+istride*(i + em)+1]  += (bp + bm);
	  in[in0+istride*(i + ecm)]   += (ap + am);
	  in[in0+istride*(i + ecm)+1] += (bp - bm);
	}
	idx += e;
	idx %= factor;
      }
    }

    i = 0;
    j = 0;

    /* k = 0 */
    for (k1 = 0; k1 < p_1; k1++) {
      out[out0+ostride*k1]   = in[in0+istride*k1];
      out[out0+ostride*k1+1] = in[in0+istride*k1+1]; }

    for (e1 = 1; e1 < factor; e1++){
      for (k1 = 0; k1 < p_1; k1++){
	out[out0+ostride*(k1 + e1 * p_1)]   = in[in0+istride*(k1 + e1 * m)];
	out[out0+ostride*(k1 + e1 * p_1)+1] = in[in0+istride*(k1 + e1 * m)+1];
      }}

    i = p_1;
    j = product;

    for (k = 1; k < q; k++) {
      for (k1 = 0; k1 < p_1; k1++) {
	out[out0+ostride*j]   = in[in0+istride*i];
	out[out0+ostride*j+1] = in[in0+istride*i+1];
	i++;
	j++;
      }
      j += jump;
    }

    i = p_1;
    j = product;

    for (k = 1; k < q; k++) {
      twiddl = twiddle[fi][k];
      for (k1 = 0; k1 < p_1; k1++) {
	for (e1 = 1; e1 < factor; e1++) {
	  float x_real = in[in0+istride*(i + e1 * m)];
	  float x_imag = in[in0+istride*(i + e1 * m)+1];

	  float w_real =       twiddl[2*(e1-1)];
	  float w_imag = -sign*twiddl[2*(e1-1)+1];

	  out[out0+ostride*(j + e1 * p_1)]   = w_real * x_real - w_imag * x_imag;
	  out[out0+ostride*(j + e1 * p_1)+1] = w_real * x_imag + w_imag * x_real;
	}
 	i++;
	j++;
      }
      j += jump;
    }
  }


}
