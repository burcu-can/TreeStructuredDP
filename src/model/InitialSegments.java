package model;

public class InitialSegments {

	int treeId;
	String stem, suffix;
	double prob;
	
	public InitialSegments(int id,String st, String suf, double p){
		
		this.treeId	= id;
		this.stem		= st;
		this.suffix 	= suf;
		this.prob		= p;
	}

	public int getTreeId() {
		return treeId;
	}

	public void setTreeId(int treeId) {
		this.treeId = treeId;
	}

	public String getStem() {
		return stem;
	}

	public void setStem(String stem) {
		this.stem = stem;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public double getProb() {
		return prob;
	}

	public void setProb(double prob) {
		this.prob = prob;
	}
}
