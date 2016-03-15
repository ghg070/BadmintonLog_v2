package nctu.nol.algo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Vector;

import nctu.nol.file.LogFileWriter;

import android.util.Log;


public class FrequencyBandModel {
	private static final String TAG = FrequencyBandModel.class.getName();
	
	//Find Spectrum Peak
	private static final int PEAKFREQ_NUM = 5;
	private static final float PPEAKFREQ_POWERTHRESHOLD = 10;
	private static final int PEAKFREQ_DELETENEIGHBOR_NUM = 5;
	
	//Training Setting
	private static final int WINDOW_NUM = 5;
	private static final int MAINFREQ_NUM = 5;
	private List<HashMap.Entry<Float, Float>> TopNMainFreqBandTable = null;
	private float FreqPowerMin = 0, FreqPowerMax = 0;
	public boolean ModelHasTrained = false; 
	
	
	public void SetMainFreqBandTableWithTrainingDataset(final List<Integer> pos, final float[] dataset, int fft_length, int SamplingFreq){
		//fft_length need to be 2's power
		CountSpectrum fft_algo = new CountSpectrum(fft_length);
		
		//Vector<Vector<FreqBand>> AllSpectrums = new Vector<Vector<FreqBand>>();
		Vector<MainFreqInOneWindow> AllSpectrumMainFreqs = new Vector<MainFreqInOneWindow>();
		
		
		for(int i = 0; i < pos.size(); i++){
			int peak = pos.get(i);
			
			//check if the window exceed the dataset size 
			if( dataset.length < peak + WINDOW_NUM*fft_length )
				break;
			
			for(int j = 0; j < WINDOW_NUM; j++){
				double x[] = new double[fft_length]; //real
				double y[] = new double[fft_length]; //imag
				
				//get window data
				int curPos = peak + j*fft_length;
				for(int k = 0; k < fft_length; k++){
					x[k] = dataset[curPos+k];
					//x[k] = Math.sin(2*Math.PI*50*(1/(double)SamplingFreq)*j)+Math.sin(2*Math.PI*120*(1/(double)SamplingFreq)*j);
					y[k] = 0;
				}
				
				//use fft
				fft_algo.fft(x, y);
				
				//store spectrum
				Vector<FreqBand> Spectrum = new Vector<FreqBand>();
				for(int k = 0; k < (int)(fft_length/2); k++){
					float freq = (SamplingFreq/(float)fft_length)*k;
					float power = (float)Math.sqrt(Math.pow(x[k], 2)+Math.pow(y[k], 2));
					FreqBand fb = new FreqBand(freq,power);
					Spectrum.add(fb);
				}
				
				//peak detection(Freq Band)
				List<Integer> peaks = FindSpectrumPeakIndex(Spectrum, PEAKFREQ_NUM, PPEAKFREQ_POWERTHRESHOLD);
				if( !peaks.isEmpty() ){
					MainFreqInOneWindow mf = new MainFreqInOneWindow(i,j, peaks.size());
					for(int k = 0; k < peaks.size(); k++){
						mf.freqbands[k].Freq = Spectrum.get(peaks.get(k)).Freq;
						mf.freqbands[k].Power = Spectrum.get(peaks.get(k)).Power;
					}
					AllSpectrumMainFreqs.add(mf);
				}
			}

		}
		
		//LogFileWriter MainFreqTestWriter = new LogFileWriter("MainFreqTable.txt", LogFileWriter.OTHER_TYPE, LogFileWriter.TRAINING_TYPE);
		HashMap<Float, Float> MainFreqMap = new HashMap<Float, Float>();
		for(int i = 0 ; i < AllSpectrumMainFreqs.size(); i++){
			MainFreqInOneWindow mf = AllSpectrumMainFreqs.get(i);
			
			//Check main frequency band of each window
			for(int j = 0; j < mf.freqbands.length; j++){
				float FreqBand = mf.freqbands[j].Freq;
				
				//Initial value of new key(freq band)
				if(!MainFreqMap.containsKey(FreqBand))
					MainFreqMap.put(FreqBand, (float)0);	
				
				//Sum power with same freq bands
				MainFreqMap.put(FreqBand, MainFreqMap.get(FreqBand) + mf.freqbands[j].Power);
			}
			
			//Print Log File
			/*float [] sortedFreq = new float[mf.freqbands.length];
			for(int j = 0; j < mf.freqbands.length; j++)
				sortedFreq[j] = mf.freqbands[j].Freq;
			
			try {
				MainFreqTestWriter.writeFreqPeakIndexFile(mf.peak_num+1, mf.window_num+1, sortedFreq);
			} catch (IOException e) {
				Log.e(TAG,e.getMessage());
			}*/
		}
		//MainFreqTestWriter.closefile();
		
		//Find Top N Max Freq Band(Key)
		TopNMainFreqBandTable = FindNGreatest(MainFreqMap, MAINFREQ_NUM);
		
		//Average Value
		for(int i = 0; i < TopNMainFreqBandTable.size(); i++){
			HashMap.Entry<Float, Float> entry = TopNMainFreqBandTable.get(i);
		    entry.setValue(entry.getValue()/(pos.size()*WINDOW_NUM));
		    
		    Log.d(TAG,"key:"+entry.getKey()+" val:"+entry.getValue());
		    
		}
			
		//Record Min and Max Power for Top N Freq
		if( TopNMainFreqBandTable.size() != 0 ){
			FreqPowerMin = TopNMainFreqBandTable.get(0).getValue();
			FreqPowerMax = TopNMainFreqBandTable.get(TopNMainFreqBandTable.size()-1).getValue();
			ModelHasTrained = true;
			
			//Print Log File
			LogFileWriter MainFreqPowerWriter = new LogFileWriter("MainFreqPower.txt", LogFileWriter.OTHER_TYPE, LogFileWriter.TRAINING_TYPE);
			for(int i = 0; i < TopNMainFreqBandTable.size(); i++){
				HashMap.Entry<Float, Float> entry = TopNMainFreqBandTable.get(i);
				float freq = entry.getKey();
			    float val = entry.getValue();
			    try {
			    	MainFreqPowerWriter.writeMainFreqPower(freq, val);
				} catch (IOException e) {
					Log.e(TAG,e.getMessage());
				}
			}
			MainFreqPowerWriter.closefile();
		}
	}
	
