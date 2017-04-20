package io;

import java.util.StringTokenizer;

import model.Word;


public class Tokenizer {

	public Word TokenizeWordListLine(String line, double gammas, double gammam, int wordFreq){
		StringTokenizer st = new StringTokenizer(line);
		int freq = Integer.valueOf(st.nextToken());
		String word = st.nextToken();
		if(freq>=wordFreq && word.length()>2)
			return new Word(word, freq, gammas, gammam);
		else return null;
	}
}
