package model;

public class TestWord {
	
	public String stem;
	public String suffix;
	public double logprob = 0;
	

	public TestWord(String stem, String suffix) {
		super();
		this.stem = stem;
		this.suffix = suffix;
	}
	
	public void increaseLogProb(double logProb){
		this.logprob += logProb;
	}
}
