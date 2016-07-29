package nctu.nol.badmintonlogprogram.chart;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart;
import org.achartengine.chart.CombinedXYChart;
import org.achartengine.chart.LineChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.ScatterChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by user on 2016/7/21.
 */
public class AudioWaveChart {
    private final static String TAG = AudioWaveChart.class.getSimpleName();
    public final static String ACTION_CLICK_EVENT = "AUDIOWAVECHART.ACTION_CLICK_EVENT";
    public final static String EXTRA_CLICK_POSITION_TIME = "AudioWaveChart.EXTRA_CLICK_POSITION_TIME";

    private Context context;

    // Dataset
    private List<double[]> list_X_dataset = new ArrayList<double[]>(); // 點的x坐標, 每個element代表一種資料
    private List<double[]> list_Y_dataset = new ArrayList<double[]>(); // 點的y坐標
    private List<Integer> list_color = new ArrayList<Integer>(); // 不同資料集的代表顏色

    // Chart
    private RelativeLayout layout;
    private XYMultipleSeriesRenderer renderer = null;
    private XYMultipleSeriesDataset XYDataset = new XYMultipleSeriesDataset();
    private float XaxisMax = Float.NEGATIVE_INFINITY, XaxisMin = Float.POSITIVE_INFINITY;
    private int PrevIndexForChangeColor = -1;
    private int PrevColorForChangeColor = -1;
    private GraphicalView chart;
    private final static long ChartRangeMilliSecond = 400;

    public AudioWaveChart(Context c, RelativeLayout l){
        this.context = c;
        this.layout = l;
    }

    public void AddChartDataset(double[] time, double[] val, int color){
        // Original data setting
        for(int i = 0; i < time.length; i++){
            if(XaxisMax < time[i] )
                XaxisMax = (float)time[i];
            if( XaxisMin > time[i] )
                XaxisMin = (float)time[i];
        }
        list_X_dataset.add(time);
        list_Y_dataset.add(val);
        list_color.add(color);
    }

    public void ClearAllDataset(){
        list_X_dataset.clear();
        list_Y_dataset.clear();
        list_color.clear();
        XYDataset.clear();
    }

    public void MovePointToCenter(double x, double left_percent, double right_percent){
        if(renderer != null && chart != null){
            if( x-ChartRangeMilliSecond*left_percent < XaxisMin ){
                renderer.setXAxisMin(XaxisMin);
                renderer.setXAxisMax(XaxisMin + ChartRangeMilliSecond);
            }else if( x+ChartRangeMilliSecond*right_percent > XaxisMax ){
                renderer.setXAxisMin(XaxisMax - ChartRangeMilliSecond);
                renderer.setXAxisMax(XaxisMax);
            }else {
                renderer.setXAxisMin(x - ChartRangeMilliSecond * left_percent);
                renderer.setXAxisMax(x + ChartRangeMilliSecond * right_percent);
            }
            chart.repaint();
        }
    }

    public void MakeChart(){
        buildDataset(list_X_dataset, list_Y_dataset); // 儲存座標值

        // Line Render
        //PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE, PointStyle.DIAMOND }; // 折線點的形狀
        renderer = buildRenderer(list_color, true);

        // Chart Render
        setChartSettings(renderer, "Time (ms)", "", XaxisMin, XaxisMax, -1, 1);// 定義折線圖


        CombinedXYChart.XYCombinedChartDef[] types = new CombinedXYChart.XYCombinedChartDef[list_X_dataset.size()];
        for(int i = 0; i < list_X_dataset.size(); i++){
            if(i == 1)
                types[i] = new CombinedXYChart.XYCombinedChartDef(ScatterChart.TYPE, i);
            else
                types[i] = new CombinedXYChart.XYCombinedChartDef(LineChart.TYPE, i);
        }
        chart = ChartFactory.getCombinedXYChartView(context, XYDataset, renderer, types);
        chart.setOnLongClickListener(ChartClickListener);

        layout.removeAllViews();
        layout.addView(chart);

        PrevIndexForChangeColor = -1;
        PrevColorForChangeColor = -1;
    }

    private View.OnLongClickListener ChartClickListener = new View.OnLongClickListener(){
        public boolean onLongClick(View v) {
            //Log.e(TAG, "X :" + chart.toRealPoint(0)[0]);
            //Log.e(TAG, "Y :" + chart.toRealPoint(0)[1]);

            Intent broadcast = new Intent(ACTION_CLICK_EVENT);
            broadcast.putExtra(EXTRA_CLICK_POSITION_TIME, chart.toRealPoint(0)[0]);
            context.sendBroadcast(broadcast);

            return true;
        }
    };

