package nctu.nol.algo;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Vector;

import nctu.nol.bt.devices.BeaconHandler;
import nctu.nol.file.LogFileWriter;
import nctu.nol.file.SystemParameters;
import nctu.nol.file.sqlite.StrokeListItem;
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

    // SQLite Related
    private Vector<StrokeRecord> strokes = new Vector<>();

    private Context mContext;

    public Instances dataset = null;
    public ArrayList<Attribute> attributeList = new ArrayList<Attribute>();

    public StrokeClassifier(Context c) {
        this.mContext = c;
        BuildDataset();
    }

    public void initial(){
        strokes.clear();
        initLogFile();
    }

    public void close(){
        SQLiteInsertStroke(strokes,SystemParameters.TestingId);
        closeLogFile();
    }

    private void initLogFile(){
        StrokeWriter = new LogFileWriter("StrokeType.csv", LogFileWriter.STROKE_TYPE, LogFileWriter.TESTING_TYPE);
        StrokeFeature = new LogFileWriter("StrokeFeature.csv", LogFileWriter.STROKE_TYPE, LogFileWriter.TESTING_TYPE);
    }
    private void closeLogFile(){
        if(StrokeWriter != null)
            StrokeWriter.closefile();
        if(StrokeFeature != null)
            StrokeFeature.closefile();
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
            StrokeFeature.writeFeatures(allVals);

            // load classifier from file
            InputStream in_stream = mContext.getResources().openRawResource( mContext.getResources().getIdentifier("smo", "raw", mContext.getPackageName()));
            Classifier clf = (Classifier) weka.core.SerializationHelper.read(in_stream);
            result = clf.classifyInstance(inst);
            String type = dataset.classAttribute().value((int)result);

            //print result
            Log.d(TAG, dataset.classAttribute().value((int)result));
            Intent broadcast = new Intent(ACTION_OUTPUT_RESULT_STATE);
            broadcast.putExtra(EXTRA_TYPE, type);
            mContext.sendBroadcast(broadcast);


            StrokeRecord sr = new StrokeRecord(stroke_time, type);
            strokes.add(sr);

            // Log File
            try {
                StrokeWriter.writeStroke( MillisecToString(stroke_time), type);
            } catch (IOException e) {
                Log.e(TAG,e.getMessage());
            }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private void SQLiteInsertStroke(final Vector<StrokeRecord> strokes, long matching_testing_id){

        StrokeListItem slistDB = new StrokeListItem(mContext);
        for(int i = 0 ; i < strokes.size(); i++)
            slistDB.insert(strokes.get(i).strokeTime, strokes.get(i).strokeType, matching_testing_id);
        slistDB.close();
    }

    public class StrokeRecord{
        public long strokeTime;
        public String strokeType;
        public StrokeRecord(long time, String type){
            this.strokeTime = time;
            this.strokeType = type;
        }
    }

    public final ArrayList<Float> FeatureExtraction(final ArrayList<BeaconHandler.SensorData> AccData,
                                                    final ArrayList<BeaconHandler.SensorData> AccData_Without_Gravity,
                                                    final ArrayList<BeaconHandler.SensorData> GyroData) {

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
                wz_dataset = new double[GyroData.size()],
                angleX_dataset = new double[GyroData.size()],
                angleY_dataset = new double[GyroData.size()],
                angleZ_dataset = new double[GyroData.size()],
                abs_gx_dataset = new double[AccData.size()],
                abs_gy_dataset = new double[AccData.size()],
                abs_gz_dataset = new double[AccData.size()],
                abs_ax_dataset = new double[AccData.size()],
                abs_ay_dataset = new double[AccData.size()],
                abs_az_dataset = new double[AccData.size()],
                abs_gravX_dataset = new double[AccData.size()],
                abs_gravY_dataset = new double[AccData.size()],
                abs_gravZ_dataset = new double[AccData.size()],
                abs_wx_dataset = new double[GyroData.size()],
                abs_wy_dataset = new double[GyroData.size()],
                abs_wz_dataset = new double[GyroData.size()],
                abs_angleX_dataset = new double[GyroData.size()],
                abs_angleY_dataset = new double[GyroData.size()],
                abs_angleZ_dataset = new double[GyroData.size()];

        for (int i = 0; i < AccData.size(); i++) {
            gx_dataset[i] = AccData.get(i).values[0];
            gy_dataset[i] = AccData.get(i).values[1];
            gz_dataset[i] = AccData.get(i).values[2];

            abs_gx_dataset[i] = Math.abs(gx_dataset[i]);
            abs_gy_dataset[i] = Math.abs(gy_dataset[i]);
            abs_gz_dataset[i] = Math.abs(gz_dataset[i]);
        }
        for (int i = 0; i < AccData_Without_Gravity.size(); i++) {
            ax_dataset[i] = AccData_Without_Gravity.get(i).values[0];
            ay_dataset[i] = AccData_Without_Gravity.get(i).values[1];
            az_dataset[i] = AccData_Without_Gravity.get(i).values[2];
            force_dataset[i] = Math.sqrt(Math.pow(ax_dataset[i], 2) + Math.pow(ay_dataset[i], 2) + Math.pow(az_dataset[i], 2));

            abs_ax_dataset[i] = Math.abs(ax_dataset[i]);
            abs_ay_dataset[i] = Math.abs(ay_dataset[i]);
            abs_az_dataset[i] = Math.abs(az_dataset[i]);
        }
        for (int i = 0; i < AccData.size(); i++) {
            gravX_dataset[i] = AccData.get(i).values[0]-AccData_Without_Gravity.get(i).values[0];
            gravY_dataset[i] = AccData.get(i).values[1]-AccData_Without_Gravity.get(i).values[1];
            gravZ_dataset[i] = AccData.get(i).values[2]-AccData_Without_Gravity.get(i).values[2];

            abs_gravX_dataset[i] = Math.abs(gravX_dataset[i]);
            abs_gravY_dataset[i] = Math.abs(gravY_dataset[i]);
            abs_gravZ_dataset[i] = Math.abs(gravZ_dataset[i]);
        }
        for (int i = 0; i < GyroData.size(); i++) {
            wx_dataset[i] = GyroData.get(i).values[0];
            wy_dataset[i] = GyroData.get(i).values[1];
            wz_dataset[i] = GyroData.get(i).values[2];

            abs_wx_dataset[i] = Math.abs(wx_dataset[i]);
            abs_wy_dataset[i] = Math.abs(wy_dataset[i]);
            abs_wz_dataset[i] = Math.abs(wz_dataset[i]);
        }

        // Count Angle
        double sum[] = {0,0,0};
        angleX_dataset[0] = sum[0];
        angleY_dataset[0] = sum[1];
        angleZ_dataset[0] = sum[2];
        abs_angleX_dataset[0] = Math.abs(angleX_dataset[0]);
        abs_angleY_dataset[0] = Math.abs(angleY_dataset[0]);
        abs_angleZ_dataset[0] = Math.abs(angleZ_dataset[0]);
        for (int i = 1; i < GyroData.size(); i++) {
            double interval = (double)(GyroData.get(i).time - GyroData.get(i-1).time)/1000;
            sum[0] = sum[0] + interval*GyroData.get(i).values[0];
            sum[1] = sum[1] + interval*GyroData.get(i).values[1];
            sum[2] = sum[2] + interval*GyroData.get(i).values[2];

            angleX_dataset[i] = sum[0];
            angleY_dataset[i] = sum[1];
            angleZ_dataset[i] = sum[2];

            abs_angleX_dataset[i] = Math.abs(angleX_dataset[i]);
            abs_angleY_dataset[i] = Math.abs(angleY_dataset[i]);
            abs_angleZ_dataset[i] = Math.abs(angleZ_dataset[i]);
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

        max_val = max(angleX_dataset);
        rms_val = rms(angleX_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(angleX_dataset));
        allValues.add((float)mean(angleX_dataset));
        allValues.add((float)std(angleX_dataset));
        allValues.add((float)rms(angleX_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(angleX_dataset));
        allValues.add((float)skewness(angleX_dataset));
        allValues.add((float)kurtosis(angleX_dataset));

        max_val = max(angleY_dataset);
        rms_val = rms(angleY_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(angleY_dataset));
        allValues.add((float)mean(angleY_dataset));
        allValues.add((float)std(angleY_dataset));
        allValues.add((float)rms(angleY_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(angleY_dataset));
        allValues.add((float)skewness(angleY_dataset));
        allValues.add((float)kurtosis(angleY_dataset));

        max_val = max(angleZ_dataset);
        rms_val = rms(angleZ_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(angleZ_dataset));
        allValues.add((float)mean(angleZ_dataset));
        allValues.add((float)std(angleZ_dataset));
        allValues.add((float)rms(angleZ_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(angleZ_dataset));
        allValues.add((float)skewness(angleZ_dataset));
        allValues.add((float)kurtosis(angleZ_dataset));

        allValues.add((float)corrcoef(angleX_dataset, angleY_dataset));
        allValues.add((float)corrcoef(angleX_dataset, angleZ_dataset));
        allValues.add((float)corrcoef(angleY_dataset, angleZ_dataset));

        max_val = max(abs_gx_dataset);
        rms_val = rms(abs_gx_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_gx_dataset));
        allValues.add((float)mean(abs_gx_dataset));
        allValues.add((float)std(abs_gx_dataset));
        allValues.add((float)rms(abs_gx_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_gx_dataset));
        allValues.add((float)skewness(abs_gx_dataset));
        allValues.add((float)kurtosis(abs_gx_dataset));

        max_val = max(abs_gy_dataset);
        rms_val = rms(abs_gy_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_gy_dataset));
        allValues.add((float)mean(abs_gy_dataset));
        allValues.add((float)std(abs_gy_dataset));
        allValues.add((float)rms(abs_gy_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_gy_dataset));
        allValues.add((float)skewness(abs_gy_dataset));
        allValues.add((float)kurtosis(abs_gy_dataset));

        max_val = max(abs_gz_dataset);
        rms_val = rms(abs_gz_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_gz_dataset));
        allValues.add((float)mean(abs_gz_dataset));
        allValues.add((float)std(abs_gz_dataset));
        allValues.add((float)rms(abs_gz_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_gz_dataset));
        allValues.add((float)skewness(abs_gz_dataset));
        allValues.add((float)kurtosis(abs_gz_dataset));

        allValues.add((float)corrcoef(abs_gx_dataset, abs_gy_dataset));
        allValues.add((float)corrcoef(abs_gx_dataset, abs_gz_dataset));
        allValues.add((float)corrcoef(abs_gy_dataset, abs_gz_dataset));

        max_val = max(abs_ax_dataset);
        rms_val = rms(abs_ax_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_ax_dataset));
        allValues.add((float)mean(abs_ax_dataset));
        allValues.add((float)std(abs_ax_dataset));
        allValues.add((float)rms(abs_ax_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_ax_dataset));
        allValues.add((float)skewness(abs_ax_dataset));
        allValues.add((float)kurtosis(abs_ax_dataset));

        max_val = max(abs_ay_dataset);
        rms_val = rms(abs_ay_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_ay_dataset));
        allValues.add((float)mean(abs_ay_dataset));
        allValues.add((float)std(abs_ay_dataset));
        allValues.add((float)rms(abs_ay_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_ay_dataset));
        allValues.add((float)skewness(abs_ay_dataset));
        allValues.add((float)kurtosis(abs_ay_dataset));

        max_val = max(abs_az_dataset);
        rms_val = rms(abs_az_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_az_dataset));
        allValues.add((float)mean(abs_az_dataset));
        allValues.add((float)std(abs_az_dataset));
        allValues.add((float)rms(abs_az_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_az_dataset));
        allValues.add((float)skewness(abs_az_dataset));
        allValues.add((float)kurtosis(abs_az_dataset));

        allValues.add((float)corrcoef(abs_ax_dataset, abs_ay_dataset));
        allValues.add((float)corrcoef(abs_ax_dataset, abs_az_dataset));
        allValues.add((float)corrcoef(abs_ay_dataset, abs_az_dataset));

        max_val = max(abs_gravX_dataset);
        rms_val = rms(abs_gravX_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_gravX_dataset));
        allValues.add((float)mean(abs_gravX_dataset));
        allValues.add((float)std(abs_gravX_dataset));
        allValues.add((float)rms(abs_gravX_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_gravX_dataset));
        allValues.add((float)skewness(abs_gravX_dataset));
        allValues.add((float)kurtosis(abs_gravX_dataset));

        max_val = max(abs_gravY_dataset);
        rms_val = rms(abs_gravY_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_gravY_dataset));
        allValues.add((float)mean(abs_gravY_dataset));
        allValues.add((float)std(abs_gravY_dataset));
        allValues.add((float)rms(abs_gravY_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_gravY_dataset));
        allValues.add((float)skewness(abs_gravY_dataset));
        allValues.add((float)kurtosis(abs_gravY_dataset));

        max_val = max(abs_gravZ_dataset);
        rms_val = rms(abs_gravZ_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_gravZ_dataset));
        allValues.add((float)mean(abs_gravZ_dataset));
        allValues.add((float)std(abs_gravZ_dataset));
        allValues.add((float)rms(abs_gravZ_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_gravZ_dataset));
        allValues.add((float)skewness(abs_gravZ_dataset));
        allValues.add((float)kurtosis(abs_gravZ_dataset));

        allValues.add((float)corrcoef(abs_gravX_dataset, abs_gravY_dataset));
        allValues.add((float)corrcoef(abs_gravX_dataset, abs_gravZ_dataset));
        allValues.add((float)corrcoef(abs_gravY_dataset, abs_gravZ_dataset));

        max_val = max(abs_wx_dataset);
        rms_val = rms(abs_wx_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_wx_dataset));
        allValues.add((float)mean(abs_wx_dataset));
        allValues.add((float)std(abs_wx_dataset));
        allValues.add((float)rms(abs_wx_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_wx_dataset));
        allValues.add((float)skewness(abs_wx_dataset));
        allValues.add((float)kurtosis(abs_wx_dataset));

        max_val = max(abs_wy_dataset);
        rms_val = rms(abs_wy_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_wy_dataset));
        allValues.add((float)mean(abs_wy_dataset));
        allValues.add((float)std(abs_wy_dataset));
        allValues.add((float)rms(abs_wy_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_wy_dataset));
        allValues.add((float)skewness(abs_wy_dataset));
        allValues.add((float)kurtosis(abs_wy_dataset));

        max_val = max(abs_wz_dataset);
        rms_val = rms(abs_wz_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_wz_dataset));
        allValues.add((float)mean(abs_wz_dataset));
        allValues.add((float)std(abs_wz_dataset));
        allValues.add((float)rms(abs_wz_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_wz_dataset));
        allValues.add((float)skewness(abs_wz_dataset));
        allValues.add((float)kurtosis(abs_wz_dataset));

        allValues.add((float)corrcoef(abs_wx_dataset, abs_wy_dataset));
        allValues.add((float)corrcoef(abs_wx_dataset, abs_wz_dataset));
        allValues.add((float)corrcoef(abs_wy_dataset, abs_wz_dataset));

        max_val = max(abs_angleX_dataset);
        rms_val = rms(abs_angleX_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_angleX_dataset));
        allValues.add((float)mean(abs_angleX_dataset));
        allValues.add((float)std(abs_angleX_dataset));
        allValues.add((float)rms(abs_angleX_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_angleX_dataset));
        allValues.add((float)skewness(abs_angleX_dataset));
        allValues.add((float)kurtosis(abs_angleX_dataset));

        max_val = max(abs_angleY_dataset);
        rms_val = rms(abs_angleY_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_angleY_dataset));
        allValues.add((float)mean(abs_angleY_dataset));
        allValues.add((float)std(abs_angleY_dataset));
        allValues.add((float)rms(abs_angleY_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_angleY_dataset));
        allValues.add((float)skewness(abs_angleY_dataset));
        allValues.add((float)kurtosis(abs_angleY_dataset));

        max_val = max(abs_angleZ_dataset);
        rms_val = rms(abs_angleZ_dataset);
        allValues.add((float)max_val);
        allValues.add((float)min(abs_angleZ_dataset));
        allValues.add((float)mean(abs_angleZ_dataset));
        allValues.add((float)std(abs_angleZ_dataset));
        allValues.add((float)rms(abs_angleZ_dataset));
        allValues.add((float)CF(max_val, rms_val));
        allValues.add((float)iqr(abs_angleZ_dataset));
        allValues.add((float)skewness(abs_angleZ_dataset));
        allValues.add((float)kurtosis(abs_angleZ_dataset));

        allValues.add((float)corrcoef(abs_angleX_dataset, abs_angleY_dataset));
        allValues.add((float)corrcoef(abs_angleX_dataset, abs_angleZ_dataset));
        allValues.add((float)corrcoef(abs_angleY_dataset, abs_angleZ_dataset));

        return allValues;
    }

    private void BuildDataset() {
        // Create attributes to be used with classifiers
        attributeList.add( new Attribute("gx_Max") );
        attributeList.add( new Attribute("gx_Min") );
        attributeList.add( new Attribute("gx_Avg") );
        attributeList.add( new Attribute("gx_Std") );
        attributeList.add( new Attribute("gx_RMS") );
        attributeList.add( new Attribute("gx_CF") );
        attributeList.add( new Attribute("gx_Iqr") );
        attributeList.add( new Attribute("gx_Skewness") );
        attributeList.add( new Attribute("gx_Kurtosis") );
        attributeList.add( new Attribute("gy_Max") );
        attributeList.add( new Attribute("gy_Min") );
        attributeList.add( new Attribute("gy_Avg") );
        attributeList.add( new Attribute("gy_Std") );
        attributeList.add( new Attribute("gy_RMS") );
        attributeList.add( new Attribute("gy_CF") );
        attributeList.add( new Attribute("gy_Iqr") );
        attributeList.add( new Attribute("gy_Skewness") );
        attributeList.add( new Attribute("gy_Kurtosis") );
        attributeList.add( new Attribute("gz_Max") );
        attributeList.add( new Attribute("gz_Min") );
        attributeList.add( new Attribute("gz_Avg") );
        attributeList.add( new Attribute("gz_Std") );
        attributeList.add( new Attribute("gz_RMS") );
        attributeList.add( new Attribute("gz_CF") );
        attributeList.add( new Attribute("gz_Iqr") );
        attributeList.add( new Attribute("gz_Skewness") );
        attributeList.add( new Attribute("gz_Kurtosis") );
        attributeList.add( new Attribute("gxgy_Corr") );
        attributeList.add( new Attribute("gxgz_Corr") );
        attributeList.add( new Attribute("gygz_Corr") );
        attributeList.add( new Attribute("ax_Max") );
        attributeList.add( new Attribute("ax_Min") );
        attributeList.add( new Attribute("ax_Avg") );
        attributeList.add( new Attribute("ax_Std") );
        attributeList.add( new Attribute("ax_RMS") );
        attributeList.add( new Attribute("ax_CF") );
        attributeList.add( new Attribute("ax_Iqr") );
        attributeList.add( new Attribute("ax_Skewness") );
        attributeList.add( new Attribute("ax_Kurtosis") );
        attributeList.add( new Attribute("ay_Max") );
        attributeList.add( new Attribute("ay_Min") );
        attributeList.add( new Attribute("ay_Avg") );
        attributeList.add( new Attribute("ay_Std") );
        attributeList.add( new Attribute("ay_RMS") );
        attributeList.add( new Attribute("ay_CF") );
        attributeList.add( new Attribute("ay_Iqr") );
        attributeList.add( new Attribute("ay_Skewness") );
        attributeList.add( new Attribute("ay_Kurtosis") );
        attributeList.add( new Attribute("az_Max") );
        attributeList.add( new Attribute("az_Min") );
        attributeList.add( new Attribute("az_Avg") );
        attributeList.add( new Attribute("az_Std") );
        attributeList.add( new Attribute("az_RMS") );
        attributeList.add( new Attribute("az_CF") );
        attributeList.add( new Attribute("az_Iqr") );
        attributeList.add( new Attribute("az_Skewness") );
        attributeList.add( new Attribute("az_Kurtosis") );
        attributeList.add( new Attribute("axay_Corr") );
        attributeList.add( new Attribute("axaz_Corr") );
        attributeList.add( new Attribute("ayaz_Corr") );
        attributeList.add( new Attribute("force_Max") );
        attributeList.add( new Attribute("force_Min") );
        attributeList.add( new Attribute("force_Avg") );
        attributeList.add( new Attribute("force_Std") );
        attributeList.add( new Attribute("force_RMS") );
        attributeList.add( new Attribute("force_CF") );
        attributeList.add( new Attribute("force_Iqr") );
        attributeList.add( new Attribute("force_Skewness") );
        attributeList.add( new Attribute("force_Kurtosis") );
        attributeList.add( new Attribute("gravX_Max") );
        attributeList.add( new Attribute("gravX_Min") );
        attributeList.add( new Attribute("gravX_Avg") );
        attributeList.add( new Attribute("gravX_Std") );
        attributeList.add( new Attribute("gravX_RMS") );
        attributeList.add( new Attribute("gravX_CF") );
        attributeList.add( new Attribute("gravX_Iqr") );
        attributeList.add( new Attribute("gravX_Skewness") );
        attributeList.add( new Attribute("gravX_Kurtosis") );
        attributeList.add( new Attribute("gravY_Max") );
        attributeList.add( new Attribute("gravY_Min") );
        attributeList.add( new Attribute("gravY_Avg") );
        attributeList.add( new Attribute("gravY_Std") );
        attributeList.add( new Attribute("gravY_RMS") );
        attributeList.add( new Attribute("gravY_CF") );
        attributeList.add( new Attribute("gravY_Iqr") );
        attributeList.add( new Attribute("gravY_Skewness") );
        attributeList.add( new Attribute("gravY_Kurtosis") );
        attributeList.add( new Attribute("gravZ_Max") );
        attributeList.add( new Attribute("gravZ_Min") );
        attributeList.add( new Attribute("gravZ_Avg") );
        attributeList.add( new Attribute("gravZ_Std") );
        attributeList.add( new Attribute("gravZ_RMS") );
        attributeList.add( new Attribute("gravZ_CF") );
        attributeList.add( new Attribute("gravZ_Iqr") );
        attributeList.add( new Attribute("gravZ_Skewness") );
        attributeList.add( new Attribute("gravZ_Kurtosis") );
        attributeList.add( new Attribute("gravXgravY_Corr") );
        attributeList.add( new Attribute("gravXgravZ_Corr") );
        attributeList.add( new Attribute("gravYgravZ_Corr") );
        attributeList.add( new Attribute("wx_Max") );
        attributeList.add( new Attribute("wx_Min") );
        attributeList.add( new Attribute("wx_Avg") );
        attributeList.add( new Attribute("wx_Std") );
        attributeList.add( new Attribute("wx_RMS") );
        attributeList.add( new Attribute("wx_CF") );
        attributeList.add( new Attribute("wx_Iqr") );
        attributeList.add( new Attribute("wx_Skewness") );
        attributeList.add( new Attribute("wx_Kurtosis") );
        attributeList.add( new Attribute("wy_Max") );
        attributeList.add( new Attribute("wy_Min") );
        attributeList.add( new Attribute("wy_Avg") );
        attributeList.add( new Attribute("wy_Std") );
        attributeList.add( new Attribute("wy_RMS") );
        attributeList.add( new Attribute("wy_CF") );
        attributeList.add( new Attribute("wy_Iqr") );
        attributeList.add( new Attribute("wy_Skewness") );
        attributeList.add( new Attribute("wy_Kurtosis") );
        attributeList.add( new Attribute("wz_Max") );
        attributeList.add( new Attribute("wz_Min") );
        attributeList.add( new Attribute("wz_Avg") );
        attributeList.add( new Attribute("wz_Std") );
        attributeList.add( new Attribute("wz_RMS") );
        attributeList.add( new Attribute("wz_CF") );
        attributeList.add( new Attribute("wz_Iqr") );
        attributeList.add( new Attribute("wz_Skewness") );
        attributeList.add( new Attribute("wz_Kurtosis") );
        attributeList.add( new Attribute("wxwy_Corr") );
        attributeList.add( new Attribute("wxwz_Corr") );
        attributeList.add( new Attribute("wywz_Corr") );
        attributeList.add( new Attribute("angleX_Max") );
        attributeList.add( new Attribute("angleX_Min") );
        attributeList.add( new Attribute("angleX_Avg") );
        attributeList.add( new Attribute("angleX_Std") );
        attributeList.add( new Attribute("angleX_RMS") );
        attributeList.add( new Attribute("angleX_CF") );
        attributeList.add( new Attribute("angleX_Iqr") );
        attributeList.add( new Attribute("angleX_Skewness") );
        attributeList.add( new Attribute("angleX_Kurtosis") );
        attributeList.add( new Attribute("angleY_Max") );
        attributeList.add( new Attribute("angleY_Min") );
        attributeList.add( new Attribute("angleY_Avg") );
        attributeList.add( new Attribute("angleY_Std") );
        attributeList.add( new Attribute("angleY_RMS") );
        attributeList.add( new Attribute("angleY_CF") );
        attributeList.add( new Attribute("angleY_Iqr") );
        attributeList.add( new Attribute("angleY_Skewness") );
        attributeList.add( new Attribute("angleY_Kurtosis") );
        attributeList.add( new Attribute("angleZ_Max") );
        attributeList.add( new Attribute("angleZ_Min") );
        attributeList.add( new Attribute("angleZ_Avg") );
        attributeList.add( new Attribute("angleZ_Std") );
        attributeList.add( new Attribute("angleZ_RMS") );
        attributeList.add( new Attribute("angleZ_CF") );
        attributeList.add( new Attribute("angleZ_Iqr") );
        attributeList.add( new Attribute("angleZ_Skewness") );
        attributeList.add( new Attribute("angleZ_Kurtosis") );
        attributeList.add( new Attribute("angleXangleY_Corr") );
        attributeList.add( new Attribute("angleXangleZ_Corr") );
        attributeList.add( new Attribute("angleYangleZ_Corr") );
        attributeList.add( new Attribute("abs_gx_Max") );
        attributeList.add( new Attribute("abs_gx_Min") );
        attributeList.add( new Attribute("abs_gx_Avg") );
        attributeList.add( new Attribute("abs_gx_Std") );
        attributeList.add( new Attribute("abs_gx_RMS") );
        attributeList.add( new Attribute("abs_gx_CF") );
        attributeList.add( new Attribute("abs_gx_Iqr") );
        attributeList.add( new Attribute("abs_gx_Skewness") );
        attributeList.add( new Attribute("abs_gx_Kurtosis") );
        attributeList.add( new Attribute("abs_gy_Max") );
        attributeList.add( new Attribute("abs_gy_Min") );
        attributeList.add( new Attribute("abs_gy_Avg") );
        attributeList.add( new Attribute("abs_gy_Std") );
        attributeList.add( new Attribute("abs_gy_RMS") );
        attributeList.add( new Attribute("abs_gy_CF") );
        attributeList.add( new Attribute("abs_gy_Iqr") );
        attributeList.add( new Attribute("abs_gy_Skewness") );
        attributeList.add( new Attribute("abs_gy_Kurtosis") );
        attributeList.add( new Attribute("abs_gz_Max") );
        attributeList.add( new Attribute("abs_gz_Min") );
        attributeList.add( new Attribute("abs_gz_Avg") );
        attributeList.add( new Attribute("abs_gz_Std") );
        attributeList.add( new Attribute("abs_gz_RMS") );
        attributeList.add( new Attribute("abs_gz_CF") );
        attributeList.add( new Attribute("abs_gz_Iqr") );
        attributeList.add( new Attribute("abs_gz_Skewness") );
        attributeList.add( new Attribute("abs_gz_Kurtosis") );
        attributeList.add( new Attribute("abs_gxgy_Corr") );
        attributeList.add( new Attribute("abs_gxgz_Corr") );
        attributeList.add( new Attribute("abs_gygz_Corr") );
        attributeList.add( new Attribute("abs_ax_Max") );
        attributeList.add( new Attribute("abs_ax_Min") );
        attributeList.add( new Attribute("abs_ax_abs_avg") );
        attributeList.add( new Attribute("abs_ax_Std") );
        attributeList.add( new Attribute("abs_ax_RMS") );
        attributeList.add( new Attribute("abs_ax_CF") );
        attributeList.add( new Attribute("abs_ax_Iqr") );
        attributeList.add( new Attribute("abs_ax_Skewness") );
        attributeList.add( new Attribute("abs_ax_Kurtosis") );
        attributeList.add( new Attribute("abs_ay_Max") );
        attributeList.add( new Attribute("abs_ay_Min") );
        attributeList.add( new Attribute("abs_ay_abs_avg") );
        attributeList.add( new Attribute("abs_ay_Std") );
        attributeList.add( new Attribute("abs_ay_RMS") );
        attributeList.add( new Attribute("abs_ay_CF") );
        attributeList.add( new Attribute("abs_ay_Iqr") );
        attributeList.add( new Attribute("abs_ay_Skewness") );
        attributeList.add( new Attribute("abs_ay_Kurtosis") );
        attributeList.add( new Attribute("abs_az_Max") );
        attributeList.add( new Attribute("abs_az_Min") );
        attributeList.add( new Attribute("abs_az_abs_avg") );
        attributeList.add( new Attribute("abs_az_Std") );
        attributeList.add( new Attribute("abs_az_RMS") );
        attributeList.add( new Attribute("abs_az_CF") );
        attributeList.add( new Attribute("abs_az_Iqr") );
        attributeList.add( new Attribute("abs_az_Skewness") );
        attributeList.add( new Attribute("abs_az_Kurtosis") );
        attributeList.add( new Attribute("abs_axay_Corr") );
        attributeList.add( new Attribute("abs_axaz_Corr") );
        attributeList.add( new Attribute("abs_ayaz_Corr") );
        attributeList.add( new Attribute("abs_gravX_Max") );
        attributeList.add( new Attribute("abs_gravX_Min") );
        attributeList.add( new Attribute("abs_gravX_Avg") );
        attributeList.add( new Attribute("abs_gravX_Std") );
        attributeList.add( new Attribute("abs_gravX_RMS") );
        attributeList.add( new Attribute("abs_gravX_CF") );
        attributeList.add( new Attribute("abs_gravX_Iqr") );
        attributeList.add( new Attribute("abs_gravX_Skewness") );
        attributeList.add( new Attribute("abs_gravX_Kurtosis") );
        attributeList.add( new Attribute("abs_gravY_Max") );
        attributeList.add( new Attribute("abs_gravY_Min") );
        attributeList.add( new Attribute("abs_gravY_Avg") );
        attributeList.add( new Attribute("abs_gravY_Std") );
        attributeList.add( new Attribute("abs_gravY_RMS") );
        attributeList.add( new Attribute("abs_gravY_CF") );
        attributeList.add( new Attribute("abs_gravY_Iqr") );
        attributeList.add( new Attribute("abs_gravY_Skewness") );
        attributeList.add( new Attribute("abs_gravY_Kurtosis") );
        attributeList.add( new Attribute("abs_gravZ_Max") );
        attributeList.add( new Attribute("abs_gravZ_Min") );
        attributeList.add( new Attribute("abs_gravZ_Avg") );
        attributeList.add( new Attribute("abs_gravZ_Std") );
        attributeList.add( new Attribute("abs_gravZ_RMS") );
        attributeList.add( new Attribute("abs_gravZ_CF") );
        attributeList.add( new Attribute("abs_gravZ_Iqr") );
        attributeList.add( new Attribute("abs_gravZ_Skewness") );
        attributeList.add( new Attribute("abs_gravZ_Kurtosis") );
        attributeList.add( new Attribute("abs_gravXgravY_Corr") );
        attributeList.add( new Attribute("abs_gravXgravZ_Corr") );
        attributeList.add( new Attribute("abs_gravYgravZ_Corr") );
        attributeList.add( new Attribute("abs_wx_Max") );
        attributeList.add( new Attribute("abs_wx_Min") );
        attributeList.add( new Attribute("abs_wx_Avg") );
        attributeList.add( new Attribute("abs_wx_Std") );
        attributeList.add( new Attribute("abs_wx_RMS") );
        attributeList.add( new Attribute("abs_wx_CF") );
        attributeList.add( new Attribute("abs_wx_Iqr") );
        attributeList.add( new Attribute("abs_wx_Skewness") );
        attributeList.add( new Attribute("abs_wx_Kurtosis") );
        attributeList.add( new Attribute("abs_wy_Max") );
        attributeList.add( new Attribute("abs_wy_Min") );
        attributeList.add( new Attribute("abs_wy_Avg") );
        attributeList.add( new Attribute("abs_wy_Std") );
        attributeList.add( new Attribute("abs_wy_RMS") );
        attributeList.add( new Attribute("abs_wy_CF") );
        attributeList.add( new Attribute("abs_wy_Iqr") );
        attributeList.add( new Attribute("abs_wy_Skewness") );
        attributeList.add( new Attribute("abs_wy_Kurtosis") );
        attributeList.add( new Attribute("abs_wz_Max") );
        attributeList.add( new Attribute("abs_wz_Min") );
        attributeList.add( new Attribute("abs_wz_Avg") );
        attributeList.add( new Attribute("abs_wz_Std") );
        attributeList.add( new Attribute("abs_wz_RMS") );
        attributeList.add( new Attribute("abs_wz_CF") );
        attributeList.add( new Attribute("abs_wz_Iqr") );
        attributeList.add( new Attribute("abs_wz_Skewness") );
        attributeList.add( new Attribute("abs_wz_Kurtosis") );
        attributeList.add( new Attribute("abs_wxwy_Corr") );
        attributeList.add( new Attribute("abs_wxwz_Corr") );
        attributeList.add( new Attribute("abs_wywz_Corr") );
        attributeList.add( new Attribute("abs_angleX_Max") );
        attributeList.add( new Attribute("abs_angleX_Min") );
        attributeList.add( new Attribute("abs_angleX_Avg") );
        attributeList.add( new Attribute("abs_angleX_Std") );
        attributeList.add( new Attribute("abs_angleX_RMS") );
        attributeList.add( new Attribute("abs_angleX_CF") );
        attributeList.add( new Attribute("abs_angleX_Iqr") );
        attributeList.add( new Attribute("abs_angleX_Skewness") );
        attributeList.add( new Attribute("abs_angleX_Kurtosis") );
        attributeList.add( new Attribute("abs_angleY_Max") );
        attributeList.add( new Attribute("abs_angleY_Min") );
        attributeList.add( new Attribute("abs_angleY_Avg") );
        attributeList.add( new Attribute("abs_angleY_Std") );
        attributeList.add( new Attribute("abs_angleY_RMS") );
        attributeList.add( new Attribute("abs_angleY_CF") );
        attributeList.add( new Attribute("abs_angleY_Iqr") );
        attributeList.add( new Attribute("abs_angleY_Skewness") );
        attributeList.add( new Attribute("abs_angleY_Kurtosis") );
        attributeList.add( new Attribute("abs_angleZ_Max") );
        attributeList.add( new Attribute("abs_angleZ_Min") );
        attributeList.add( new Attribute("abs_angleZ_Avg") );
        attributeList.add( new Attribute("abs_angleZ_Std") );
        attributeList.add( new Attribute("abs_angleZ_RMS") );
        attributeList.add( new Attribute("abs_angleZ_CF") );
        attributeList.add( new Attribute("abs_angleZ_Iqr") );
        attributeList.add( new Attribute("abs_angleZ_Skewness") );
        attributeList.add( new Attribute("abs_angleZ_Kurtosis") );
        attributeList.add( new Attribute("abs_angleXangleY_Corr") );
        attributeList.add( new Attribute("abs_angleXangleZ_Corr") );
        attributeList.add( new Attribute("abs_angleYangleZ_Corr") );

        // Result Type
        ArrayList<String> classVal = new ArrayList<String>();
        classVal.add("netplay");
        classVal.add("lob");
        classVal.add("drive");
        classVal.add("drop");
        classVal.add("long");
        classVal.add("smash");
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

    public static String MillisecToString(long timestamp){

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
