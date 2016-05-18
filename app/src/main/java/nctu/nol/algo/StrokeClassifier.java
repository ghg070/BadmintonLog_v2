package nctu.nol.algo;


import android.app.Activity;
import android.content.Intent;
import android.text.format.Time;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import nctu.nol.file.LogFileWriter;
import nctu.nol.file.SystemParameters;
import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Attribute;

public class StrokeClassifier {
    private final static String TAG = StrokeClassifier.class.getSimpleName();

    public static final long FeatureExtraction_Alpha = 600;
    public static final long FeatureExtraction_Beta = 400;

    // Broadcast Related
    public final static String ACTION_OUTPUT_RESULT_STATE = "STROKECLASSIFIER.ACTION_OUTPUT_RESULT_STATE";
    public final static String EXTRA_TYPE = "STROKECLASSIFIER.EXTRA_TYPE";

    // LogFile Related
    public LogFileWriter StrokeWriter;

    private Activity mActivity;

    public Instances dataset = null;
    public ArrayList<Attribute> attributeList = new ArrayList<Attribute>();

    public StrokeClassifier(Activity act) {
        this.mActivity = act;
        BuildDataset();
    }

    public void initLogFile(){
        StrokeWriter = new LogFileWriter("StrokeType.csv", LogFileWriter.STROKE_TYPE, LogFileWriter.TESTING_TYPE);
    }
    public void closeLogFile(){
        if(StrokeWriter != null)
            StrokeWriter.closefile();
    }

