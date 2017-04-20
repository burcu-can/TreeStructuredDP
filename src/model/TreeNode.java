package model;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import driver.Main;


public class TreeNode {

	private TreeNode parent;	
	private TreeNode leftNode;
	private TreeNode rightNode;
		
	private HashMap<String, Morpheme> stemSet;
	private HashMap<String, Morpheme> suffixSet;
	private ArrayList<Word> wordSet; // words
	
	private double logDkH1; 
	private double logDkTk; //piK*p(Dk|H1)+(1-piK)*p(Di|Ti)*p(Dj|Tj)
	
	private static int idCounter=0;
	private int id;

	
	public TreeNode() {
		super();
		this.stemSet	= new HashMap<String, Morpheme>();
		this.suffixSet	= new HashMap<String, Morpheme>();
		this.wordSet = new ArrayList<Word>();
		this.leftNode = null;
		this.rightNode = null;
		this.parent = null;
		this.setId(TreeNode.idCounter+1);
		TreeNode.idCounter++; 
	}
	
	public TreeNode(Word word, double gammas, double gammam){
		this.setStemSet(new HashMap<String, Morpheme>());
		this.setSuffixSet(new HashMap<String, Morpheme>());
		this.setWordSet(new ArrayList<Word>());
		this.setLeftNode(null);
		this.setRightNode(null);
		this.setParent(null);
		this.addWordToNode(word, gammas, gammam);
		this.setId(TreeNode.idCounter+1);
		TreeNode.idCounter++;
	}
	
	
	/***************************************************************************/
	/***************************************************************************/
	/***************************GETTERS & SETTERS ******************************/
	/***************************************************************************/
	/***************************************************************************/
	
	public TreeNode getParent() {
		return parent;
	}

	public void setParent(TreeNode parent) {
		this.parent = parent;
	}

	public TreeNode getLeftNode() {
		return leftNode;
	}

	public void setLeftNode(TreeNode leftNode) {
		this.leftNode = leftNode;
	}

	public TreeNode getRightNode() {
		return rightNode;
	}

	public void setRightNode(TreeNode rightNode) {
		this.rightNode = rightNode;
	}

	public HashMap<String, Morpheme> getStemSet() {
		return stemSet;
	}

	public void setStemSet(HashMap<String, Morpheme> stemSet) {
		this.stemSet = stemSet;
	}

	public HashMap<String, Morpheme> getSuffixSet() {
		return suffixSet;
	}

	public void setSuffixSet(HashMap<String, Morpheme> suffixSet) {
		this.suffixSet = suffixSet;
	}

	public ArrayList<Word> getWordSet() {
		return wordSet;
	}

