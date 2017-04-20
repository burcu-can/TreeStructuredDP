package driver;

import io.HBC_IO;
import io.ProbType;
import io.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Iterator;
import java.util.Map;

import model.*;
import various.RandomGenerator;
import java.util.Enumeration;

/**
 * 
 * Created by @author burcucan
 * 20/04/2017
 */

public class Main {

	static String dataFile, segmentedFile, goldfile, fullFileresults;
	static int trainingSetSize;
	static ArrayList<Word> corpus = new ArrayList<Word>();
	static ArrayList<TreeNode> leafNodes = new ArrayList<TreeNode>();
	static HashMap<Integer,Tree> allNodesOnWhichTree = new HashMap<Integer,Tree>();
	static HashMap<Integer, Tree> trees = new HashMap<Integer, Tree>();
	
	private Hashtable<String,Morpheme> globalStems = new Hashtable<String,Morpheme>();
	private Hashtable<String,Morpheme> globalSuffixes = new Hashtable<String,Morpheme>();
	private int fs=0,fm=0; //total number of stem and suffix tables in trees that are occupied.
	
	static int WordNumberOnTrees = 0, wordfreq;
	static RandomGenerator randomGenerator = new RandomGenerator(); 
	static HBC_IO io = new HBC_IO();
	static double currentLogDkTk = 0;
	static double temperature = 2;
	static double tempDec;
	static double betas, betam, gammas, gammam, alpha; //alpha is for tree mixture components..
	static ProbType pt = new ProbType();
	static State maxSurvivor=null;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		HBC_IO io = new HBC_IO();	
		dataFile=args[0];
		trainingSetSize = Integer.valueOf(args[1]);
		segmentedFile = args[2];
		tempDec = Double.valueOf(args[3]);
		betas = Double.valueOf(args[4]);
		betam = Double.valueOf(args[5]);
		gammas = Double.valueOf(args[6]);
		gammam = Double.valueOf(args[7]);
		alpha = Double.valueOf(args[8]);
		wordfreq = Integer.valueOf(args[9]);
		goldfile = args[10];
		
