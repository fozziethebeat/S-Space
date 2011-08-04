package edu.ucla.sspace.fft;

import edu.ucla.sspace.vector.DoubleVector;


/**
 * Computes  FFT's of {@code DoubleVector}s.
  * The physical layout of the mathematical data d[i] in the array data is as
  * follows:
  *<PRE>
  *    d[i] = data[i0 + stride*i]
  *</PRE>
  * The FFT (D[i]) of real data (d[i]) is complex, but restricted by symmetry:
  *<PRE>
  *    D[n-i] = conj(D[i])
  *</PRE>
  *
  * @author Keith Stevens
  * @author Derived from JNT FFT code by Bruce R. Miller bruce.miller@nist.gov
  * @author Derived from GSL (Gnu Scientific Library)
  * @author Contribution of the National Institute of Standards and Technology,
  */

public class FastFourierTransform {

    /**
     * Create an FFT for transforming n points of real, double precision data.
     * n must be an integral power of 2.
     */
    public static int checkFactor(int n){
        int logn = log2(n);
        if (logn < 0)
          throw new IllegalArgumentException(n+" is not a power of 2");
        return logn;
    }

    /**
     * Compute the Fast Fourier Transform of data leaving the result in data.
     */
    public static void transform(DoubleVector data) {
        int i0 = 0;
        int stride = 1;
        checkData(data,i0,stride);
        int p, p_1, q;
  
        int n = data.length();
        int logn = checkFactor(n);
        if (n == 1)
            return;
  
        // Bit reverse the ordering of input data for decimation in time
        // algorithm.
        bitreverse(data, i0, stride);
        //bitReverse(data, logn);
  
        // apply fft recursion
        p = 1; q = n ;
        for (int i = 1; i <= 1; i++) {
            int a, b;
  
            p_1 = p ;
            p = 2 * p ;
            q = q / 2 ;
  
            for (b = 0; b < q; b++) {
                data.set(i0+stride*b*p,
                         data.get(i0+stride*b*p) +
                         data.get(i0+stride*(b*p + p_1)));
                data.set(i0+stride*(b*p + p_1),
                         data.get(i0+stride*b*p) -
                         data.get(i0+stride*(b*p + p_1)));
            }
  
            double w_real = 1.0;
            double w_imag = 0.0;
  
            double theta = - 2.0 * Math.PI / p;
          
            double s = Math.sin(theta);
            double t = Math.sin(theta / 2.0);
            double s2 = 2.0 * t * t;
          
            // trignometric recurrence for w-> exp(i theta) w
            for (a = 1; a < (p_1)/2; a++) {
                double tmp_real = w_real - s * w_imag - s2 * w_real;
                double tmp_imag = w_imag + s * w_real - s2 * w_imag;
                w_real = tmp_real;
                w_imag = tmp_imag;
              
                for (b = 0; b < q; b++) {
                    double z0_real = data.get(i0+stride*(b*p + a));
                    double z0_imag = data.get(i0+stride*(b*p + p_1 - a));
                    double z1_real = data.get(i0+stride*(b*p + p_1 + a));
                    double z1_imag = data.get(i0+stride*(b*p + p - a));
                  
                    data.set(i0+stride*(b*p + a),
                             z0_real + w_real * z1_real - w_imag * z1_imag);
                    data.set(i0+stride*(b*p + p - a),
                             z0_imag + w_real * z1_imag + w_imag * z1_real);
                    data.set(i0+stride*(b*p + p_1 - a),
                             z0_real - w_real * z1_real + w_imag * z1_imag);
                    data.set(i0+stride*(b*p + p_1 + a),
                             -(z0_imag - w_real * z1_imag - w_imag * z1_real));
                }
            }
  
            if (p_1 >  1) {
                for (b = 0; b < q; b++) {
                    int index = i0+stride*(b*p + p - p_1/2);
                    data.set(index, data.get(index) * -1);
                }
            }
        }
    }

