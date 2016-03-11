package nctu.nol.algo;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;


public class PeakDetector {
	private static final String TAG = "PeakDetector";
	private float WINDOWSIZE_IN_MILLISECOND;
	private float PEAKWINDOWSIZE_IN_MILLISECOND;
	
	//Constructor
	public PeakDetector(float w_size, float p_size){
		this.WINDOWSIZE_IN_MILLISECOND = w_size;
		this.PEAKWINDOWSIZE_IN_MILLISECOND = p_size;
	}
	
	//Check the rule
	private boolean IsPeak(final float []vals, int index, float threshold){
		if(index < 1 || index > vals.length-2)
			return false;	
		
		float prev = vals[index-1], 
			  cur = vals[index], 
			  next = vals[index+1];
		
		//確認是否符合規則 ( 旁邊兩點比自己小, 且大於Threshold )
		if( cur > threshold && cur > prev && cur > next)
			return true;
		else
			return false;
	}
	
	//Check if there is another higher peak in Peak Window
	private int CheckNeighborPeak(final float[] attrs, final float[] vals, final int curPeakIndex ){
		int newPeak = -1;
		float maxVal = vals[curPeakIndex];
		//向左找
		for(int i = curPeakIndex-1; attrs[curPeakIndex]-attrs[i] <= PEAKWINDOWSIZE_IN_MILLISECOND && i > 0; i--){
			if( IsPeak(vals, i, maxVal) ){
				newPeak = i;
				maxVal = vals[newPeak];
			}
		}
		
		//向右找
		for(int i = curPeakIndex+1; attrs[i]-attrs[curPeakIndex] <= PEAKWINDOWSIZE_IN_MILLISECOND && i < vals.length-1; i++){
			if( IsPeak(vals, i, maxVal) ){
				newPeak = i;
				maxVal = vals[newPeak];
			}
		}
		
		//Recursive Check
		if(newPeak != -1){
			int temp = CheckNeighborPeak(attrs, vals, newPeak);
			if(temp != -1)
				newPeak = temp;
		}
		return newPeak;
	}
	
	public final List<Integer> findPeakIndex(final float[] attrs, final float[] vals, final float Threshold){
		if(attrs.length != vals.length){
			Log.e(TAG,"Time array length is not equal to vals array.");
			return null;
		}
			
		int curPos = 0, endPos = 0;
		List<Integer> peakIndex = new ArrayList<Integer>();
		
		while(endPos < attrs.length){
			//指定此次Window的右端位置
			while( endPos < attrs.length && attrs[endPos]-attrs[curPos] <= WINDOWSIZE_IN_MILLISECOND )
				endPos++;
			
			float maxVal = Float.NEGATIVE_INFINITY;
			int curPeakIndex = -1;
			
			// First Stage: 找旁邊兩點比自己小, 且大於Threshold 和 maxVal 的點
			for(int i = curPos; i < endPos; i++){
				float t = (Threshold > maxVal)? Threshold : maxVal;
				
				//檢查是否符合波峰規則
				if( IsPeak(vals, i, t) ){

					//確認與上一個波峰相隔一個Peak Window以上
					if(peakIndex.size() != 0){
						int prevPeakIndex = peakIndex.get(peakIndex.size()-1);
						if(Math.abs(attrs[i]-attrs[prevPeakIndex]) <= PEAKWINDOWSIZE_IN_MILLISECOND )
							continue;
					}
					maxVal = vals[i];
					curPeakIndex = i;
				}
					
			}
			
			//Second Stage: 該WINDOW內有找到波峰, 確認peak window內是否還有其他更高的波峰(有可能跨Window的情形)
			if(curPeakIndex != -1){
				int temp = CheckNeighborPeak(attrs,vals,curPeakIndex);
				if(temp != -1)
					curPeakIndex = temp;
				
				peakIndex.add(curPeakIndex);
			}
			
			curPos = endPos;
		}	
		return peakIndex;
	}
}