	private List<Integer> FindSpectrumPeakIndex(final Vector<FreqBand> spectrum, final int PeakNum, final float Threshold){
		List<Integer> result = new ArrayList<Integer>();
		Vector<FreqBand> s = new Vector<FreqBand>();
		for(int i = 0; i < spectrum.size(); i++){//copy
			FreqBand fb = new FreqBand(spectrum.get(i).Freq, spectrum.get(i).Power);
			s.add(fb);
		}
		
		for(int i = 0; i < PeakNum; i++){
			float max = Float.MIN_VALUE;
			int maxIndex = -1;
			//find max
			for(int j = 0; j < s.size(); j++){
				final FreqBand fb = s.get(j);
				if(fb.Power > max && fb.Power > Threshold){
					max = fb.Power;
					maxIndex = j;
				}
			}
			
			if(maxIndex == -1) //找不到擊球聲音的主要頻帶了
				break;
			else{
				result.add(maxIndex);
				//將最大值附近的頻帶歸零, 下一次搜尋最大值時可以順利找到下一個頻帶
				for(int j = maxIndex-PEAKFREQ_DELETENEIGHBOR_NUM; j <= maxIndex+PEAKFREQ_DELETENEIGHBOR_NUM; j++){
					if(j < 0 || j >= s.size()) 
						continue;
					
					s.get(j).Power = 0;
				}
			}
		}
		return result;
	}
	
	
	private final <K, V extends Comparable<? super V>> List<Entry<K, V>> FindNGreatest(HashMap<K, V> map, int n){
		Comparator<? super Entry<K, V>> comparator = new Comparator<Entry<K, V>>(){
			@Override
	        public int compare(Entry<K, V> e0, Entry<K, V> e1){
	            V v0 = e0.getValue();
	            V v1 = e1.getValue();
	            return v0.compareTo(v1);
			}
		};
		
	    PriorityQueue<Entry<K, V>> highest = new PriorityQueue<Entry<K,V>>(n, comparator);
	    for (Entry<K, V> entry : map.entrySet()){
	        highest.offer(entry);
	        while (highest.size() > n)
	            highest.poll();
	    }
	
	    List<Entry<K, V>> result = new ArrayList<HashMap.Entry<K,V>>();
	    while (highest.size() > 0)
	        result.add(highest.poll());
	    return result;
	}
	
	public class MainFreqInOneWindow{
		public int peak_num;
		public int window_num;
		public FreqBand[] freqbands;
		
		public MainFreqInOneWindow(int peak_index, int window_index, int Freq_Size){
			this.peak_num = peak_index;
			this.window_num = window_index;
			this.freqbands = new FreqBand[Freq_Size];
			for(int i = 0; i < Freq_Size; i++)
				this.freqbands[i] = new FreqBand(0,0);
		}
	}
	
	public class FreqBand {
		public float Freq;
		public float Power;
		
		public FreqBand(float freq, float power){
			this.Freq = freq;
			this.Power = power;
		}
	};

};