    /**
     * Compute the (unnomalized) inverse FFT of data, leaving it in place.  The
     * data must be in the same arrangement as that produced by {@link
     * #transform transform}.
     */
    public static void backtransform(DoubleVector data) {
        int i0 = 0;
        int stride = 1;
        checkData(data,i0,stride);
        int n = data.length();
        int logn = checkFactor(n);
        int p, p_1, q;

        if (n == 1)
            return;

        // apply fft recursion
        p = n; q = 1 ; p_1 = n/2 ;

        for (int i = 1; i <= logn; i++) {
            int a, b;

            for (b = 0; b < q; b++) {
                double z0 = data.get(i0+stride*b*p);
                double z1 = data.get(i0+stride*(b*p + p_1));
                data.set(i0+stride*b*p, z0 + z1);
                data.set(i0+stride*(b*p + p_1), z0 - z1);
            }

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
                    double z0_real =  data.get(i0+stride*(b*p + a));
                    double z0_imag =  data.get(i0+stride*(b*p + p - a));
                    double z1_real =  data.get(i0+stride*(b*p + p_1 - a));
                    double z1_imag = -data.get(i0+stride*(b*p + p_1 + a));
            
                    /* t0 = z0 + z1 */		
                    data.set(i0+stride*(b*p + a), z0_real + z1_real);
                    data.set(i0+stride*(b*p + p_1 - a), z0_imag + z1_imag);

                    /* t1 = (z0 - z1) */
                    double t1_real = z0_real -  z1_real;
                    double t1_imag = z0_imag -  z1_imag;
                    data.set(i0+stride*(b*p + p_1 + a),
                             (w_real * t1_real - w_imag * t1_imag));
                    data.set(i0+stride*(b*p + p - a),
                             (w_real * t1_imag + w_imag * t1_real));
                }
            }

            if (p_1 >  1) {
                for (b = 0; b < q; b++) {
                    int index = i0+stride*(b*p + p_1/2);
                    data.set(index, data.get(index) * 2);
                    index = i0+stride*(b*p + p_1 + p_1/2);
                    data.set(index, data.get(index) * -2);
                }
            }

            p_1 = p_1 / 2 ;
            p = p / 2 ;
            q = q * 2 ;
        }

        // bit reverse the ordering of output data for decimation in frequency
        // algorithm 
        //bitReverse(data, logn);
        bitreverse(data, i0, stride);
    }

    /**
     * This is the Gold rader bit-reversal algorithm
     */
    public static void bitreverse(DoubleVector data, int i0, int stride) {
        int n = data.length();
        for (int i = 0,j = 0; i < n - 1; i++) {
            int k = n / 2;
            if (i < j) {
                double tmp = data.get(i0+stride*i);
                data.set(i0+stride*i, data.get(i0+stride*j));
                data.set(i0+stride*j, tmp);
            }

            while (k <= j) {
                j = j - k;
                k = k / 2;
            }
            j += k;
        }
    }

    /**
     * Reverses the bits, a step required for doing the FFT in place.  This
     * implementation is significantly faster than {@link bitreverse}. This
     * implementation is based on the following paper:
     *   
     *   </li style="font-family:Garamond, Georgia, serif">M. Rubio, P. Gomez,
     *   and K. Drouice.  "A new superfast bit reversal algorithm"
     *   <i>International Journal of Adaptive Control and Signal Processing</i>
     *
     * @param vector The vector to be permuted according to the bit reversal.
     *               This vector's length must be a power of two
     * @param power The log of the vector's length
     */
    private static void bitReverse(DoubleVector vector, int power) {
        vector.set(0, 0);
        vector.set(1, 2 << (power - 1));
        vector.set(2, 2 << (power - 2));
        vector.set(3, 3 * 2 << (power - 2));
        int prevN = 3;
        for (int k = 3; k < power - 2; ++k) {
            int currN = (2 << k) - 1;
            vector.set(currN, vector.get(prevN) + (2 << (power - k)));
            for (int l = 0; l < prevN - 1; ++l)
                vector.set(currN - l, vector.get(currN) - vector.get(l));
            prevN = currN;
        }
    }

    private static int log2(int n) {
        int log = 0;
        for (int k = 1; k < n; k *=2, log++)
            ;
        return (n != (1<<log)) ? -1 : log;
    }

    private static void checkData(DoubleVector data, int i0, int stride){
        int n = data.length();
        if (i0+stride*(n-1)+1 > data.length())
          throw new IllegalArgumentException(
                  "The data array is too small for "+n+":"+
                  "i0="+i0+" stride="+stride+" data.length="+n);
    }
}
