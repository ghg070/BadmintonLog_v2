package nctu.nol.algo;

/**
 * Created by Smile on 2016/3/15.
 */

//https://www.ee.columbia.edu/~ronw/code/MEAPsoft/doc/html/FFT_8java-source.html
public class CountSpectrum {
    int n, m;
    // Lookup tables. Only need to recompute when size of FFT changes.
    double[] cos;
    double[] sin;

    public CountSpectrum(int n) {
        this.n = n;
        this.m = (int) (Math.log(n) / Math.log(2));

        // Make sure n is a power of 2
        if (n != (1 << m))
            throw new RuntimeException("FFT length must be power of 2");

        // precompute tables
        cos = new double[n / 2];
        sin = new double[n / 2];

        for (int i = 0; i < n / 2; i++) {
            cos[i] = Math.cos(-2 * Math.PI * i / n);
            sin[i] = Math.sin(-2 * Math.PI * i / n);
        }

    }

    public double dft_specific_idx(int idx, final double[] signal){
        /*
        *   idx: 第幾個頻帶(要觀察哪一個頻帶的能量)
        *   signal: 原始資料, final型別代表不可以更動signal內的值, 要改必須copy一份
        *   可參考的java code, 此code是計算每個頻帶的能量值(也就是完整的dft), 請改成只針對某一頻帶的能量進行計算
        *   code: https://www.nayuki.io/res/how-to-implement-the-discrete-fourier-transform/Dft.java
        * */


        return 0;
    }

    public void fft(double[] x, double[] y){
        int i, j, k, n1, n2, a;
        double c, s, t1, t2;

        // Bit-reverse
        j = 0;
        n2 = n / 2;
        for (i = 1; i < n - 1; i++) {
            n1 = n2;
            while (j >= n1) {
                j = j - n1;
                n1 = n1 / 2;
            }
            j = j + n1;
            if (i < j) {
                t1 = x[i];
                x[i] = x[j];
                x[j] = t1;
                t1 = y[i];
                y[i] = y[j];
                y[j] = t1;
            }
        }

        // FFT
        n1 = 0;
        n2 = 1;

        for (i = 0; i < m; i++) {
            n1 = n2;
            n2 = n2 + n2;
            a = 0;

            for (j = 0; j < n1; j++) {
                c = cos[a];
                s = sin[a];
                a += 1 << (m - i - 1);
                for (k = j; k < n; k = k + n2) {
                    t1 = c * x[k + n1] - s * y[k + n1];
                    t2 = s * x[k + n1] + c * y[k + n1];
                    x[k + n1] = x[k] - t1;
                    y[k + n1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }
    }
};