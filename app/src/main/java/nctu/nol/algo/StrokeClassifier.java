package nctu.nol.algo;


import android.util.Log;

import java.util.ArrayList;

import weka.classifiers.Classifier;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.Attribute;

public class StrokeClassifier {
    private final static String TAG = StrokeClassifier.class.getSimpleName();

    public Instances dataset = null;
    public ArrayList<Attribute> attributeList = new ArrayList<Attribute>();

    public StrokeClassifier(){ BuildDataset(); }

    private double classify(/* Add Parameters */)  {

        double result = -1;
        try {
            // Single Data
            DenseInstance inst = new DenseInstance(dataset.numAttributes());
            inst.setDataset(dataset);

            // Set instance's values for the attributes
            /*inst.setValue(attributeList.get(0), 123);
            inst.setValue(attributeList.get(1), 0);*/

            // load classifier from file
            Classifier smo = (Classifier)weka.core.SerializationHelper.read("smo.model");
            result = smo.classifyInstance(inst);

            //print result
            Log.d(TAG, dataset.classAttribute().value((int) inst.classValue()));

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private void BuildDataset(){
        // Create attributes to be used with classifiers
        attributeList.add( new Attribute("gx_Max") );
        attributeList.add( new Attribute("gx_Min") );
        attributeList.add( new Attribute("gx_Std") );
        attributeList.add( new Attribute("gx_Avg") );
        attributeList.add( new Attribute("gx_RMS") );
        attributeList.add( new Attribute("gx_CF") );
        attributeList.add( new Attribute("gx_Iqr") );
        attributeList.add( new Attribute("gx_Skewness") );
        attributeList.add( new Attribute("gx_Kurtosis") );
        attributeList.add( new Attribute("gy_Max") );
        attributeList.add( new Attribute("gy_Min") );
        attributeList.add( new Attribute("gy_Std") );
        attributeList.add( new Attribute("gy_Avg") );
        attributeList.add( new Attribute("gy_RMS") );
        attributeList.add( new Attribute("gy_CF") );
        attributeList.add( new Attribute("gy_Iqr") );
        attributeList.add( new Attribute("gy_Skewness") );
        attributeList.add( new Attribute("gy_Kurtosis") );
        attributeList.add( new Attribute("gz_Max") );
        attributeList.add( new Attribute("gz_Min") );
        attributeList.add( new Attribute("gz_Std") );
        attributeList.add( new Attribute("gz_Avg") );
        attributeList.add( new Attribute("gz_RMS") );
        attributeList.add( new Attribute("gz_CF") );
        attributeList.add( new Attribute("gz_Iqr") );
        attributeList.add( new Attribute("gz_Skewness") );
        attributeList.add( new Attribute("gz_Kurtosis") );
        attributeList.add( new Attribute("ax_Max") );
        attributeList.add( new Attribute("ax_Min") );
        attributeList.add( new Attribute("ax_Std") );
        attributeList.add( new Attribute("ax_Avg") );
        attributeList.add( new Attribute("ax_RMS") );
        attributeList.add( new Attribute("ax_CF") );
        attributeList.add( new Attribute("ax_Iqr") );
        attributeList.add( new Attribute("ax_Skewness") );
        attributeList.add( new Attribute("ax_Kurtosis") );
        attributeList.add( new Attribute("ay_Max") );
        attributeList.add( new Attribute("ay_Min") );
        attributeList.add( new Attribute("ay_Std") );
        attributeList.add( new Attribute("ay_Avg") );
        attributeList.add( new Attribute("ay_RMS") );
        attributeList.add( new Attribute("ay_CF") );
        attributeList.add( new Attribute("ay_Iqr") );
        attributeList.add( new Attribute("ay_Skewness") );
        attributeList.add( new Attribute("ay_Kurtosis") );
        attributeList.add( new Attribute("az_Max") );
        attributeList.add( new Attribute("az_Min") );
        attributeList.add( new Attribute("az_Std") );
        attributeList.add( new Attribute("az_Avg") );
        attributeList.add( new Attribute("az_RMS") );
        attributeList.add( new Attribute("az_CF") );
        attributeList.add( new Attribute("az_Iqr") );
        attributeList.add( new Attribute("az_Skewness") );
        attributeList.add( new Attribute("az_Kurtosis") );
        attributeList.add( new Attribute("gxgy_corr") );
        attributeList.add( new Attribute("gxgz_corr") );
        attributeList.add( new Attribute("gygz_corr") );
        attributeList.add( new Attribute("wx_Max") );
        attributeList.add( new Attribute("wx_Min") );
        attributeList.add( new Attribute("wx_Std") );
        attributeList.add( new Attribute("wx_Avg") );
        attributeList.add( new Attribute("wx_RMS") );
        attributeList.add( new Attribute("wx_CF") );
        attributeList.add( new Attribute("wx_Iqr") );
        attributeList.add( new Attribute("wx_Skewness") );
        attributeList.add( new Attribute("wx_Kurtosis") );
        attributeList.add( new Attribute("wy_Max") );
        attributeList.add( new Attribute("wy_Min") );
        attributeList.add( new Attribute("wy_Std") );
        attributeList.add( new Attribute("wy_Avg") );
        attributeList.add( new Attribute("wy_RMS") );
        attributeList.add( new Attribute("wy_CF") );
        attributeList.add( new Attribute("wy_Iqr") );
        attributeList.add( new Attribute("wy_Skewness") );
        attributeList.add( new Attribute("wy_Kurtosis") );
        attributeList.add( new Attribute("wz_Max") );
        attributeList.add( new Attribute("wz_Min") );
        attributeList.add( new Attribute("wz_Std") );
        attributeList.add( new Attribute("wz_Avg") );
        attributeList.add( new Attribute("wz_RMS") );
        attributeList.add( new Attribute("wz_CF") );
        attributeList.add( new Attribute("wz_Iqr") );
        attributeList.add( new Attribute("wz_Skewness") );
        attributeList.add( new Attribute("wz_Kurtosis") );
        attributeList.add( new Attribute("wxwy_corr") );
        attributeList.add( new Attribute("wxwz_corr") );
        attributeList.add( new Attribute("wywz_corr") );

        // Result Type
        ArrayList<String> classVal = new ArrayList<String>();
        classVal.add("netplay");
        classVal.add("lob");
        classVal.add("drive");
        classVal.add("drop");
        classVal.add("long");
        classVal.add("smash");
        classVal.add("forehand serve");
        attributeList.add(new Attribute("Type",classVal));

        // Empty Test Dataset
        dataset = new Instances("TestInstances",attributeList,0);
        dataset.setClassIndex(dataset.numAttributes() - 1);
    }
}