    public double Classify(final long stroke_time, final ArrayList<Float> allVals) {

        double result = -1;
        try {
            // Single Data
            DenseInstance inst = new DenseInstance(dataset.numAttributes());
            inst.setDataset(dataset);

            // Set instance's values for the attributes
            for(int i = 0; i < allVals.size(); i++)
                inst.setValue(attributeList.get(i), allVals.get(i));

            // load classifier from file
            InputStream in_stream = mActivity.getResources().openRawResource( mActivity.getResources().getIdentifier("smo", "raw", mActivity.getPackageName()));
            //InputStream in_stream = mActivity.getResources().openRawResource( mActivity.getResources().getIdentifier("random_forest", "raw", mActivity.getPackageName()));
            Classifier clf = (Classifier) weka.core.SerializationHelper.read(in_stream);
            result = clf.classifyInstance(inst);
            String type = dataset.classAttribute().value((int)result);

            //print result
            Log.d(TAG, dataset.classAttribute().value((int)result));
            Intent broadcast = new Intent(ACTION_OUTPUT_RESULT_STATE);
            broadcast.putExtra(EXTRA_TYPE, type);
            mActivity.sendBroadcast(broadcast);

            // Log File
            try {
                long offset = SystemParameters.SoundStartTime-SystemParameters.StartTime;
                StrokeWriter.writeStroke( MillisecToString(stroke_time-offset), type);
            } catch (IOException e) {
                Log.e(TAG,e.getMessage());
            }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public final ArrayList<Float> FeatureExtraction(final ArrayList<float[]> L_AccData,
                                                    final ArrayList<float[]> R_AccData,
                                                    final ArrayList<float[]> L_AccData_Without_Gravity,
                                                    final ArrayList<float[]> R_AccData_Without_Gravity,
                                                    final ArrayList<float[]> L_GyroData,
                                                    final ArrayList<float[]> R_GyroData) {
        // Preprocessing
        double[] l_gx_dataset = new double[L_AccData.size()],
                l_gy_dataset = new double[L_AccData.size()],
                l_gz_dataset = new double[L_AccData.size()],
                r_gx_dataset = new double[R_AccData.size()],
                r_gy_dataset = new double[R_AccData.size()],
                r_gz_dataset = new double[R_AccData.size()],
                l_ax_dataset = new double[L_AccData_Without_Gravity.size()],
                l_ay_dataset = new double[L_AccData_Without_Gravity.size()],
                l_az_dataset = new double[L_AccData_Without_Gravity.size()],
                r_ax_dataset = new double[R_AccData_Without_Gravity.size()],
                r_ay_dataset = new double[R_AccData_Without_Gravity.size()],
                r_az_dataset = new double[R_AccData_Without_Gravity.size()],
                l_force_dataset = new double[L_AccData_Without_Gravity.size()],
                r_force_dataset = new double[R_AccData_Without_Gravity.size()],
                l_wx_dataset = new double[L_GyroData.size()],
                l_wy_dataset = new double[L_GyroData.size()],
                l_wz_dataset = new double[L_GyroData.size()],
                r_wx_dataset = new double[R_GyroData.size()],
                r_wy_dataset = new double[R_GyroData.size()],
                r_wz_dataset = new double[R_GyroData.size()];
        for (int i = 0; i < L_AccData.size(); i++) {
            l_gx_dataset[i] = L_AccData.get(i)[0];
            l_gy_dataset[i] = L_AccData.get(i)[1];
            l_gz_dataset[i] = L_AccData.get(i)[2];
        }
        for (int i = 0; i < R_AccData.size(); i++) {
            r_gx_dataset[i] = R_AccData.get(i)[0];
            r_gy_dataset[i] = R_AccData.get(i)[1];
            r_gz_dataset[i] = R_AccData.get(i)[2];
        }
        for (int i = 0; i < L_AccData_Without_Gravity.size(); i++) {
            l_ax_dataset[i] = L_AccData_Without_Gravity.get(i)[0];
            l_ay_dataset[i] = L_AccData_Without_Gravity.get(i)[1];
            l_az_dataset[i] = L_AccData_Without_Gravity.get(i)[2];
            l_force_dataset[i] = Math.sqrt(Math.pow(l_ax_dataset[i], 2) + Math.pow(l_ay_dataset[i], 2) + Math.pow(l_az_dataset[i], 2));
        }
        for (int i = 0; i < R_AccData_Without_Gravity.size(); i++) {
            r_ax_dataset[i] = R_AccData_Without_Gravity.get(i)[0];
            r_ay_dataset[i] = R_AccData_Without_Gravity.get(i)[1];
            r_az_dataset[i] = R_AccData_Without_Gravity.get(i)[2];
            r_force_dataset[i] = Math.sqrt(Math.pow(r_ax_dataset[i], 2) + Math.pow(r_ay_dataset[i], 2) + Math.pow(r_az_dataset[i], 2));
        }
        for (int i = 0; i < L_GyroData.size(); i++) {
            l_wx_dataset[i] = L_GyroData.get(i)[0];
            l_wy_dataset[i] = L_GyroData.get(i)[1];
            l_wz_dataset[i] = L_GyroData.get(i)[2];
        }
        for (int i = 0; i < R_GyroData.size(); i++) {
            r_wx_dataset[i] = R_GyroData.get(i)[0];
            r_wy_dataset[i] = R_GyroData.get(i)[1];
            r_wz_dataset[i] = R_GyroData.get(i)[2];
        }

        ArrayList<Float> allValues = new ArrayList<Float>();
        
        double max_val = max(l_gx_dataset), rms_val = rms(l_gx_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(l_gx_dataset));
        allValues.add((float)mean(l_gx_dataset));
        allValues.add((float)std(l_gx_dataset));
        allValues.add((float)rms(l_gx_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(l_gx_dataset));
        allValues.add((float)skewness(l_gx_dataset));
        allValues.add((float)kurtosis(l_gx_dataset));

        max_val = max(r_gx_dataset);
        rms_val = rms(r_gx_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(r_gx_dataset));
        allValues.add((float)mean(r_gx_dataset));
        allValues.add((float)std(r_gx_dataset));
        allValues.add((float)rms(r_gx_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(r_gx_dataset));
        allValues.add((float)skewness(r_gx_dataset));
        allValues.add((float)kurtosis(r_gx_dataset));

        max_val = max(l_gy_dataset);
        rms_val = rms(l_gy_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(l_gy_dataset));
        allValues.add((float)mean(l_gy_dataset));
        allValues.add((float)std(l_gy_dataset));
        allValues.add((float)rms(l_gy_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(l_gy_dataset));
        allValues.add((float)skewness(l_gy_dataset));
        allValues.add((float)kurtosis(l_gy_dataset));

        max_val = max(r_gy_dataset);
        rms_val = rms(r_gy_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(r_gy_dataset));
        allValues.add((float)mean(r_gy_dataset));
        allValues.add((float)std(r_gy_dataset));
        allValues.add((float)rms(r_gy_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(r_gy_dataset));
        allValues.add((float)skewness(r_gy_dataset));
        allValues.add((float)kurtosis(r_gy_dataset));

        max_val = max(l_gz_dataset);
        rms_val = rms(l_gz_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(l_gz_dataset));
        allValues.add((float)mean(l_gz_dataset));
        allValues.add((float)std(l_gz_dataset));
        allValues.add((float)rms(l_gz_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(l_gz_dataset));
        allValues.add((float)skewness(l_gz_dataset));
        allValues.add((float)kurtosis(l_gz_dataset));

        max_val = max(r_gz_dataset);
        rms_val = rms(r_gz_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(r_gz_dataset));
        allValues.add((float)mean(r_gz_dataset));
        allValues.add((float)std(r_gz_dataset));
        allValues.add((float)rms(r_gz_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(r_gz_dataset));
        allValues.add((float)skewness(r_gz_dataset));
        allValues.add((float)kurtosis(r_gz_dataset));

        allValues.add((float)corrcoef(l_gx_dataset, l_gy_dataset));
        allValues.add((float)corrcoef(l_gx_dataset, l_gz_dataset));
        allValues.add((float)corrcoef(l_gy_dataset, l_gz_dataset));

        allValues.add((float)corrcoef(r_gx_dataset, r_gy_dataset));
        allValues.add((float)corrcoef(r_gx_dataset, r_gz_dataset));
        allValues.add((float)corrcoef(r_gy_dataset, r_gz_dataset));

        max_val = max(l_ax_dataset);
        rms_val = rms(l_ax_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(l_ax_dataset));
        allValues.add((float)mean(l_ax_dataset));
        allValues.add((float)std(l_ax_dataset));
        allValues.add((float)rms(l_ax_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(l_ax_dataset));
        allValues.add((float)skewness(l_ax_dataset));
        allValues.add((float)kurtosis(l_ax_dataset));

        max_val = max(r_ax_dataset);
        rms_val = rms(r_ax_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(r_ax_dataset));
        allValues.add((float)mean(r_ax_dataset));
        allValues.add((float)std(r_ax_dataset));
        allValues.add((float)rms(r_ax_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(r_ax_dataset));
        allValues.add((float)skewness(r_ax_dataset));
        allValues.add((float)kurtosis(r_ax_dataset));

        max_val = max(l_ay_dataset);
        rms_val = rms(l_ay_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(l_ay_dataset));
        allValues.add((float)mean(l_ay_dataset));
        allValues.add((float)std(l_ay_dataset));
        allValues.add((float)rms(l_ay_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(l_ay_dataset));
        allValues.add((float)skewness(l_ay_dataset));
        allValues.add((float)kurtosis(l_ay_dataset));

        max_val = max(r_ay_dataset);
        rms_val = rms(r_ay_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(r_ay_dataset));
        allValues.add((float)mean(r_ay_dataset));
        allValues.add((float)std(r_ay_dataset));
        allValues.add((float)rms(r_ay_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(r_ay_dataset));
        allValues.add((float)skewness(r_ay_dataset));
        allValues.add((float)kurtosis(r_ay_dataset));

        max_val = max(l_az_dataset);
        rms_val = rms(l_az_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(l_az_dataset));
        allValues.add((float)mean(l_az_dataset));
        allValues.add((float)std(l_az_dataset));
        allValues.add((float)rms(l_az_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(l_az_dataset));
        allValues.add((float)skewness(l_az_dataset));
        allValues.add((float)kurtosis(l_az_dataset));

        max_val = max(r_az_dataset);
        rms_val = rms(r_az_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(r_az_dataset));
        allValues.add((float)mean(r_az_dataset));
        allValues.add((float)std(r_az_dataset));
        allValues.add((float)rms(r_az_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(r_az_dataset));
        allValues.add((float)skewness(r_az_dataset));
        allValues.add((float)kurtosis(r_az_dataset));

        allValues.add((float)corrcoef(l_ax_dataset, l_ay_dataset));
        allValues.add((float)corrcoef(l_ax_dataset, l_az_dataset));
        allValues.add((float)corrcoef(l_ay_dataset, l_az_dataset));

        allValues.add((float)corrcoef(r_ax_dataset, r_ay_dataset));
        allValues.add((float)corrcoef(r_ax_dataset, r_az_dataset));
        allValues.add((float)corrcoef(r_ay_dataset, r_az_dataset));

        max_val = max(l_force_dataset);
        rms_val = rms(l_force_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(l_force_dataset));
        allValues.add((float)mean(l_force_dataset));
        allValues.add((float)std(l_force_dataset));
        allValues.add((float)rms(l_force_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(l_force_dataset));
        allValues.add((float)skewness(l_force_dataset));
        allValues.add((float)kurtosis(l_force_dataset));

        max_val = max(r_force_dataset);
        rms_val = rms(r_force_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(r_force_dataset));
        allValues.add((float)mean(r_force_dataset));
        allValues.add((float)std(r_force_dataset));
        allValues.add((float)rms(r_force_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(r_force_dataset));
        allValues.add((float)skewness(r_force_dataset));
        allValues.add((float)kurtosis(r_force_dataset));

        max_val = max(l_wx_dataset);
        rms_val = rms(l_wx_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(l_wx_dataset));
        allValues.add((float)mean(l_wx_dataset));
        allValues.add((float)std(l_wx_dataset));
        allValues.add((float)rms(l_wx_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(l_wx_dataset));
        allValues.add((float)skewness(l_wx_dataset));
        allValues.add((float)kurtosis(l_wx_dataset));

        max_val = max(r_wx_dataset);
        rms_val = rms(r_wx_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(r_wx_dataset));
        allValues.add((float)mean(r_wx_dataset));
        allValues.add((float)std(r_wx_dataset));
        allValues.add((float)rms(r_wx_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(r_wx_dataset));
        allValues.add((float)skewness(r_wx_dataset));
        allValues.add((float)kurtosis(r_wx_dataset));

        max_val = max(l_wy_dataset);
        rms_val = rms(l_wy_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(l_wy_dataset));
        allValues.add((float)mean(l_wy_dataset));
        allValues.add((float)std(l_wy_dataset));
        allValues.add((float)rms(l_wy_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(l_wy_dataset));
        allValues.add((float)skewness(l_wy_dataset));
        allValues.add((float)kurtosis(l_wy_dataset));

        max_val = max(r_wy_dataset);
        rms_val = rms(r_wy_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(r_wy_dataset));
        allValues.add((float)mean(r_wy_dataset));
        allValues.add((float)std(r_wy_dataset));
        allValues.add((float)rms(r_wy_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(r_wy_dataset));
        allValues.add((float)skewness(r_wy_dataset));
        allValues.add((float)kurtosis(r_wy_dataset));

        max_val = max(l_wz_dataset);
        rms_val = rms(l_wz_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(l_wz_dataset));
        allValues.add((float)mean(l_wz_dataset));
        allValues.add((float)std(l_wz_dataset));
        allValues.add((float)rms(l_wz_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(l_wz_dataset));
        allValues.add((float)skewness(l_wz_dataset));
        allValues.add((float)kurtosis(l_wz_dataset));

        max_val = max(r_wz_dataset);
        rms_val = rms(r_wz_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(r_wz_dataset));
        allValues.add((float)mean(r_wz_dataset));
        allValues.add((float)std(r_wz_dataset));
        allValues.add((float)rms(r_wz_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(r_wz_dataset));
        allValues.add((float)skewness(r_wz_dataset));
        allValues.add((float)kurtosis(r_wz_dataset));

        allValues.add((float)corrcoef(l_wx_dataset, l_wy_dataset));
        allValues.add((float)corrcoef(l_wx_dataset, l_wz_dataset));
        allValues.add((float)corrcoef(l_wy_dataset, l_wz_dataset));

        allValues.add((float)corrcoef(r_wx_dataset, r_wy_dataset));
        allValues.add((float)corrcoef(r_wx_dataset, r_wz_dataset));
        allValues.add((float)corrcoef(r_wy_dataset, r_wz_dataset));

        return allValues;
    }

    private void BuildDataset() {
        // Create attributes to be used with classifiers
        attributeList.add( new Attribute("l_gx_Max") );
        attributeList.add( new Attribute("l_gx_Min") );
        attributeList.add( new Attribute("l_gx_Avg") );
        attributeList.add( new Attribute("l_gx_Std") );
        attributeList.add( new Attribute("l_gx_RMS") );
        attributeList.add( new Attribute("l_gx_CF") );
        attributeList.add( new Attribute("l_gx_Iqr") );
        attributeList.add( new Attribute("l_gx_Skewness") );
        attributeList.add( new Attribute("l_gx_Kurtosis") );
        attributeList.add( new Attribute("r_gx_Max") );
        attributeList.add( new Attribute("r_gx_Min") );
        attributeList.add( new Attribute("r_gx_Avg") );
        attributeList.add( new Attribute("r_gx_Std") );
        attributeList.add( new Attribute("r_gx_RMS") );
        attributeList.add( new Attribute("r_gx_CF") );
        attributeList.add( new Attribute("r_gx_Iqr") );
        attributeList.add( new Attribute("r_gx_Skewness") );
        attributeList.add( new Attribute("r_gx_Kurtosis") );
        attributeList.add( new Attribute("l_gy_Max") );
        attributeList.add( new Attribute("l_gy_Min") );
        attributeList.add( new Attribute("l_gy_Avg") );
        attributeList.add( new Attribute("l_gy_Std") );
        attributeList.add( new Attribute("l_gy_RMS") );
        attributeList.add( new Attribute("l_gy_CF") );
        attributeList.add( new Attribute("l_gy_Iqr") );
        attributeList.add( new Attribute("l_gy_Skewness") );
        attributeList.add( new Attribute("l_gy_Kurtosis") );
        attributeList.add( new Attribute("r_gy_Max") );
        attributeList.add( new Attribute("r_gy_Min") );
        attributeList.add( new Attribute("r_gy_Avg") );
        attributeList.add( new Attribute("r_gy_Std") );
        attributeList.add( new Attribute("r_gy_RMS") );
        attributeList.add( new Attribute("r_gy_CF") );
        attributeList.add( new Attribute("r_gy_Iqr") );
        attributeList.add( new Attribute("r_gy_Skewness") );
        attributeList.add( new Attribute("r_gy_Kurtosis") );
        attributeList.add( new Attribute("l_gz_Max") );
        attributeList.add( new Attribute("l_gz_Min") );
        attributeList.add( new Attribute("l_gz_Avg") );
        attributeList.add( new Attribute("l_gz_Std") );
        attributeList.add( new Attribute("l_gz_RMS") );
        attributeList.add( new Attribute("l_gz_CF") );
        attributeList.add( new Attribute("l_gz_Iqr") );
        attributeList.add( new Attribute("l_gz_Skewness") );
        attributeList.add( new Attribute("l_gz_Kurtosis") );
        attributeList.add( new Attribute("r_gz_Max") );
        attributeList.add( new Attribute("r_gz_Min") );
        attributeList.add( new Attribute("r_gz_Avg") );
        attributeList.add( new Attribute("r_gz_Std") );
        attributeList.add( new Attribute("r_gz_RMS") );
        attributeList.add( new Attribute("r_gz_CF") );
        attributeList.add( new Attribute("r_gz_Iqr") );
        attributeList.add( new Attribute("r_gz_Skewness") );
        attributeList.add( new Attribute("r_gz_Kurtosis") );
        attributeList.add( new Attribute("l_gxgy_Corr") );
        attributeList.add( new Attribute("l_gxgz_Corr") );
        attributeList.add( new Attribute("l_gygz_Corr") );
        attributeList.add( new Attribute("r_gxgy_Corr") );
        attributeList.add( new Attribute("r_gxgz_Corr") );
        attributeList.add( new Attribute("r_gygz_Corr") );
        attributeList.add( new Attribute("l_ax_Max") );
        attributeList.add( new Attribute("l_ax_Min") );
        attributeList.add( new Attribute("l_ax_Avg") );
        attributeList.add( new Attribute("l_ax_Std") );
        attributeList.add( new Attribute("l_ax_RMS") );
        attributeList.add( new Attribute("l_ax_CF") );
        attributeList.add( new Attribute("l_ax_Iqr") );
        attributeList.add( new Attribute("l_ax_Skewness") );
        attributeList.add( new Attribute("l_ax_Kurtosis") );
        attributeList.add( new Attribute("r_ax_Max") );
        attributeList.add( new Attribute("r_ax_Min") );
        attributeList.add( new Attribute("r_ax_Avg") );
        attributeList.add( new Attribute("r_ax_Std") );
        attributeList.add( new Attribute("r_ax_RMS") );
        attributeList.add( new Attribute("r_ax_CF") );
        attributeList.add( new Attribute("r_ax_Iqr") );
        attributeList.add( new Attribute("r_ax_Skewness") );
        attributeList.add( new Attribute("r_ax_Kurtosis") );
        attributeList.add( new Attribute("l_ay_Max") );
        attributeList.add( new Attribute("l_ay_Min") );
        attributeList.add( new Attribute("l_ay_Avg") );
        attributeList.add( new Attribute("l_ay_Std") );
        attributeList.add( new Attribute("l_ay_RMS") );
        attributeList.add( new Attribute("l_ay_CF") );
        attributeList.add( new Attribute("l_ay_Iqr") );
        attributeList.add( new Attribute("l_ay_Skewness") );
        attributeList.add( new Attribute("l_ay_Kurtosis") );
        attributeList.add( new Attribute("r_ay_Max") );
        attributeList.add( new Attribute("r_ay_Min") );
        attributeList.add( new Attribute("r_ay_Avg") );
        attributeList.add( new Attribute("r_ay_Std") );
        attributeList.add( new Attribute("r_ay_RMS") );
        attributeList.add( new Attribute("r_ay_CF") );
        attributeList.add( new Attribute("r_ay_Iqr") );
        attributeList.add( new Attribute("r_ay_Skewness") );
        attributeList.add( new Attribute("r_ay_Kurtosis") );
        attributeList.add( new Attribute("l_az_Max") );
        attributeList.add( new Attribute("l_az_Min") );
        attributeList.add( new Attribute("l_az_Avg") );
        attributeList.add( new Attribute("l_az_Std") );
        attributeList.add( new Attribute("l_az_RMS") );
        attributeList.add( new Attribute("l_az_CF") );
        attributeList.add( new Attribute("l_az_Iqr") );
        attributeList.add( new Attribute("l_az_Skewness") );
        attributeList.add( new Attribute("l_az_Kurtosis") );
        attributeList.add( new Attribute("r_az_Max") );
        attributeList.add( new Attribute("r_az_Min") );
        attributeList.add( new Attribute("r_az_Avg") );
        attributeList.add( new Attribute("r_az_Std") );
        attributeList.add( new Attribute("r_az_RMS") );
        attributeList.add( new Attribute("r_az_CF") );
        attributeList.add( new Attribute("r_az_Iqr") );
        attributeList.add( new Attribute("r_az_Skewness") );
        attributeList.add( new Attribute("r_az_Kurtosis") );
        attributeList.add( new Attribute("l_axay_Corr") );
        attributeList.add( new Attribute("l_axaz_Corr") );
        attributeList.add( new Attribute("l_ayaz_Corr") );
        attributeList.add( new Attribute("r_axay_Corr") );
        attributeList.add( new Attribute("r_axaz_Corr") );
        attributeList.add( new Attribute("r_ayaz_Corr") );
        attributeList.add( new Attribute("l_force_Max") );
        attributeList.add( new Attribute("l_force_Min") );
        attributeList.add( new Attribute("l_force_Avg") );
        attributeList.add( new Attribute("l_force_Std") );
        attributeList.add( new Attribute("l_force_RMS") );
        attributeList.add( new Attribute("l_force_CF") );
        attributeList.add( new Attribute("l_force_Iqr") );
        attributeList.add( new Attribute("l_force_Skewness") );
        attributeList.add( new Attribute("l_force_Kurtosis") );
        attributeList.add( new Attribute("r_force_Max") );
        attributeList.add( new Attribute("r_force_Min") );
        attributeList.add( new Attribute("r_force_Avg") );
        attributeList.add( new Attribute("r_force_Std") );
        attributeList.add( new Attribute("r_force_RMS") );
        attributeList.add( new Attribute("r_force_CF") );
        attributeList.add( new Attribute("r_force_Iqr") );
        attributeList.add( new Attribute("r_force_Skewness") );
        attributeList.add( new Attribute("r_force_Kurtosis") );
        attributeList.add( new Attribute("l_wx_Max") );
        attributeList.add( new Attribute("l_wx_Min") );
        attributeList.add( new Attribute("l_wx_Avg") );
        attributeList.add( new Attribute("l_wx_Std") );
        attributeList.add( new Attribute("l_wx_RMS") );
        attributeList.add( new Attribute("l_wx_CF") );
        attributeList.add( new Attribute("l_wx_Iqr") );
        attributeList.add( new Attribute("l_wx_Skewness") );
        attributeList.add( new Attribute("l_wx_Kurtosis") );
        attributeList.add( new Attribute("r_wx_Max") );
        attributeList.add( new Attribute("r_wx_Min") );
        attributeList.add( new Attribute("r_wx_Avg") );
        attributeList.add( new Attribute("r_wx_Std") );
        attributeList.add( new Attribute("r_wx_RMS") );
        attributeList.add( new Attribute("r_wx_CF") );
        attributeList.add( new Attribute("r_wx_Iqr") );
        attributeList.add( new Attribute("r_wx_Skewness") );
        attributeList.add( new Attribute("r_wx_Kurtosis") );
        attributeList.add( new Attribute("l_wy_Max") );
        attributeList.add( new Attribute("l_wy_Min") );
        attributeList.add( new Attribute("l_wy_Avg") );
        attributeList.add( new Attribute("l_wy_Std") );
        attributeList.add( new Attribute("l_wy_RMS") );
        attributeList.add( new Attribute("l_wy_CF") );
        attributeList.add( new Attribute("l_wy_Iqr") );
        attributeList.add( new Attribute("l_wy_Skewness") );
        attributeList.add( new Attribute("l_wy_Kurtosis") );
        attributeList.add( new Attribute("r_wy_Max") );
        attributeList.add( new Attribute("r_wy_Min") );
        attributeList.add( new Attribute("r_wy_Avg") );
        attributeList.add( new Attribute("r_wy_Std") );
        attributeList.add( new Attribute("r_wy_RMS") );
        attributeList.add( new Attribute("r_wy_CF") );
        attributeList.add( new Attribute("r_wy_Iqr") );
        attributeList.add( new Attribute("r_wy_Skewness") );
        attributeList.add( new Attribute("r_wy_Kurtosis") );
        attributeList.add( new Attribute("l_wz_Max") );
        attributeList.add( new Attribute("l_wz_Min") );
        attributeList.add( new Attribute("l_wz_Avg") );
        attributeList.add( new Attribute("l_wz_Std") );
        attributeList.add( new Attribute("l_wz_RMS") );
        attributeList.add( new Attribute("l_wz_CF") );
        attributeList.add( new Attribute("l_wz_Iqr") );
        attributeList.add( new Attribute("l_wz_Skewness") );
        attributeList.add( new Attribute("l_wz_Kurtosis") );
        attributeList.add( new Attribute("r_wz_Max") );
        attributeList.add( new Attribute("r_wz_Min") );
        attributeList.add( new Attribute("r_wz_Avg") );
        attributeList.add( new Attribute("r_wz_Std") );
        attributeList.add( new Attribute("r_wz_RMS") );
        attributeList.add( new Attribute("r_wz_CF") );
        attributeList.add( new Attribute("r_wz_Iqr") );
        attributeList.add( new Attribute("r_wz_Skewness") );
        attributeList.add( new Attribute("r_wz_Kurtosis") );
        attributeList.add( new Attribute("l_wxwy_Corr") );
        attributeList.add( new Attribute("l_wxwz_Corr") );
        attributeList.add( new Attribute("l_wywz_Corr") );
        attributeList.add( new Attribute("r_wxwy_Corr") );
        attributeList.add( new Attribute("r_wxwz_Corr") );
        attributeList.add( new Attribute("r_wywz_Corr") );
        
        // Result Type
        ArrayList<String> classVal = new ArrayList<String>();
        classVal.add("netplay");
        classVal.add("lob");
        classVal.add("drive");
        classVal.add("drop");
        classVal.add("long");
        classVal.add("smash");
        classVal.add("forehand serve");
        attributeList.add(new Attribute("Type", classVal));

        // Empty Test Dataset
        dataset = new Instances("TestInstances", attributeList, 0);
        dataset.setClassIndex(dataset.numAttributes() - 1);
    }


    /**********************
     * Statistic Function
     **********************/
    private double max(final double[] vals) {
        if(vals.length == 0)
            return 0;

        double result = Double.MIN_VALUE;
        for (int i = 0; i < vals.length; i++) {
            if (result < vals[i])
                result = vals[i];
        }
        return result;
    }

    private double min(final double[] vals) {
        if(vals.length == 0)
            return 0;

        double result = Double.MAX_VALUE;
        for (int i = 0; i < vals.length; i++) {
            if (result > vals[i])
                result = vals[i];
        }
        return result;
    }

    private double mean(final double[] vals) {
        if(vals.length == 0)
            return 0;

        double result = 0;
        for (int i = 0; i < vals.length; i++)
            result += vals[i];
        result /= vals.length;
        return result;
    }

    private double std(final double[] vals) {
        if(vals.length == 0)
            return 0;

        double result = 0;
        double mean_val = mean(vals);
        for (int i = 0; i < vals.length; i++)
            result += Math.pow(vals[i] - mean_val, 2);
        result = Math.sqrt(result / vals.length);
        return result;
    }

    private double rms(final double[] vals) {
        if(vals.length == 0)
            return 0;

        double result = 0;
        for (int i = 0; i < vals.length; i++)
            result += Math.pow(vals[i], 2);
        result = (float) Math.sqrt(result/vals.length);
        return result;
    }

    // Crest Factor
    private double CF(final double max, final double rms) {
        if (rms == 0)
            return 0;
        return max / rms;
    }

    private double skewness(final double[] vals){
        if(vals.length == 0)
            return 0;

        double result = 0;
        double mean_val = mean(vals);
        for(int i = 0; i < vals.length; i++)
            result += Math.pow(vals[i]-mean_val, 3) / Math.pow(std(vals),3);
        result /= vals.length;
        return result;
    }

    private double kurtosis(final double[] vals){
        if(vals.length == 0)
            return 0;

        double result = 0;
        double mean_val = mean(vals);
        for(int i = 0; i < vals.length; i++)
            result += Math.pow(vals[i]-mean_val, 4) / Math.pow(std(vals),4);
        result /= vals.length;
        return result;
    }

    private double corrcoef(final double[] l_val, final double[] r_val) {
        if (l_val.length != r_val.length || l_val.length  == 0 || r_val.length == 0)
            return 0;
        double l_mean = mean(l_val), r_mean = mean(r_val);

        double up = 0, down1 = 0, down2 = 0;
        //up
        for (int i = 0; i < l_val.length; i++)
            up += (l_val[i] - l_mean) * (r_val[i] - r_mean);

        //down1
        for (int i = 0; i < l_val.length; i++)
            down1 += Math.pow(l_val[i] - l_mean, 2);
        down1 = Math.sqrt(down1);

        //down2
        for (int i = 0; i < r_val.length; i++)
            down2 += Math.pow(r_val[i] - r_mean, 2);
        down2 = Math.sqrt(down2);

        return up / (down1 * down2);
    }

    private double iqr(final double[] vals){
        if (vals.length < 2)
            return 0;

        double [] copy_vals = new double[vals.length];
        for(int i = 0; i < vals.length; i++)
            copy_vals[i] = vals[i];
        quickSort(copy_vals,0,copy_vals.length-1);

        // ref algo: http://www.mathworks.com/help/stats/prctile.html
        double percent_25_pos = 25*copy_vals.length/100.0+0.5,
                percent_75_pos = 75*copy_vals.length/100.0+0.5;

        // count Q1
        double percent_25_val = Double.NaN;
        int prev_idx = (int)Math.floor(percent_25_pos);
        int next_idx = (int)Math.ceil(percent_25_pos);
        if(prev_idx == next_idx) // 25%的位置找得到值
            percent_25_val = copy_vals[prev_idx-1];
        else { // 找不到, 用內差
            double val1 = copy_vals[prev_idx-1], val2 = copy_vals[next_idx-1];
            percent_25_val = (val1*(next_idx-percent_25_pos)+val2*(percent_25_pos-prev_idx))/(next_idx-prev_idx);
        }

        // count Q3
        double percent_75_val = Double.NaN;
        prev_idx = (int)Math.floor(percent_75_pos);
        next_idx = (int)Math.ceil(percent_75_pos);
        if(prev_idx == next_idx) // 75%的位置找得到值
            percent_75_val = copy_vals[prev_idx-1];
        else { // 找不到, 用內差
            double val1 = copy_vals[prev_idx-1], val2 = copy_vals[next_idx-1];
            percent_75_val = (val1*(next_idx-percent_75_pos)+val2*(percent_75_pos-prev_idx))/(next_idx-prev_idx);
        }

        return percent_75_val-percent_25_val;
    }


    /*******************
     *  Help Function
     *******************/
    int partition(double arr[], int left, int right) {
        int i = left, j = right;
        double tmp;
        double pivot = arr[(left + right) / 2];

        while (i <= j) {
            while (arr[i] < pivot)
                i++;
            while (arr[j] > pivot)
                j--;
            if (i <= j) {
                tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;
                i++;
                j--;
            }
        };
        return i;
    }
    private void quickSort(double arr[], int left, int right) {
        int index = partition(arr, left, right);
        if (left < index - 1)
            quickSort(arr, left, index - 1);
        if (index < right)
            quickSort(arr, index, right);
    }

    public String MillisecToString(long timestamp){

        //compute the passed minutes
        Long minutes = (timestamp/1000)/60;
        //compute the passed seconds
        Long seconds = (timestamp/1000) % 60;
        //compute the passed hours
        Long millisecond = timestamp%1000 ;


        // MM:SS.mmm
        return String.format("%02d:%02d.%03d", minutes, seconds, millisecond);
    }

}
