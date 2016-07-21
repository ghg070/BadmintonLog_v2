package nctu.nol.badmintonlogprogram;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Smile on 2016/7/21.
 */
public class ShowTrainingData extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_training);

        LinearLayout chart_layout = (LinearLayout)findViewById(R.id.chart_whole_audio_wave);


        String[] titles = new String[] { "Points" }; // 定義折線的名稱
        List<double[]> x = new ArrayList<double[]>(); // 點的x坐標
        List<double[]> y = new ArrayList<double[]>(); // 點的y坐標
        // 數值X,Y坐標值輸入
        x.add(new double[] { 10, 30, 50, 70, 90, 100 });
        y.add(new double[] { 1, 0.5, 0.8, 0.22, 0.16, 0.18 });
        XYMultipleSeriesDataset dataset = buildDatset(titles, x, y); // 儲存座標值

        // Line Render
        int[] colors = new int[] { Color.argb(255,51,102,0) };// 折線的顏色
        //PointStyle[] styles = new PointStyle[] { PointStyle.CIRCLE, PointStyle.DIAMOND }; // 折線點的形狀
        XYMultipleSeriesRenderer renderer = buildRenderer(colors, true);

        // Chart Render
        setChartSettings(renderer, "Time", "", 0, 100, -1, 1);// 定義折線圖
        View chart = ChartFactory.getLineChartView(ShowTrainingData.this, dataset, renderer);

        // Add View
        chart_layout.addView(chart);

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    // 定義折線圖名稱
    protected void setChartSettings(XYMultipleSeriesRenderer renderer, String xTitle,
                                    String yTitle, double xMin, double xMax, double yMin, double yMax) {
        //renderer.setChartTitle(title); // 折線圖名稱
        //renderer.setChartTitleTextSize(24); // 折線圖名稱字形大小

        renderer.setXTitle(xTitle); // X軸名稱
        renderer.setXAxisMin(xMin); // X軸顯示最小值
        renderer.setXAxisMax(xMax); // X軸顯示最大值
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
        renderer.setBackgroundColor(Color.argb(255,238,238,238));
        renderer.setShowGrid(true); // 設定格線
        renderer.setShowLegend(false);
        renderer.setMargins(new int[]{25, 50, 0, 50});

        renderer.setZoomEnabled(true, false);
        renderer.setPanEnabled(true, false);

        //限制Scrolling的範圍
        double[] Limits={xMin,xMax,yMin,yMax}; // [panMinimumX, panMaximumX, panMinimumY, panMaximumY]
        renderer.setPanLimits(Limits);
        renderer.setZoomLimits(Limits);

    }

    // 定義折線圖的格式
    private XYMultipleSeriesRenderer buildRenderer(int[] colors, /*PointStyle[] styles,*/ boolean fill) {
            XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        int length = colors.length;
        for (int i = 0; i < length; i++) {
            XYSeriesRenderer r = new XYSeriesRenderer();
            r.setColor(colors[i]);
            //r.setPointStyle(styles[i]);
            r.setFillPoints(fill);
            renderer.addSeriesRenderer(r); //將座標變成線加入圖中顯示
        }
        return renderer;
    }

    // 資料處理
    private XYMultipleSeriesDataset buildDatset(String[] titles, List<double[]> xValues,
                                                List<double[]> yValues) {
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

        int length = titles.length; // 折線數量
        for (int i = 0; i < length; i++) {
            // XYseries對象,用於提供繪製的點集合的資料
            XYSeries series = new XYSeries(titles[i]); // 依據每條線的名稱新增
            double[] xV = xValues.get(i); // 獲取第i條線的資料
            double[] yV = yValues.get(i);
            int seriesLength = xV.length; // 有幾個點

            for (int k = 0; k < seriesLength; k++) // 每條線裡有幾個點
                series.add(xV[k], yV[k]);
            dataset.addSeries(series);
        }
        return dataset;
    }
}
