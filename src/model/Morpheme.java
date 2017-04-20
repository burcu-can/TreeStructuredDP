package model;

public class Morpheme {

	public String morpheme;
	public int freq;
	public double probLength;

	public Morpheme(String morpheme, double gamma){
		
		this.morpheme=morpheme;
		this.freq=1;
		this.setProbLength(calBaseDistribution(gamma));
	}

	public String getMorpheme() {
		return morpheme;
	}

	public void setMorpheme(String morpheme, char type, double gammas, double gammam) {
		this.morpheme = morpheme;
		if(type=='s')
			this.setProbLength(calBaseDistribution(gammas));
		else
			this.setProbLength(calBaseDistribution(gammam));
	}

	public int getFreq() {
		return freq;
	}

	public void setFreq(int freq) {
		this.freq = freq;
	}

	
	public double getProbLength() {
		return probLength;
	}

	public void setProbLength(double probLength) {
		this.probLength = probLength;
	}
	
	public void IncreaseFreq(int f){
		this.freq += f;
	}
	
	public void DecreaseFreq(int f){
		this.freq -= f;
	}
	
	public double calBaseDistribution(double gamma){
		
		return Math.pow(gamma, this.morpheme.length()+1);
	}
}