		Main driver = new Main();
		System.out.println("Reading corpus..");
		corpus = io.ReadWordList(dataFile, corpus, randomGenerator, trainingSetSize, gammas, gammam, wordfreq, goldfile);
		System.out.println("Creating initial trees..");
		CreateInitialTrees(driver);
		double totalModelsProb = PostOrderUpdateOfAllTrees(driver);
		currentLogDkTk = currentTotalLogDkTk(driver);
		System.out.println("Start sampling..");
 		MetropolisHastings(driver);
 		System.out.println("Inference is done..");
		System.out.println("Start segmenting words..");
 		io.SplitWithViterbi(dataFile, segmentedFile, driver, true); 
		System.out.println("Finished..");
		System.out.println("<output>" + segmentedFile + "</output>");
		io.PrintTrees(trees);
	}
	
	
	public static double PostOrderUpdateOfAllTrees(Main driver){
		
		Iterator<Map.Entry<Integer, Tree>> entries = Main.trees.entrySet().iterator();
		double treeTotalProb = 0;
		Stack<TreeNode> nodeStack=new Stack<TreeNode>();
		
		while(entries.hasNext())
		{
			Map.Entry<Integer, Tree> entry = entries.next();
			Tree nextTree = entry.getValue();
		
			 nodeStack.push(nextTree.getRoot());
			 TreeNode pre = null;
			 
			 while(!nodeStack.empty()){ 
				 
				 TreeNode curr = nodeStack.lastElement();
				 if(pre==null || (pre.getLeftNode()!=null && pre.getLeftNode().getId()==curr.getId()) || (pre.getRightNode()!=null && pre.getRightNode().getId()==curr.getId())){
					 if(curr.getLeftNode()!=null)
						 nodeStack.push(curr.getLeftNode());
					 else if(curr.getRightNode()!=null)
						 nodeStack.push(curr.getRightNode());
				 }else if(curr.getLeftNode()==pre){
					 if(curr.getRightNode()!=null)
						 nodeStack.push(curr.getRightNode());
				 }
				 else{
					 /*******************/
					 
					 double logDkH1 = curr.calculatePDkGivenH1(driver);
					 treeTotalProb += logDkH1; 
						 
					 curr.setLogDkH1(logDkH1);
					 curr.setLogDkTk(curr.calculateLogPDkGivenTk());
						 
					 /*******************/
					 nodeStack.pop();
				 }
				 pre = curr;
			 }
			 nodeStack.clear();
		}
		return treeTotalProb;
	}

	public int getFs() {
		return fs;
	}

	public void setFs(int fs) {
		this.fs = fs;
	}

	public int getFm() {
		return fm;
	}

	public void setFm(int fm) {
		this.fm = fm;
	}
	
	public double getCurrentDkTk() {
		return this.currentLogDkTk;
	}

	public void setCurrentDkTk(double cur) {
		this.currentLogDkTk = cur;
	}

	public Hashtable<String, Morpheme> getGlobalStems() {
		return globalStems;
	}

	public void setGlobalStems(Hashtable<String, Morpheme> globalStems) {
		this.globalStems = globalStems;
	}

	public Hashtable<String, Morpheme> getGlobalSuffixes() {
		return globalSuffixes;
	}

	public void setGlobalSuffixes(Hashtable<String, Morpheme> globalSuffixes) {
		this.globalSuffixes = globalSuffixes;
	}
	
	public double getBetas() {
		return betas;
	}

	public static void setBetas(double betas_) {
		betas = betas_;
	}

	public double getBetam() {
		return betam;
	}

	public static void setBetam(double betam_) {
		betam = betam_;
	}

	public double getGammas() {
		return gammas;
	}

	public void setGammas(double gammas_) {
		gammas = gammas_;
	}

	public double getGammam() {
		return gammam;
	}

	public static void setGammam(double gammam_) {
		gammam = gammam_;
	}

	public double getAlpha() {
		return alpha;
	}

	public static void setAlpha(double alpha_) {
		alpha = alpha_;
	}
					
	public HashMap<Integer, Tree> getTrees() {
		return trees;
	}


	public void setTrees(HashMap<Integer, Tree> trees) {
		Main.trees = trees;
	}
					
					/*******************************************************/
					/************CREATE THE INITIAL STRUCTURE***************/
					/*******************************************************/
	
	public static void CreateInitialTrees(Main driver){

		for(int k=0; k<corpus.size(); k++){
			Word nextWord = corpus.get(k);	
			TreeNode newNode = new TreeNode();
			Main.leafNodes.add(newNode);
			Tree sampledTree = SampleTreeSegmentation(nextWord, driver); 
			
			if(sampledTree==null){ //Create a new tree..
				UpdateGlobalSetsAfterAdding(true, nextWord, null, driver);
				Tree newTree = new Tree(newNode, nextWord, driver);
				UpdateGlobalVariablesAfterAddingNode(true, newTree, newNode, nextWord);
			}
			else{ //Insert the word to an existing tree, to a randomly chosen position.
				newNode.addWordToNode(nextWord, driver.getGammas(), driver.getGammam());
				TreeNode sibling = sampledTree.getallNodes().get(randomGenerator.GeneratePosIntSmallerThan(sampledTree.getallNodes().size())); //choose a sibling node
				newNode.setLogDkH1(newNode.calculatePDkGivenH1(driver));
				newNode.setLogDkTk(newNode.calculateLogPDkGivenTk());
				AddWordToTree(nextWord, newNode, sibling, sampledTree, driver, false);
			}	
		}
	}
	
	public static void AddWordToTree(Word word, TreeNode nodeToInsert, TreeNode sibling, Tree tree, Main driver, boolean createdNewTree){

		UpdateGlobalSetsAfterAdding(createdNewTree, word, tree, driver);
		tree.AddNode(sibling, nodeToInsert, word, driver);
		UpdateGlobalVariablesAfterAddingNode(createdNewTree, tree, nodeToInsert, word);
	}
	
	public static boolean RemoveWordFromTree(Tree tree, Word word, TreeNode node, Main driver){
	
		if(node.getParent()==null) //tree is to be deleted..
			UpdateGlobalSetsAfterRemoving(true, word, null, driver);
		else
			UpdateGlobalSetsAfterRemoving(false, word, tree, driver);
		
		TreeNode newRoot = tree.RemoveNodeFromTree(node, word, driver);
		
		if(newRoot==null)
			UpdateGlobalVariablesAfterRemovingNode(true, tree, null, node, word);
		else
			UpdateGlobalVariablesAfterRemovingNode(false, null, tree, node, word);
		
		if(newRoot==null)
			return true;
		return false;
	}
						
	
						/*******************************************************/
						/**********************SAMPLING*************************/
						/*******************************************************/
	
	/**
	 * Sampling step 1: Sample a segmentation for the given word and a tree (where the word will be inserted.)
	 * @param word
	 */
	public static Tree SampleTreeSegmentation(Word word, Main driver){
	
		int wordLength = word.getWord().length();
		double sumOfProbs = 0, nextProb;
		
		ArrayList<InitialSegments> allPosSegments = new ArrayList<InitialSegments>(); 
		
		//For existing trees..
		Iterator<Map.Entry<Integer, Tree>> entries = Main.trees.entrySet().iterator();
		while(entries.hasNext())
		{
			Map.Entry<Integer, Tree> entry = entries.next();
			Tree nextTree = entry.getValue();
			if(wordLength>2){
				for(int j=1; j<wordLength+1; j++){ //for each split point of the word
					nextProb = treeSegmentationProb(nextTree, word.getWord().substring(0, j), word.getWord().substring(j, wordLength), driver);
					allPosSegments.add(new InitialSegments(nextTree.id, word.getWord().substring(0, j), word.getWord().substring(j, wordLength), nextProb));
					sumOfProbs += nextProb; 
				}
			}
			else{
				nextProb = treeSegmentationProb(nextTree, word.getWord(), "", driver);
				allPosSegments.add(new InitialSegments(nextTree.id, word.getWord(), "", nextProb));
				sumOfProbs += nextProb;
			}
		}
	    
		//For an empty tree..
		if(wordLength>2){
			for(int j=1; j<wordLength+1; j++){ //for each split point of the word
				nextProb = treeSegmentationProb(null, word.getWord().substring(0, j), word.getWord().substring(j, wordLength), driver); 
				allPosSegments.add(new InitialSegments(-1, word.getWord().substring(0, j), word.getWord().substring(j, wordLength), nextProb));
				sumOfProbs += nextProb;
			}
		}
		else{
			nextProb = treeSegmentationProb(null, word.getWord(), "", driver);
			allPosSegments.add(new InitialSegments(-1, word.getWord(), "", nextProb));
			sumOfProbs += nextProb;	
		}
		
		int treeSegmentationProbsIndex = Various.NormaliseNSampleFromArray(allPosSegments, sumOfProbs, randomGenerator );
		InitialSegments chosensample = allPosSegments.get(treeSegmentationProbsIndex);
		
		word.setStem(chosensample.getStem(), driver.getGammas(), driver.getGammam());
		word.setSuffix(chosensample.getSuffix(), driver.getGammas(), driver.getGammam());
		allPosSegments.clear();
		if(chosensample.getTreeId()==-1)
			return null;
		else
			return driver.getTrees().get(chosensample.getTreeId());
	}
	
	public static double treeSegmentationProb(Tree tree, String stem, String suffix, Main driver){
		
		double treeProb = 0; 
		double segmentProb = 0;
		double stemProb, suffixProb;
		
		if(tree!=null){ //for an existing tree
			treeProb = ((double)(tree.getRoot().getWordSet().size()))/((double)Main.WordNumberOnTrees+driver.getAlpha());
			
			stemProb = tree.getRoot().CalStemGivenRoot(stem, driver);
			suffixProb = tree.getRoot().CalSuffixGivenRoot(suffix, driver);
			segmentProb = stemProb*suffixProb;
		}
		else{ //for the new (empty) tree
			treeProb = (driver.getAlpha())/((double)Main.WordNumberOnTrees+driver.getAlpha());
			segmentProb = Various.CalculateStemProbGivenGlobalSet(stem, driver)*Various.CalculateSuffixProbGivenGlobalSet(suffix, driver);
		}
		return treeProb*segmentProb;
	}
	
	public static void UpdateGlobalSetsAfterAdding(boolean createdNewTree, Word word, Tree sampledTree, Main driver){
		
		if(createdNewTree){
			Various.addToHashtable(word.stem.morpheme, driver.getGlobalStems(), driver.getGammas());
			Various.addToHashtable(word.suffix.morpheme, driver.getGlobalSuffixes(), driver.getGammam());
			driver.setFm(driver.getFm()+1); 
			driver.setFs(driver.getFs()+1);
		}
		else{
			if(!sampledTree.getRoot().getStemSet().containsKey(word.stem.morpheme)){
				Various.addToHashtable(word.stem.morpheme, driver.getGlobalStems(), driver.getGammas());
				driver.setFs(driver.getFs()+1);
			}
			if(!sampledTree.getRoot().getSuffixSet().containsKey(word.suffix.morpheme)){
				Various.addToHashtable(word.suffix.morpheme, driver.getGlobalSuffixes(), driver.getGammam());
				driver.setFm(driver.getFm()+1);
			}
		}
	}
	
	public static void UpdateGlobalVariablesAfterAddingNode(boolean createdNewTree, Tree tree, TreeNode newNode, Word word){
		
		if(createdNewTree){ //a new tree is created..
			Main.allNodesOnWhichTree.put(newNode.getId(), tree);
			Main.trees.put(tree.id, tree);
		}
		else
			Main.allNodesOnWhichTree.put(newNode.getId(), tree);
		Main.WordNumberOnTrees++;
	}
	
	public static void UpdateGlobalSetsAfterRemoving(boolean deletedATree, Word word, Tree tree, Main driver){
	
		if(deletedATree){ //a tree is deleted..
			Various.removeFromHashtable(word.stem.getMorpheme(), driver.getGlobalStems());
			Various.removeFromHashtable(word.suffix.getMorpheme(), driver.getGlobalSuffixes());
			driver.setFs(driver.getFs()-1);
			driver.setFm(driver.getFm()-1);
		}
		else{
			if(tree.getRoot().getStemSet().get(word.stem.morpheme).freq==1){ //if the stem is to be removed from the tree..
				Various.removeFromHashtable(word.stem.morpheme, driver.getGlobalStems());
				driver.setFs(driver.getFs()-1);
			}
			if(tree.getRoot().getSuffixSet().get(word.suffix.morpheme).freq==1){ //if the suffix is to be removed from the tree..
				Various.removeFromHashtable(word.suffix.morpheme, driver.getGlobalSuffixes());
				driver.setFm(driver.getFm()-1);
			}
		}
	}
	
	public static void UpdateGlobalVariablesAfterRemovingNode(boolean deletedATree, Tree deletedTree, Tree tree, TreeNode deletedNode, Word word){
		
		if(deletedATree){ //a tree is deleted..
			Main.allNodesOnWhichTree.remove(deletedNode.getId());
			Main.trees.remove(deletedTree.id);
		}
		else
			Main.allNodesOnWhichTree.remove(deletedNode.getId());
		Main.WordNumberOnTrees--;
	}
	
	public static double currentTotalLogDkTk(Main driver){
		
		double totaldktk=0;
		Tree tree;
		
		Iterator<Map.Entry<Integer, Tree>> entries = Main.trees.entrySet().iterator();
		while(entries.hasNext()){
			Map.Entry<Integer, Tree> entry = entries.next();
			tree = entry.getValue();
			totaldktk += tree.getRoot().getLogDkTk();
		}
		totaldktk+=CalculateGlobalStemsCRP(driver);
		totaldktk+=CalculateGlobalSuffixesCRP(driver);
		return totaldktk;
	}
	
	public static double CalculateGlobalStemsCRP(Main driver){
		
		double totalProb=0;
		Enumeration<String> keys = driver.getGlobalStems().keys();
		while( keys.hasMoreElements()){
			Object key = keys.nextElement();
			Morpheme nextStem = driver.getGlobalStems().get(key);
			totalProb += Math.log(nextStem.getProbLength());
			totalProb += jsc.util.Maths.logGamma(nextStem.getFreq()); //log(nk-1)!
		}
		totalProb += (driver.getGlobalStems().size())*Math.log(driver.getBetas());
		totalProb += jsc.util.Maths.logGamma(driver.getBetas())-jsc.util.Maths.logGamma(driver.getFs()+driver.getBetas());
		return totalProb;
	}
	
	public static double CalculateGlobalSuffixesCRP(Main driver){
		
		double totalProb=0;
		Enumeration<String> keys = driver.getGlobalSuffixes().keys();
		while( keys.hasMoreElements()){
			Object key = keys.nextElement();
			Morpheme nextSuffix = driver.getGlobalSuffixes().get(key);
			totalProb += Math.log(nextSuffix.getProbLength());
			totalProb += jsc.util.Maths.logGamma(nextSuffix.getFreq()); //log(nk-1)!
		}
		totalProb += (driver.getGlobalSuffixes().size())*Math.log(driver.getBetam());
		totalProb += jsc.util.Maths.logGamma(driver.getBetam())-jsc.util.Maths.logGamma(driver.getFm()+driver.getBetam());
		return totalProb;
	}
	
	public static double UpdateGlobalCRPs_afterRemoving(Main driver, TreeNode newRoot, Morpheme stem, Morpheme suffix, boolean treeDeleted){
		
		double curLogDkH1 = Main.currentLogDkTk;
		
		//stems
		if(treeDeleted){
			curLogDkH1 += jsc.util.Maths.logGamma(driver.getFs()+1+driver.getBetas());
			curLogDkH1 -= jsc.util.Maths.logGamma(driver.getFs()+driver.getBetas());
		}
		else{
			if(!newRoot.getStemSet().containsKey(stem.morpheme)){
				curLogDkH1 += jsc.util.Maths.logGamma(driver.getFs()+1+driver.getBetas());
				curLogDkH1 -= jsc.util.Maths.logGamma(driver.getFs()+driver.getBetas());
			}
		}
		
		if(driver.globalStems.containsKey(stem.morpheme)){
			if(!newRoot.getStemSet().containsKey(stem.morpheme)){
				int freq = driver.globalStems.get(stem.morpheme).getFreq();
				curLogDkH1 -= jsc.util.Maths.logGamma(freq+1);
				curLogDkH1 += jsc.util.Maths.logGamma(freq);
			}
		}
		else{
			curLogDkH1 -= Math.log(stem.probLength);
			curLogDkH1 -= Math.log(driver.getBetas());
		}
		
		//suffixes
		if(treeDeleted){
			curLogDkH1 += jsc.util.Maths.logGamma(driver.getFm()+1+driver.getBetam());
			curLogDkH1 -= jsc.util.Maths.logGamma(driver.getFm()+driver.getBetam());
		}
		else{
			if(!newRoot.getSuffixSet().containsKey(suffix.morpheme)){
				curLogDkH1 += jsc.util.Maths.logGamma(driver.getFm()+1+driver.getBetam());
				curLogDkH1 -= jsc.util.Maths.logGamma(driver.getFm()+driver.getBetam());
			}
		}
		
		if(driver.globalSuffixes.containsKey(suffix.morpheme)){
			if(!newRoot.getSuffixSet().containsKey(suffix.morpheme)){
				int freq = driver.globalSuffixes.get(suffix.morpheme).getFreq();
				curLogDkH1 -= jsc.util.Maths.logGamma(freq+1);
				curLogDkH1 += jsc.util.Maths.logGamma(freq);
			}
		}
		else{
			curLogDkH1 -= Math.log(suffix.probLength);
			curLogDkH1 -= Math.log(driver.getBetam());
		}
		
		return curLogDkH1;
	}
	
	public static double UpdateGlobalCRPs_afterAdding(Main driver, TreeNode newRoot, Morpheme stem, Morpheme suffix, boolean treeCreated){
	
		double curLogDkH1 = Main.currentLogDkTk;

		//stems
		if(treeCreated || (newRoot.getStemSet().get(stem.morpheme).freq==1)){
			curLogDkH1 += jsc.util.Maths.logGamma(driver.getFs()-1+driver.getBetas());
			curLogDkH1 -= jsc.util.Maths.logGamma(driver.getFs()+driver.getBetas());
		}
		
		if(driver.globalStems.get(stem.morpheme).freq>1 && (treeCreated || (newRoot.getStemSet().get(stem.morpheme).freq==1))){
			int freq = driver.globalStems.get(stem.morpheme).getFreq();
			curLogDkH1 -= jsc.util.Maths.logGamma(freq-1);
			curLogDkH1 += jsc.util.Maths.logGamma(freq);
		}
		else{
			if(driver.globalStems.get(stem.morpheme).freq==1 && (treeCreated || newRoot.getStemSet().get(stem.morpheme).freq==1)){
				curLogDkH1 += Math.log(stem.probLength);
				curLogDkH1 += Math.log(driver.getBetas());
			}
		}				
		
		
		//suffixes
		if(treeCreated || (newRoot.getSuffixSet().get(suffix.morpheme).freq==1)){
			curLogDkH1 += jsc.util.Maths.logGamma(driver.getFm()-1+driver.getBetam());
			curLogDkH1 -= jsc.util.Maths.logGamma(driver.getFm()+driver.getBetam());
		}
		
		if(driver.globalSuffixes.get(suffix.morpheme).freq>1 && (treeCreated || (newRoot.getSuffixSet().get(suffix.morpheme).freq==1))){
			int freq = driver.globalSuffixes.get(suffix.morpheme).getFreq();
			curLogDkH1 -= jsc.util.Maths.logGamma(freq-1);
			curLogDkH1 += jsc.util.Maths.logGamma(freq);
		}
		else{
			if(driver.globalSuffixes.get(suffix.morpheme).freq==1 && (treeCreated || newRoot.getSuffixSet().get(suffix.morpheme).freq==1)){
				curLogDkH1 += Math.log(suffix.probLength);
				curLogDkH1 += Math.log(driver.getBetam());
			}
		}
		
		return curLogDkH1;
	}
		
	public static void MetropolisHastings(Main driver){
		
		double ilk=0;
		while(Main.temperature>0.1){
			
			for(int i=0;i<Main.leafNodes.size(); i++){
				TreeNode nextNode = Main.leafNodes.get(i);
				
				//Save the previous position, split etc.
				Tree pre_nodesTree = Main.allNodesOnWhichTree.get(nextNode.getId());
				TreeNode pre_sibling = nextNode.FindSibling();
									
				Word word = nextNode.getWordSet().iterator().next(); 
				String pre_Stem		= word.stem.getMorpheme();
				String pre_Suffix	= word.suffix.getMorpheme();
				
				//1. Remove the node from the current position.
				ilk = Main.currentLogDkTk;
				
				Main.currentLogDkTk -= pre_nodesTree.getRoot().getLogDkTk();
				boolean treeDeleted = RemoveWordFromTree(pre_nodesTree, word, nextNode, driver);
				if(!treeDeleted)
					Main.currentLogDkTk += pre_nodesTree.getRoot().getLogDkTk();
				double newLogDkTk = UpdateGlobalCRPs_afterRemoving(driver, pre_nodesTree.getRoot(), word.stem, word.suffix, treeDeleted);
				Main.currentLogDkTk = newLogDkTk; 
			
				//2. Insert the node at the sampled position with the sampled segmentation point. 
				Tree newTree = generateNewSample(nextNode, word, driver);
				if(newTree.root.getWordSet().size()==1)
					newLogDkTk = UpdateGlobalCRPs_afterAdding(driver, newTree.root, word.stem, word.suffix, true);
				else
					newLogDkTk = UpdateGlobalCRPs_afterAdding(driver, newTree.root, word.stem, word.suffix, false);
				//3. Accept or reject..
				Main.currentLogDkTk = newLogDkTk;
				
				boolean accepted = AcceptOrReject(newLogDkTk, ilk);
				
				//4. If the next sample is rejected..
				if(!accepted){
					//Remove the word from the sampled position..
					Main.currentLogDkTk -= newTree.getRoot().getLogDkTk();
					treeDeleted = RemoveWordFromTree(newTree, word, nextNode, driver);
					if(!treeDeleted)
						Main.currentLogDkTk += newTree.getRoot().getLogDkTk();
					newLogDkTk = UpdateGlobalCRPs_afterRemoving(driver, newTree.getRoot(), word.stem, word.suffix, treeDeleted);
					Main.currentLogDkTk = newLogDkTk;
					
					//Re-insert the word to the previous position with the previous segmentation..
					word.setStem(pre_Stem, driver.getGammas(), driver.getGammam());
					word.setSuffix(pre_Suffix, driver.getGammas(), driver.getGammam());
					nextNode.Clear();
					nextNode.addWordToNode(word, driver.getGammas(), driver.getGammam());
					
					if(pre_sibling!=null){
						AddWordToTree(word, nextNode, pre_sibling, pre_nodesTree, driver, false);
						nextNode.setLogDkH1(nextNode.calculatePDkGivenH1(driver));
						nextNode.setLogDkTk(nextNode.calculateLogPDkGivenTk());
					}
					else{
						nextNode.setLogDkH1(0);
						nextNode.setLogDkTk(0);
						pre_nodesTree.getallNodes().add(nextNode);
						pre_nodesTree.setRoot(nextNode);
						UpdateGlobalSetsAfterAdding(true, word, null, driver);
						UpdateGlobalVariablesAfterAddingNode(true, pre_nodesTree, nextNode, word);
						driver.setCurrentDkTk(driver.getCurrentDkTk()+nextNode.getLogDkTk());
					}
					if(pre_nodesTree.root.getWordSet().size()==1)
						newLogDkTk = UpdateGlobalCRPs_afterAdding(driver, pre_nodesTree.root, word.stem, word.suffix, true);
					else
						newLogDkTk = UpdateGlobalCRPs_afterAdding(driver, pre_nodesTree.root, word.stem, word.suffix, false);
					
					Main.currentLogDkTk = newLogDkTk;
					
				}
			}
			
			Main.temperature -= Main.tempDec;
		}
	}
	
	public static Tree generateNewSample(TreeNode nextNode, Word word, Main driver){
		
		Tree tree=null;
		
		//SAMPLE TREE
		tree = sampleATree(driver);
		
		//SAMPLE A SEGMENTATION POINT
		word.RandomlySplit(randomGenerator, driver);		
		nextNode.Clear();
		
		//SAMPLE A SIBLING
		if(tree!=null){
			TreeNode sibling = tree.getallNodes().get(randomGenerator.GeneratePosIntSmallerThan(tree.getallNodes().size()));
			nextNode.addWordToNode(word, driver.getGammas(), driver.getGammam());
			UpdateGlobalSetsAfterAdding(false, word, tree, driver);
			tree.AddNode(sibling, nextNode, word, driver);
			UpdateGlobalVariablesAfterAddingNode(false, tree, nextNode, word);
			//System.out.println("existing tree");
			return tree;			
		}
		else{			
			UpdateGlobalSetsAfterAdding(true, word, null, driver);
			Tree newTree = new Tree(nextNode, word, driver);
			Main.trees.put(newTree.id, newTree);
			UpdateGlobalVariablesAfterAddingNode(true, newTree, nextNode, word);
			nextNode.setLogDkH1(0);
			nextNode.setLogDkTk(0);
			return newTree;
		}
	}

	public static boolean AcceptOrReject(double logNextDkTk, double ilk){
	
		if(logNextDkTk>=ilk)	//Accept 
				return true;
		else{
			//Decide
			double nextProportion = Math.exp(logNextDkTk-ilk);
			nextProportion  = Math.pow(nextProportion, (double)1/temperature);
			double rand = randomGenerator.GenerateDouble();
					
			if(rand<nextProportion) //accept
				return true;
			else //rejected
				return false;
		}
	}
	
	public static Tree sampleATree(Main driver){

		double sum=0, treeProb;
		ArrayList<Double> probabilities = new ArrayList<Double>(); 
		ArrayList<Tree> traversedTrees = new ArrayList<Tree>();
		
		Iterator<Map.Entry<Integer, Tree>> entries = Main.trees.entrySet().iterator();
		while(entries.hasNext())
		{
			Map.Entry<Integer, Tree> entry = entries.next();
			Tree nextTree = entry.getValue();
			treeProb = ((double)(nextTree.getRoot().getWordSet().size()))/((double)Main.WordNumberOnTrees+driver.getAlpha());
			sum+=treeProb;
			probabilities.add(treeProb);
			traversedTrees.add(nextTree);
		}
		treeProb = (driver.getAlpha())/((double)Main.WordNumberOnTrees+driver.getAlpha());
		sum+=treeProb;
		probabilities.add(treeProb);
		
		int treeIndex = NormaliseNSampleFromArray(probabilities, sum);
		if(treeIndex-1<Main.trees.size())
			return traversedTrees.get(treeIndex-1);
		return null;
	}
	
	public static int NormaliseNSampleFromArray(ArrayList<Double> probabilities, double sum){
		
		double random = randomGenerator.GenerateDouble();
		probabilities.set(0, probabilities.get(0)/sum);
		if(random < probabilities.get(0))
			return 1;
		int i=1; 
		for(i=1; i<probabilities.size(); i++){
			probabilities.set(i, probabilities.get(i-1)+probabilities.get(i)/sum);
			//System.out.println();
			if(random < probabilities.get(i))
				return i+1;
		}
		return i; 
	}
	

	public static State getMaxSurvivor() {
		return maxSurvivor;
	}


	public static void setMaxSurvivor(State maxSurvivor) {
		Main.maxSurvivor = maxSurvivor;
	}
}

