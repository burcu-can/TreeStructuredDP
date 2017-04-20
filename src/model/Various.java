package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import driver.Main;

import various.RandomGenerator;

public class Various {

public static double CalculateStemProbGivenGlobalSet(String stem, Main driver){
		
		double stemProb;
		
		if(driver.getGlobalStems().containsKey(stem)){ //if the stem exists in the global set.
			
			int fsk = driver.getGlobalStems().get(stem).getFreq(); //the number of stem tables serving dish stem k.
			stemProb = (double)fsk/(driver.getFs()+driver.getBetas()); //degistir gammas
		}
		else //otherwise
			stemProb = (driver.getBetas()*calBaseDistribution(stem, driver.getGammas()))/(driver.getFs()+driver.getBetas()); //degistir gammas
		
		return stemProb;
	}

public static double CalculateStemProbGivenGlobalSet_test(String stem, Main driver){
	
	double stemProb;
	
	if(driver.getGlobalStems().containsKey(stem)){ //if the stem exists in the global set.
		
		int fsk = driver.getGlobalStems().get(stem).getFreq(); //the number of stem tables serving dish stem k.
		stemProb = (double)fsk/(driver.getFs()+driver.getBetas()); //degistir gammas
	}
	else //otherwise
		stemProb = (driver.getBetas())/(driver.getFs()+driver.getBetas()); //degistir gammas
	
	return stemProb;
}
	
	public static double CalculateSuffixProbGivenGlobalSet(String suffix, Main driver){
		
		double suffixProb;
		
		if(driver.getGlobalSuffixes().containsKey(suffix)){ //if the stem exists in the global set.
			
			int fmk = driver.getGlobalSuffixes().get(suffix).getFreq(); //the number of stem tables serving dish stem k.
			suffixProb = (double)fmk/(driver.getFm()+driver.getBetam()); //degistir gammas
		}
		else //otherwise
			suffixProb = (driver.getBetam()*calBaseDistribution(suffix, driver.getGammam()))/(driver.getFm()+driver.getBetam()); //degistir gammas
		
		return suffixProb;
	}
	
	public static double CalculateSuffixProbGivenGlobalSet_test(String suffix, Main driver){
		
		double suffixProb;
		
		if(driver.getGlobalSuffixes().containsKey(suffix)){ //if the stem exists in the global set.
			
			int fmk = driver.getGlobalSuffixes().get(suffix).getFreq(); //the number of stem tables serving dish stem k.
			suffixProb = (double)fmk/(driver.getFm()+driver.getBetam()); //degistir gammas
		}
		else //otherwise
			suffixProb = (driver.getBetam())/(driver.getFm()+driver.getBetam()); //degistir gammas
		
		return suffixProb;
	}
	
	public static double calBaseDistribution(String morpheme, double gamma){
		return Math.pow(gamma, morpheme.length()+1);
	}
	
	public static int NormaliseNSampleFromArray(ArrayList<InitialSegments> allSegments, double sum, RandomGenerator randomGenerator){
		
		double random = randomGenerator.GenerateDouble();
		InitialSegments nextSample = allSegments.get(0);
		nextSample.prob /= sum;
		if(random < nextSample.prob)
			return 0;
		int i=1; 
		for( ; i<allSegments.size(); i++){
			nextSample = allSegments.get(i);
			nextSample.prob /=sum;
			nextSample.prob += allSegments.get(i-1).prob;
			if(random < nextSample.prob)
				return i;
		}
		return i;
	}
	
	public static int NormaliseNPickMax(ArrayList<Double> probabilities, double sum){
		
		double max=0;
		int maxIndex=0;
		
		for(int i=0; i<probabilities.size(); i++){
			if(probabilities.get(i)/sum > max){
				max=probabilities.get(i)/sum;
				maxIndex=i;
			}
		}
		return maxIndex;
	}
	
	public static void MergeTwoMaps(HashMap<Integer, Integer> map1, HashMap<Integer, Integer> map2){
		
		Iterator<Map.Entry<Integer, Integer>> entries = map1.entrySet().iterator();
		Integer freq1, freq2;
		
		while(entries.hasNext())
		{
			Map.Entry<Integer, Integer> entry = entries.next();
			freq1 = entry.getValue();
			if((freq2 = map2.get(entry.getKey()))!=null)
				map2.put(entry.getKey(), freq1+freq2);
			else
				map2.put(entry.getKey(), freq1);
		}
	}
	
	public static void addToHashtable(String str, Hashtable<String, Morpheme> table, double gamma){
		
		if(table.containsKey(str))
			table.get(str).IncreaseFreq(1);
		else
			table.put(str, new Morpheme(str, gamma));
	}

	public static void removeFromHashtable(String str, Hashtable<String, Morpheme> table){
	
		Morpheme mor = table.get(str);
		if(mor.getFreq()==1)
			table.remove(str);
		else
			mor.DecreaseFreq(1);
	}
}
