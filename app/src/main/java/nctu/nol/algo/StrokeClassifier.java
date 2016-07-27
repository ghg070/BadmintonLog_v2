package nctu.nol.algo;


import android.app.Activity;
import android.content.Intent;
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

    public static final long FeatureExtraction_Alpha = 500;
    public static final long FeatureExtraction_Beta = 500;

    // Broadcast Related
    public final static String ACTION_OUTPUT_RESULT_STATE = "STROKECLASSIFIER.ACTION_OUTPUT_RESULT_STATE";
    public final static String EXTRA_TYPE = "STROKECLASSIFIER.EXTRA_TYPE";

    // LogFile Related
    public LogFileWriter StrokeWriter;
    public LogFileWriter StrokeFeature;

    private Activity mActivity;

    public Instances dataset = null;
    public ArrayList<Attribute> attributeList = new ArrayList<Attribute>();

    public StrokeClassifier(Activity act) {
        this.mActivity = act;
        BuildDataset();
    }

    public void initLogFile(){
        StrokeWriter = new LogFileWriter("StrokeType.csv", LogFileWriter.STROKE_TYPE, LogFileWriter.TESTING_TYPE);
        StrokeFeature = new LogFileWriter("StrokeFeature.csv", LogFileWriter.STROKE_TYPE, LogFileWriter.TESTING_TYPE);
    }
    public void closeLogFile(){
        if(StrokeWriter != null)
            StrokeWriter.closefile();
        if(StrokeFeature != null)
            StrokeFeature.closefile();
    }

    public double Classify(final long stroke_time, final ArrayList<Float> allVals) {

        double result = -1;
        try {
            /*
            // Single Data
            DenseInstance inst = new DenseInstance(dataset.numAttributes());
            inst.setDataset(dataset);

            // Set instance's values for the attributes
            for(int i = 0; i < allVals.size(); i++)
                inst.setValue(attributeList.get(i), allVals.get(i));
            StrokeFeature.writeFeatures(allVals);

            // load classifier from file
            InputStream in_stream = mActivity.getResources().openRawResource( mActivity.getResources().getIdentifier("smo", "raw", mActivity.getPackageName()));
            Classifier clf = (Classifier) weka.core.SerializationHelper.read(in_stream);
            result = clf.classifyInstance(inst);
            String type = dataset.classAttribute().value((int)result);

            //print result
            Log.d(TAG, dataset.classAttribute().value((int)result));
            Intent broadcast = new Intent(ACTION_OUTPUT_RESULT_STATE);
            broadcast.putExtra(EXTRA_TYPE, type);
            mActivity.sendBroadcast(broadcast);
*/
            // Log File
            try {
                long offset = SystemParameters.SoundStartTime-SystemParameters.StartTime;
                StrokeWriter.writeStroke( MillisecToString(stroke_time-offset), "None");
            } catch (IOException e) {
                Log.e(TAG,e.getMessage());
            }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public final ArrayList<Float> FeatureExtraction(final ArrayList<float[]> AccData,
                                                    final ArrayList<float[]> AccData_Without_Gravity,
                                                    final ArrayList<float[]> GyroData) {

        /*LogFileWriter g, a, w;

        g = new LogFileWriter(test+"_g.csv", LogFileWriter.ACCELEROMETER_DATA_TYPE, LogFileWriter.TESTING_TYPE);
        a = new LogFileWriter(test+"_a.csv", LogFileWriter.ACCELEROMETER_DATA_TYPE, LogFileWriter.TESTING_TYPE);
        w = new LogFileWriter(test+"_w.csv", LogFileWriter.ACCELEROMETER_DATA_TYPE, LogFileWriter.TESTING_TYPE);
        test++;

        for(int i = 0; i < AccData.size(); i++){
            float []vals = AccData.get(i);
            try {
                g.writeInertialDataFile(1,1,vals[0],vals[1],vals[2]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for(int i = 0; i < AccData_Without_Gravity.size(); i++){
            float []vals = AccData_Without_Gravity.get(i);
            try {
                a.writeInertialDataFile(1,1,vals[0],vals[1],vals[2]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for(int i = 0; i < GyroData.size(); i++){
            float []vals = GyroData.get(i);
            try {
                w.writeInertialDataFile(1,1,vals[0],vals[1],vals[2]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        g.closefile();
        a.closefile();
        w.closefile();*/

        // Preprocessing
        double[] gx_dataset = new double[AccData.size()],
                gy_dataset = new double[AccData.size()],
                gz_dataset = new double[AccData.size()],
                ax_dataset = new double[AccData_Without_Gravity.size()],
                ay_dataset = new double[AccData_Without_Gravity.size()],
                az_dataset = new double[AccData_Without_Gravity.size()],
                force_dataset = new double[AccData_Without_Gravity.size()],
                gravX_dataset = new double[AccData.size()],
                gravY_dataset = new double[AccData.size()],
                gravZ_dataset = new double[AccData.size()],
                wx_dataset = new double[GyroData.size()],
                wy_dataset = new double[GyroData.size()],
                wz_dataset = new double[GyroData.size()];

        for (int i = 0; i < AccData.size(); i++) {
            gx_dataset[i] = AccData.get(i)[0];
            gy_dataset[i] = AccData.get(i)[1];
            gz_dataset[i] = AccData.get(i)[2];
        }
        for (int i = 0; i < AccData_Without_Gravity.size(); i++) {
            ax_dataset[i] = AccData_Without_Gravity.get(i)[0];
            ay_dataset[i] = AccData_Without_Gravity.get(i)[1];
            az_dataset[i] = AccData_Without_Gravity.get(i)[2];
            force_dataset[i] = Math.sqrt(Math.pow(ax_dataset[i], 2) + Math.pow(ay_dataset[i], 2) + Math.pow(az_dataset[i], 2));
        }
        for (int i = 0; i < AccData.size(); i++) {
            gravX_dataset[i] = AccData.get(i)[0]-AccData_Without_Gravity.get(i)[0];
            gravY_dataset[i] = AccData.get(i)[1]-AccData_Without_Gravity.get(i)[1];
            gravZ_dataset[i] = AccData.get(i)[2]-AccData_Without_Gravity.get(i)[2];
        }
        for (int i = 0; i < GyroData.size(); i++) {
            wx_dataset[i] = GyroData.get(i)[0];
            wy_dataset[i] = GyroData.get(i)[1];
            wz_dataset[i] = GyroData.get(i)[2];
        }

        ArrayList<Float> allValues = new ArrayList<Float>();

        double max_val = max(gx_dataset), rms_val = rms(gx_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(gx_dataset));
        allValues.add((float)mean(gx_dataset));
        allValues.add((float)std(gx_dataset));
        allValues.add((float)rms(gx_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(gx_dataset));
        allValues.add((float)skewness(gx_dataset));
        allValues.add((float)kurtosis(gx_dataset));

        max_val = max(gy_dataset);
        rms_val = rms(gy_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(gy_dataset));
        allValues.add((float)mean(gy_dataset));
        allValues.add((float)std(gy_dataset));
        allValues.add((float)rms(gy_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(gy_dataset));
        allValues.add((float)skewness(gy_dataset));
        allValues.add((float)kurtosis(gy_dataset));

        max_val = max(gz_dataset);
        rms_val = rms(gz_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(gz_dataset));
        allValues.add((float)mean(gz_dataset));
        allValues.add((float)std(gz_dataset));
        allValues.add((float)rms(gz_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(gz_dataset));
        allValues.add((float)skewness(gz_dataset));
        allValues.add((float)kurtosis(gz_dataset));

        allValues.add((float)corrcoef(gx_dataset, gy_dataset));
        allValues.add((float)corrcoef(gx_dataset, gz_dataset));
        allValues.add((float)corrcoef(gy_dataset, gz_dataset));

        max_val = max(ax_dataset);
        rms_val = rms(ax_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(ax_dataset));
        allValues.add((float)mean(ax_dataset));
        allValues.add((float)std(ax_dataset));
        allValues.add((float)rms(ax_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(ax_dataset));
        allValues.add((float)skewness(ax_dataset));
        allValues.add((float)kurtosis(ax_dataset));

        max_val = max(ay_dataset);
        rms_val = rms(ay_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(ay_dataset));
        allValues.add((float)mean(ay_dataset));
        allValues.add((float)std(ay_dataset));
        allValues.add((float)rms(ay_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(ay_dataset));
        allValues.add((float)skewness(ay_dataset));
        allValues.add((float)kurtosis(ay_dataset));

        max_val = max(az_dataset);
        rms_val = rms(az_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(az_dataset));
        allValues.add((float)mean(az_dataset));
        allValues.add((float)std(az_dataset));
        allValues.add((float)rms(az_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(az_dataset));
        allValues.add((float)skewness(az_dataset));
        allValues.add((float)kurtosis(az_dataset));

        allValues.add((float)corrcoef(ax_dataset, ay_dataset));
        allValues.add((float)corrcoef(ax_dataset, az_dataset));
        allValues.add((float)corrcoef(ay_dataset, az_dataset));

        max_val = max(force_dataset);
        rms_val = rms(force_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(force_dataset));
        allValues.add((float)mean(force_dataset));
        allValues.add((float)std(force_dataset));
        allValues.add((float)rms(force_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(force_dataset));
        allValues.add((float)skewness(force_dataset));
        allValues.add((float)kurtosis(force_dataset));

        max_val = max(gravX_dataset);
        rms_val = rms(gravX_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(gravX_dataset));
        allValues.add((float)mean(gravX_dataset));
        allValues.add((float)std(gravX_dataset));
        allValues.add((float)rms(gravX_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(gravX_dataset));
        allValues.add((float)skewness(gravX_dataset));
        allValues.add((float)kurtosis(gravX_dataset));

        max_val = max(gravY_dataset);
        rms_val = rms(gravY_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(gravY_dataset));
        allValues.add((float)mean(gravY_dataset));
        allValues.add((float)std(gravY_dataset));
        allValues.add((float)rms(gravY_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(gravY_dataset));
        allValues.add((float)skewness(gravY_dataset));
        allValues.add((float)kurtosis(gravY_dataset));

        max_val = max(gravZ_dataset);
        rms_val = rms(gravZ_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(gravZ_dataset));
        allValues.add((float)mean(gravZ_dataset));
        allValues.add((float)std(gravZ_dataset));
        allValues.add((float)rms(gravZ_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(gravZ_dataset));
        allValues.add((float)skewness(gravZ_dataset));
        allValues.add((float)kurtosis(gravZ_dataset));

        allValues.add((float)corrcoef(gravX_dataset, gravY_dataset));
        allValues.add((float)corrcoef(gravX_dataset, gravZ_dataset));
        allValues.add((float)corrcoef(gravY_dataset, gravZ_dataset));

        max_val = max(wx_dataset);
        rms_val = rms(wx_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(wx_dataset));
        allValues.add((float)mean(wx_dataset));
        allValues.add((float)std(wx_dataset));
        allValues.add((float)rms(wx_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(wx_dataset));
        allValues.add((float)skewness(wx_dataset));
        allValues.add((float)kurtosis(wx_dataset));

        max_val = max(wy_dataset);
        rms_val = rms(wy_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(wy_dataset));
        allValues.add((float)mean(wy_dataset));
        allValues.add((float)std(wy_dataset));
        allValues.add((float)rms(wy_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(wy_dataset));
        allValues.add((float)skewness(wy_dataset));
        allValues.add((float)kurtosis(wy_dataset));

        max_val = max(wz_dataset);
        rms_val = rms(wz_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(wz_dataset));
        allValues.add((float)mean(wz_dataset));
        allValues.add((float)std(wz_dataset));
        allValues.add((float)rms(wz_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(wz_dataset));
        allValues.add((float)skewness(wz_dataset));
        allValues.add((float)kurtosis(wz_dataset));

        allValues.add((float)corrcoef(wx_dataset, wy_dataset));
        allValues.add((float)corrcoef(wx_dataset, wz_dataset));
        allValues.add((float)corrcoef(wy_dataset, wz_dataset));

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
        attributeList.add( new Attribute("l_gravX_Max") );
        attributeList.add( new Attribute("l_gravX_Min") );
        attributeList.add( new Attribute("l_gravX_Avg") );
        attributeList.add( new Attribute("l_gravX_Std") );
        attributeList.add( new Attribute("l_gravX_RMS") );
        attributeList.add( new Attribute("l_gravX_CF") );
        attributeList.add( new Attribute("l_gravX_Iqr") );
        attributeList.add( new Attribute("l_gravX_Skewness") );
        attributeList.add( new Attribute("l_gravX_Kurtosis") );
        attributeList.add( new Attribute("r_gravX_Max") );
        attributeList.add( new Attribute("r_gravX_Min") );
        attributeList.add( new Attribute("r_gravX_Avg") );
        attributeList.add( new Attribute("r_gravX_Std") );
        attributeList.add( new Attribute("r_gravX_RMS") );
        attributeList.add( new Attribute("r_gravX_CF") );
        attributeList.add( new Attribute("r_gravX_Iqr") );
        attributeList.add( new Attribute("r_gravX_Skewness") );
        attributeList.add( new Attribute("r_gravX_Kurtosis") );
        attributeList.add( new Attribute("l_gravY_Max") );
        attributeList.add( new Attribute("l_gravY_Min") );
        attributeList.add( new Attribute("l_gravY_Avg") );
        attributeList.add( new Attribute("l_gravY_Std") );
        attributeList.add( new Attribute("l_gravY_RMS") );
        attributeList.add( new Attribute("l_gravY_CF") );
        attributeList.add( new Attribute("l_gravY_Iqr") );
        attributeList.add( new Attribute("l_gravY_Skewness") );
        attributeList.add( new Attribute("l_gravY_Kurtosis") );
        attributeList.add( new Attribute("r_gravY_Max") );
        attributeList.add( new Attribute("r_gravY_Min") );
        attributeList.add( new Attribute("r_gravY_Avg") );
        attributeList.add( new Attribute("r_gravY_Std") );
        attributeList.add( new Attribute("r_gravY_RMS") );
        attributeList.add( new Attribute("r_gravY_CF") );
        attributeList.add( new Attribute("r_gravY_Iqr") );
        attributeList.add( new Attribute("r_gravY_Skewness") );
        attributeList.add( new Attribute("r_gravY_Kurtosis") );
        attributeList.add( new Attribute("l_gravZ_Max") );
        attributeList.add( new Attribute("l_gravZ_Min") );
        attributeList.add( new Attribute("l_gravZ_Avg") );
        attributeList.add( new Attribute("l_gravZ_Std") );
        attributeList.add( new Attribute("l_gravZ_RMS") );
        attributeList.add( new Attribute("l_gravZ_CF") );
        attributeList.add( new Attribute("l_gravZ_Iqr") );
        attributeList.add( new Attribute("l_gravZ_Skewness") );
        attributeList.add( new Attribute("l_gravZ_Kurtosis") );
        attributeList.add( new Attribute("r_gravZ_Max") );
        attributeList.add( new Attribute("r_gravZ_Min") );
        attributeList.add( new Attribute("r_gravZ_Avg") );
        attributeList.add( new Attribute("r_gravZ_Std") );
        attributeList.add( new Attribute("r_gravZ_RMS") );
        attributeList.add( new Attribute("r_gravZ_CF") );
        attributeList.add( new Attribute("r_gravZ_Iqr") );
        attributeList.add( new Attribute("r_gravZ_Skewness") );
        attributeList.add( new Attribute("r_gravZ_Kurtosis") );
        attributeList.add( new Attribute("l_gravXgravY_Corr") );
        attributeList.add( new Attribute("l_gravXgravZ_Corr") );
        attributeList.add( new Attribute("l_gravYgravZ_Corr") );
        attributeList.add( new Attribute("r_gravXgravY_Corr") );
        attributeList.add( new Attribute("r_gravXgravZ_Corr") );
        attributeList.add( new Attribute("r_gravYgravZ_Corr") );
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

        double result = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < vals.length; i++) {
            if (result < vals[i])
                result = vals[i];
        }
        return result;
    }

    private double min(final double[] vals) {
        if(vals.length == 0)
            return 0;

        double result = Double.POSITIVE_INFINITY;
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
