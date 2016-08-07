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
	public static final int FFT_LENGTH = 512;
	
	//Find Spectrum Peak
	public static final int PEAKFREQ_NUM = 5;
	private static final int PEAKFREQ_DELETENEIGHBOR_NUM = 5;
	
	//Training Setting
	public static final int WINDOW_NUM = 5;
	private static final int MAINFREQ_NUM = 5;
	private List<HashMap.Entry<Float, Float>> TopKMainFreqBandTable = null;
	private float FreqPowerMin = 0, FreqPowerMax = 0;
	private boolean ModelHasTrained = false;
	
	
	public final Vector<MainFreqInOneWindow> FindSpectrumMainFreqs(final List<Integer> pos, final float[] dataset, int SamplingFreq) {
		//fft_length need to be 2's power
		CountSpectrum fft_algo = new CountSpectrum(FFT_LENGTH);

		//所有頻譜的主頻皆會存在AllSpectrumMainFreqs
		Vector<MainFreqInOneWindow> AllSpectrumMainFreqs = new Vector<MainFreqInOneWindow>();

		//Trace all window position
		for (int i = 0; i < pos.size(); i++) {
			int wIndex = pos.get(i);

			//check if the window exceed the dataset size 
			if (dataset.length < wIndex + WINDOW_NUM * FFT_LENGTH)
				break;

			// 往後取WINDOW_NUM個window進行傅利葉分析
			for (int j = 0; j < WINDOW_NUM; j++) {
				int curPos = wIndex + j * FFT_LENGTH;
				final Vector<FreqBand> Spectrum =  getSpectrum(fft_algo, curPos, dataset, SamplingFreq);

				//Find 5-max frequency band in a spectrum, store in AllSpectrumMainFreqs
				List<Integer> mainfreqs = FindSpectrumPeakIndex(Spectrum, PEAKFREQ_NUM);
				if (!mainfreqs.isEmpty()) {
					MainFreqInOneWindow mf = new MainFreqInOneWindow(i+1, j+1, mainfreqs.size()); //i and j start from 0
					for (int k = 0; k < mainfreqs.size(); k++) {
						mf.freqbands[k].Freq = Spectrum.get(mainfreqs.get(k)).Freq;
						mf.freqbands[k].Power = Spectrum.get(mainfreqs.get(k)).Power;
					}
					AllSpectrumMainFreqs.add(mf);
				}
			}
		}
		return AllSpectrumMainFreqs;
	}

	public void setTopKFreqBandTable(final Vector<MainFreqInOneWindow> AllSpectrumMainFreqs, int training_stroke_num){
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
		}

		//Find Top K Max Freq Band(Key)
		TopKMainFreqBandTable = FindNGreatest(MainFreqMap, MAINFREQ_NUM);
		
		//Average Value
		for(int i = 0; i < TopKMainFreqBandTable.size(); i++){
			HashMap.Entry<Float, Float> entry = TopKMainFreqBandTable.get(i);
		    entry.setValue(entry.getValue()/(training_stroke_num*WINDOW_NUM));
		    
		    Log.d(TAG,"key:"+entry.getKey()+" val:"+entry.getValue());

		}

		//Record Min and Max Power for Top N Freq
		if( TopKMainFreqBandTable.size() != 0 ){
			FreqPowerMin = TopKMainFreqBandTable.get(0).getValue();
			FreqPowerMax = TopKMainFreqBandTable.get(TopKMainFreqBandTable.size()-1).getValue();
			ModelHasTrained = true;
		}
	}

	public final Vector<FreqBand> getSpectrum(final CountSpectrum fft_algo, int Position, final float[] dataset, int SamplingFreq){
		double x[] = new double[FFT_LENGTH]; //real
		double y[] = new double[FFT_LENGTH]; //imag

		//Get window data
		for (int k = 0; k < FFT_LENGTH; k++) {
			x[k] = dataset[Position + k];
			y[k] = 0;
		}

		//Use fft
		fft_algo.fft(x, y);

		//Store spectrum
		Vector<FreqBand> Spectrum = new Vector<FreqBand>();
		for (int k = 0; k < FFT_LENGTH / 2; k++) {
			float freq = (SamplingFreq / (float) FFT_LENGTH) * k;
			float power = (float) Math.sqrt(Math.pow(x[k], 2) + Math.pow(y[k], 2));
			FreqBand fb = new FreqBand(freq, power);
			Spectrum.add(fb);
		}

		return Spectrum;
	}


	public final List<HashMap.Entry<Float, Float>> getTopKMainFreqBandTable(){
		return TopKMainFreqBandTable;
	}

	public boolean CheckModelHasTrained(){
		return ModelHasTrained;
	}

	public List<Integer> FindSpectrumPeakIndex(final Vector<FreqBand> spectrum, final int PeakNum){
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
				if(fb.Power > max){
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
		public int stroke_num;
		public int window_num;
		public FreqBand[] freqbands;
		
		public MainFreqInOneWindow(int stroke_index, int window_index, int Freq_Size){
			this.stroke_num = stroke_index;
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