	public void setWordSet(ArrayList<Word> dk) {
		wordSet = dk;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public double getLogDkH1() {
		return logDkH1;
	}

	public void setLogDkH1(double logDkH1) {
		this.logDkH1 = logDkH1;
	}

	public double getLogDkTk() {
		return logDkTk;
	}

	public void setLogDkTk(double logDkTk) {
		this.logDkTk = logDkTk;
	}


	/***************************************************************************/
	
	public void Clear(){
		this.getStemSet().clear();
		this.getSuffixSet().clear();
		this.getWordSet().clear();
		this.leftNode = null;
		this.rightNode = null;
		this.parent = null;
	//	this.logDkTk = 0;
	//	this.logDkH1 = 0;
	}

	public TreeNode FindSibling(){
		
		if(this.getParent()!=null){
			if(this.getParent().getLeftNode().getId()==this.getId())
				return this.getParent().getRightNode();
			else
				return this.getParent().getLeftNode();
		}
		return null;
	}
	
	/***************************************************************************/
	/***************************************************************************/
	/*************ADDING & REMOVING WORDS - TO/FROM THE TREE NODE **************/
	/***************************************************************************/
	/***************************************************************************/
	
	public void addWordToNode(Word newWord, double gammas, double gammam){
		
		this.addStemToNode(newWord.getStem().getMorpheme(), gammas);
		this.addSuffixToNode(newWord.getSuffix().getMorpheme(), gammam);
		
		this.getWordSet().add(newWord);
	}
	
	public void addStemToNode(String stem, double gamma){
				
		if(this.stemSet.containsKey(stem)){			
			Morpheme existingMorpheme = this.stemSet.get(stem);
			existingMorpheme.IncreaseFreq(1);
		}
		else{
			Morpheme newStem = new Morpheme(stem, gamma);
			this.stemSet.put(stem, newStem);
		}
	}
	
	public void addSuffixToNode(String suffix, double gamma){
		
		if(this.suffixSet.containsKey(suffix)){			
			Morpheme existingMorpheme = this.suffixSet.get(suffix);
			existingMorpheme.IncreaseFreq(1);
		}
		else{
			Morpheme newSuffix = new Morpheme(suffix, gamma);
			this.suffixSet.put(suffix, newSuffix);
		}
	}
	
	public void removeWordFromNode(Word word){

		//Remove stem
		removeStemFromNode(word.getStem().getMorpheme());
		//Remove suffix
		removeSuffixFromNode(word.getSuffix().getMorpheme());
		
		this.getWordSet().remove(word);
	}
	
	public void removeStemFromNode(String stemToRemove){
		
		Morpheme stem = this.getStemSet().get(stemToRemove);
		stem.setFreq(stem.getFreq()-1);
		if(stem.freq == 0)
			this.getStemSet().remove(stemToRemove);
	}

	public void removeSuffixFromNode(String suffixToRemove){
		
		Morpheme suffix = this.getSuffixSet().get(suffixToRemove);
		suffix.setFreq(suffix.getFreq()-1);
		if(suffix.freq == 0)
			this.getSuffixSet().remove(suffixToRemove);
	}
	
	public void AddNodesContents(TreeNode node, double gammas, double gammam){
		
		int nodeDkSize = node.getWordSet().size();
		
		for(int i=0; i<nodeDkSize; i++){
			Word nextWord = node.getWordSet().get(i);
			this.addWordToNode(nextWord, gammas, gammam);
		}
	}
	
	/***************************************************************************/
	/***************************************************************************/
	/**************************COMPUTING PROBABILITIES**************************/
	/***************************************************************************/
	/***************************************************************************/
	
	public double calculatePDkGivenH1(Main driver){
		
		double logProbStemSeq	= ComputeLogStemSequence(this.getStemSet(), driver);
		if(logProbStemSeq==0 && this.getParent()!=null)
			logProbStemSeq		+= Math.log(this.getStemSet().entrySet().iterator().next().getValue().getProbLength());
		
		double logProbSuffixSeq	= ComputeLogSuffixSequence(this.getSuffixSet(), driver);
		if(logProbSuffixSeq==0 && this.getParent()!=null)
			logProbSuffixSeq	+= Math.log(this.getSuffixSet().entrySet().iterator().next().getValue().getProbLength());
		
		return logProbStemSeq+logProbSuffixSeq;
	}
	
	public double ComputeLogStemSequence(HashMap<String, Morpheme> morphemes, Main driver){
		
		if(this.getWordSet().size()==1)
			return 0;
				
		double logDenominators = jsc.util.Maths.logGamma(driver.getBetas())-jsc.util.Maths.logGamma(this.getWordSet().size()+driver.getBetas());
		double logNewTableS	= (morphemes.size())*Math.log(driver.getBetas());
		
		double logExistingTableS	= 0;
		
		Iterator<Morpheme> it = morphemes.values().iterator();
		while(it.hasNext()){
			Morpheme nextMorpheme = it.next();
			logExistingTableS	+= jsc.util.Maths.logGamma(nextMorpheme.getFreq()); //log(nk-1)!
			if(this.getParent()!=null)
				logNewTableS		+= Math.log(nextMorpheme.getProbLength());
		}
		
		return logDenominators+logNewTableS+logExistingTableS;
	}
	
	public double ComputeLogSuffixSequence(HashMap<String, Morpheme> morphemes, Main driver){
		
		if(this.getWordSet().size()==1)
			return 0;
				
		double logDenominators = jsc.util.Maths.logGamma(driver.getBetam())-jsc.util.Maths.logGamma(this.getWordSet().size()+driver.getBetam());
		double logNewTableM		= (morphemes.size())*Math.log(driver.getBetam());
		
		double logExistingTableM	= 0;
		
		Iterator<Morpheme> it = morphemes.values().iterator();
		while(it.hasNext()){
			Morpheme nextMorpheme = it.next();
			logExistingTableM	+= jsc.util.Maths.logGamma(nextMorpheme.getFreq()); //log(nk-1)!
			if(this.getParent()!=null)
				logNewTableM		+= Math.log(nextMorpheme.getProbLength());
		}
		
		return logDenominators+logNewTableM+logExistingTableM;
	}
	
	
	/*****ROOT P(DK|H1) UPDATES - end*****/
	
	public void UpdateNodeProbabilities(Word word, int added, Main driver, boolean root){
					
			if(added == -1){ //one word is removed..
				this.DecreaseDkH1AfterRemoving_stem(word.getStem(), driver, root);
				this.DecreaseDkH1AfterRemoving_suffix(word.getSuffix(), driver, root);
			}
			else if(added == 1){ //one word is added..
				this.IncreaseDkH1AfterAdding_stem(word.getStem(), driver, root);
				this.IncreaseDkH1AfterAdding_suffix(word.getSuffix(), driver, root);
			}
			else //only the segmentation of the word has changed..
				this.setLogDkH1(this.calculatePDkGivenH1(driver));
			
			this.setLogDkTk(this.calculateLogPDkGivenTk());
	}
	
	
	public double calBaseDistribution(String str, double gamma){
		
		return Math.pow(gamma, str.length()+1);
	}
	
	public boolean DecreaseDkH1AfterRemoving_stem(Morpheme stem, Main driver, boolean root){
		
		double curLogDkH1 = this.getLogDkH1();
		int wordNumber = this.getWordSet().size();
		Morpheme stemInNode=this.getStemSet().get(stem.morpheme);
		
		//stems
		curLogDkH1 += jsc.util.Maths.logGamma(wordNumber+1+driver.getBetas());
		curLogDkH1 -= jsc.util.Maths.logGamma(wordNumber+driver.getBetas());
		
		if(this.getStemSet().containsKey(stem.getMorpheme())){
			curLogDkH1 -= jsc.util.Maths.logGamma(stemInNode.freq+1);
			curLogDkH1 += jsc.util.Maths.logGamma(stemInNode.freq);
		}
		else{
			if(!root)
				curLogDkH1 -= Math.log(calBaseDistribution(stem.morpheme, driver.getGammas()));
			curLogDkH1 -= Math.log(driver.getBetas());
			this.setLogDkH1(curLogDkH1);
			return true;
		}
		
		this.setLogDkH1(curLogDkH1);
		return false;
	}
	
	public boolean DecreaseDkH1AfterRemoving_suffix(Morpheme suffix, Main driver, boolean root){
		
		double curLogDkH1 = this.getLogDkH1();
		int wordNumber = this.getWordSet().size();
		Morpheme suffixInNode = this.getSuffixSet().get(suffix.morpheme);
		
		//suffixes
		curLogDkH1 += jsc.util.Maths.logGamma(wordNumber+1+driver.getBetam());
		curLogDkH1 -= jsc.util.Maths.logGamma(wordNumber+driver.getBetam());
		
		if(this.getSuffixSet().containsKey(suffix.getMorpheme())){
			curLogDkH1 -= jsc.util.Maths.logGamma(suffixInNode.freq+1);
			curLogDkH1 += jsc.util.Maths.logGamma(suffixInNode.freq);
		}
		else{
			if(!root)
				curLogDkH1 -= Math.log(calBaseDistribution(suffix.morpheme, driver.getGammam()));
			curLogDkH1 -= Math.log(driver.getBetam());
			this.setLogDkH1(curLogDkH1);
			return true;
		}
		
		this.setLogDkH1(curLogDkH1);
		return false;
	}
	
	public boolean IncreaseDkH1AfterAdding_stem(Morpheme stem, Main driver, boolean root){
		
		double curLogDkH1 = this.getLogDkH1();
		int wordNumber = this.getWordSet().size();
		Morpheme stemInNode=this.getStemSet().get(stem.morpheme);
		
		//stems
		curLogDkH1 += jsc.util.Maths.logGamma(wordNumber-1+driver.getBetas());
		curLogDkH1 -= jsc.util.Maths.logGamma(wordNumber+driver.getBetas());
		
		if(stemInNode.freq>1){
			curLogDkH1 -= jsc.util.Maths.logGamma(stemInNode.freq-1);
			curLogDkH1 += jsc.util.Maths.logGamma(stemInNode.freq);
		}
		else{
			if(!root)
				curLogDkH1 += Math.log(calBaseDistribution(stem.morpheme, driver.getGammas()));
			curLogDkH1 += Math.log(driver.getBetas());
			this.setLogDkH1(curLogDkH1);
			return true;
		}				
		this.setLogDkH1(curLogDkH1);
		return false;
	}
	
	public boolean IncreaseDkH1AfterAdding_suffix(Morpheme suffix, Main driver, boolean root){
		
		double curLogDkH1 = this.getLogDkH1();
		int wordNumber = this.getWordSet().size();
		Morpheme suffixInNode = this.getSuffixSet().get(suffix.morpheme);
		
		//suffixes
		curLogDkH1 += jsc.util.Maths.logGamma(wordNumber-1+driver.getBetam());
		curLogDkH1 -= jsc.util.Maths.logGamma(wordNumber+driver.getBetam());
		
		if(suffixInNode.freq>1){
			curLogDkH1 -= jsc.util.Maths.logGamma(suffixInNode.freq-1);
			curLogDkH1 += jsc.util.Maths.logGamma(suffixInNode.freq);
		}
		else{
			if(!root)
				curLogDkH1 += Math.log(calBaseDistribution(suffix.morpheme, driver.getGammam()));
			curLogDkH1 += Math.log(driver.getBetam());
			this.setLogDkH1(curLogDkH1);
			return true;
		}
		this.setLogDkH1(curLogDkH1);
		return false;
	}
	
	public void UpdateLeafNodeAllProbabilities(Main driver){
		this.setLogDkH1(this.calculatePDkGivenH1(driver));
		this.setLogDkTk(this.calculateLogPDkGivenTk());
	}
	
	public double calculateLogPDkGivenTk(){
		
		if(this.getLeftNode()!=null)
			return this.getLogDkH1()+this.getLeftNode().getLogDkTk()+this.getRightNode().getLogDkTk();
		else
			return this.getLogDkH1();
	}
	
	public double CalStemGivenRoot(String stem, Main driver){
		
		if(this.getStemSet().containsKey(stem)){
			int nsjt = this.getStemSet().get(stem).getFreq();
			return (double)nsjt/(this.getWordSet().size()+driver.getBetas());
		}
		else{
			double prob = driver.getBetas()/(this.getWordSet().size()+driver.getBetas()); //alpha_s/|S|+alpha_s
			return prob*Various.CalculateStemProbGivenGlobalSet(stem, driver); //look at the very global set..
		}
	}
	
	public double CalStemGivenRoot_test(String stem, Main driver){
		
		if(this.getStemSet().containsKey(stem)){
			int nsjt = this.getStemSet().get(stem).getFreq();
			return (double)nsjt/(this.getWordSet().size()+driver.getBetas());
		}
		else{
			double prob = driver.getBetas()/(this.getWordSet().size()+driver.getBetas()); //alpha_s/|S|+alpha_s
			return prob*Various.CalculateStemProbGivenGlobalSet_test(stem, driver); //look at the very global set..
		}
	}
	
	public double CalSuffixGivenRoot(String suffix, Main driver){
				
		if(this.getSuffixSet().containsKey(suffix)){
			int nmjt = this.getSuffixSet().get(suffix).getFreq();
			return (double)nmjt/(this.getWordSet().size()+driver.getBetam());
		}
		else{
			double prob = driver.getBetam()/(this.getWordSet().size()+driver.getBetam()); //alpha_s/|S|+alpha_s
			return prob*Various.CalculateSuffixProbGivenGlobalSet(suffix, driver); //look at the very global set..
		}
	}
	
	public double CalSuffixGivenRoot_test(String suffix, Main driver){
		
		if(this.getSuffixSet().containsKey(suffix)){
			int nmjt = this.getSuffixSet().get(suffix).getFreq();
			return (double)nmjt/(this.getWordSet().size()+driver.getBetam());
		}
		else{
			double prob = driver.getBetam()/(this.getWordSet().size()+driver.getBetam()); //alpha_s/|S|+alpha_s
			return prob*Various.CalculateSuffixProbGivenGlobalSet_test(suffix, driver); //look at the very global set..
		}
	}
}