    public void ChangeSeriesColor(int index, int color){
        if(renderer != null) {
            if (PrevIndexForChangeColor != -1) {
                final XYSeriesRenderer r =  (XYSeriesRenderer)renderer.getSeriesRendererAt(PrevIndexForChangeColor);
                final XYSeriesRenderer.FillOutsideLine fillcolor = r.getFillOutsideLine()[0];
                fillcolor.setColor(PrevColorForChangeColor);
            }

            final XYSeriesRenderer r =  (XYSeriesRenderer)renderer.getSeriesRendererAt(index);
            final XYSeriesRenderer.FillOutsideLine fillcolor = r.getFillOutsideLine()[0];
            PrevIndexForChangeColor = index;
            PrevColorForChangeColor = fillcolor.getColor();
            fillcolor.setColor(color);

        }
        if(chart != null)
            chart.repaint();
    }

    // 設定圖表樣式渲染
    private void setChartSettings(XYMultipleSeriesRenderer renderer, String xTitle,
                                  String yTitle, double xMin, double xMax, double yMin, double yMax) {
        //renderer.setChartTitle(title); // 折線圖名稱
        //renderer.setChartTitleTextSize(24); // 折線圖名稱字形大小

        renderer.setXTitle(xTitle); // X軸名稱
        renderer.setXAxisMin(xMin); // X軸顯示最小值
        renderer.setXAxisMax(xMin + ChartRangeMilliSecond); // X軸顯示最大值
        renderer.setXLabelsColor(Color.BLACK); // X軸線顏色

        renderer.setYTitle(yTitle); // Y軸名稱
        renderer.setYAxisMin(yMin); // Y軸顯示最小值
        renderer.setYAxisMax(yMax); // Y軸顯示最大值
        renderer.setYLabelsColor(0, Color.BLACK); // Y軸線顏色
        renderer.setYLabelsAlign(Paint.Align.RIGHT);

        renderer.setAxisTitleTextSize(28);
        renderer.setAxesColor(Color.BLACK); // 設定坐標軸顏色
        renderer.setLabelsTextSize(28);
        renderer.setLabelsColor(Color.BLACK); // 設定標籤顏色

        renderer.setMarginsColor(Color.parseColor("#eeeeee")); // 設定背景顏色
        renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(Color.argb(255, 238, 238, 238));
        renderer.setShowGrid(true); // 設定格線
        renderer.setGridColor(Color.LTGRAY);
        renderer.setShowLegend(false);
        renderer.setMargins(new int[]{25, 50, 10, 50});

        renderer.setZoomEnabled(false, false);
        renderer.setPanEnabled(true, false);
        renderer.setClickEnabled(true);

        //限制Scrolling的範圍
        double[] PanLimits={xMin,xMax,yMin,yMax}; // [panMinimumX, panMaximumX, panMinimumY, panMaximumY]
        renderer.setPanLimits(PanLimits);

    }

    // 定義折線、點、長條的格式
    private XYMultipleSeriesRenderer buildRenderer(List<Integer> colors, /*PointStyle[] styles,*/ boolean fill) {
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        int length = colors.size();
        //折線
        if(length > 0) {
            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(colors.get(0));
            r.setFillPoints(fill);
            renderer.addSeriesRenderer(r); //將座標變成線加入圖中顯示
        }

        //波峰點
        if (length > 1){
            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setPointStyle(PointStyle.CIRCLE);
            r.setColor(colors.get(1));
            r.setFillPoints(true);
            renderer.addSeriesRenderer(r);
            renderer.setPointSize(5.0f);
        }

        for(int i = 2; i < length; i++){
            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(colors.get(i));
            r.setFillPoints(true);
            XYSeriesRenderer.FillOutsideLine fillcolor = new XYSeriesRenderer.FillOutsideLine(XYSeriesRenderer.FillOutsideLine.Type.BELOW);
            fillcolor.setColor(colors.get(i));
            r.addFillOutsideLine(fillcolor);
            renderer.addSeriesRenderer(r); //將座標變成線加入圖中顯示
        }

        return renderer;
    }

    // 資料處理
    private void buildDataset(List<double[]> xValues, List<double[]> yValues) {
        XYDataset.clear();

        int length = xValues.size(); // 資料集數量
        for (int i = 0; i < length; i++) {
            // XYseries對象,用於提供繪製的點集合的資料
            XYSeries series = new XYSeries("Dataset_" + i); // 依據每條線的名稱新增
            double[] xV = xValues.get(i); // 獲取第i條線的資料
            double[] yV = yValues.get(i);
            int seriesLength = xV.length; // 有幾個點

            for (int k = 0; k < seriesLength; k++) // 每條線裡有幾個點
                series.add(xV[k], yV[k]);
            XYDataset.addSeries(series);
        }
    }
}
