package model;

import driver.Main;
import various.RandomGenerator;

public class Word {

	public String word;
	public Morpheme stem;
	public Morpheme suffix;
	public int freq;
	
	public Word(String word, double gammas, double gammam) {
		super();
		this.setWord(word);
		this.setStem(word, gammas, gammam);
		this.setSuffix("", gammas, gammam);
		this.setFreq(1);
	}
	
	public Word(String word,int freq, double gammas, double gammam) {
		super();
		this.setWord(word);
		this.stem = new Morpheme(word, gammas);
		this.suffix = new Morpheme("", gammam);
		this.setFreq(freq);
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getWord() {
		return word;
	}

	public void setStem(String stem, double gammas, double gammam) {
		this.stem.setMorpheme(stem, 's', gammas, gammam);
	}

	public Morpheme getStem() {
		return stem;
	}

	public void setSuffix(String suffix, double gammas, double gammam) {
		this.suffix.setMorpheme(suffix, 'm', gammas, gammam);
	}

	public Morpheme getSuffix() {
		return suffix;
	}

	public void setFreq(int freq) {
		this.freq = freq;
	}

	public int getFreq() {
		return freq;
	}
	
	public void RandomlySplit(RandomGenerator randGenerator, Main driver){
		
		int wordLength = this.word.length();
		
		if(this.getWord().length() > 2){
			int suffixSize = randGenerator.GeneratePosIntSmallerThan(wordLength-2);
			this.setSuffix(this.ReturnWordsLastNChars(suffixSize), driver.getGammas(), driver.getGammam());
			this.setStem(this.ReturnWordsInitialNChars(wordLength-suffixSize), driver.getGammas(), driver.getGammam());
		}
	}
	
	/**
	 * 
	 * @param n
	 * @return
	 */
	public String ReturnWordsLastNChars(int n){
		int wordLength = this.word.length();
		if(n<=wordLength)
			return this.word.substring(wordLength-n, wordLength);
		else
			return null;
	}
	
	/**
	 * 
	 * @param n
	 * @return
	 */
	public String ReturnWordsInitialNChars(int n){
		int wordLength = this.word.length();
		if(n<=wordLength)
			return word.substring(0, n);
		else
			return null;
	}
}
