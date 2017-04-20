package io;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import driver.Main;
import various.RandomGenerator;
import model.Morpheme;
import model.Tree;
import model.TreeNode;
import model.Word;


public class HBC_IO extends IO_Class{

	public ArrayList<Word> ReadWordList(String fileName, ArrayList<Word> corpus, RandomGenerator randGenerator, int trainingSetSize, double gammas, double gammam, int wordfreq, String goldf) {
		
		RandomAccessFile wordListFile=OpenFile(fileName);
		Tokenizer tokenizer = new Tokenizer();
		String line=new String();
		
		if(wordListFile!=null){
			
			while((line = ReadLine(wordListFile)) != null){
				Word word = tokenizer.TokenizeWordListLine(line, gammas, gammam, wordfreq);
				if(word!=null) corpus.add(word);
			}
				
			CloseFile(wordListFile);
			System.out.println("Reducing corpus size..");
			ArrayList<Word> reducedCorpus = ReduceCorpus(corpus, randGenerator, trainingSetSize);
			
			RandomAccessFile goldFile=OpenFile(goldf);
			while((line = ReadLine(goldFile)) != null){
				StringTokenizer st = new StringTokenizer(line,":");
				String ww=st.nextToken();
				if(!reducedCorpus.contains(ww))
					reducedCorpus.add(new Word(ww, 1, gammas, gammam));
			}
			CloseFile(goldFile);

			return reducedCorpus;
		}
		else{
			System.out.println("Data file cannot be opened!");
			return null;
		}
	}
	
	public ArrayList<Word> ReduceCorpus(ArrayList<Word> corpus, RandomGenerator randGenerator, int trainingSetSize){
		
		ArrayList<Word> newCorpus = new ArrayList<Word>();
		
		int count=0;
		while(newCorpus.size() < trainingSetSize){
			int index = randGenerator.GeneratePosIntSmallerThan(corpus.size());
			newCorpus.add(corpus.remove(index));
			count++;
		}
		return newCorpus;
	}
	
	/**
	 * Returns the last n letters of the given word. 
	 * @param n
	 * @return
	 */
	public static String ReturnWordsLastNChars(String word, int n){
		int wordLength = word.length();
		if(n<=wordLength)
			return word.substring(wordLength-n, wordLength);
		else
			return null;
	}
	
	/**
	 * Returns the first n letters of the given word. 
	 * @param n
	 * @return
	 */
	public static String ReturnWordsInitialNChars(String word, int n){
		int wordLength = word.length();
		if(n<=wordLength)
			return word.substring(0, n);
		else
			return null;
	}
	
	public void SplitWithViterbi(String wordListFile, String resultFile, Main driver, boolean b){
	
	RandomAccessFile testFile = OpenFile(wordListFile);
	RandomAccessFile results = OpenFile(resultFile);
	ClearContents(resultFile);
	
	String nextWord, nextLine, morph;
	int wordLength, bestl;
	double bestdelta, currdelta;
	char besttype='s';
	ProbType tempProbType = new ProbType();
	ArrayList<String> morphs = new ArrayList<String>();
	
	while((nextLine = ReadLine(testFile)) != null){
		
		StringTokenizer st;
		if(b){
			st = new StringTokenizer(nextLine);
			st.nextToken();
		}
		else
			st = new StringTokenizer(nextLine,":");
		nextWord = st.nextToken();
		//System.out.println(nextWord);
		wordLength = nextWord.length();
		ProbType[] delta = new ProbType[wordLength+1];
		int[] psi = new int[wordLength+1];
		delta[0]= new ProbType(0, 's');
		psi[0]=0;
		for(int t=1; t<=wordLength; t++){
			
			bestdelta=Double.NEGATIVE_INFINITY;
			bestl=0;
			for(int l=1; l<=t; l++){
			
				morph = nextWord.substring(t-l, t);
				if(t==l)
					ComputeProb(driver, morph, -1, tempProbType);
				else if(delta[t-l].type=='s')
					ComputeProb(driver, morph, 0, tempProbType);
				else 
					ComputeProb(driver, morph, 1, tempProbType);
				currdelta = delta[t-l].prob+Math.log(tempProbType.prob);
				if(currdelta>bestdelta){
					bestdelta = currdelta;
					bestl=l;
					besttype=tempProbType.type;
				}
			}
			ProbType newProbType = new ProbType(bestdelta, besttype);
			delta[t] = newProbType;
			psi[t] = bestl;
		}
		
		int t=wordLength;
		while(psi[t]!=0){
			morphs.add(nextWord.substring(t-psi[t],t));
			t-=psi[t];
		}
		
		WriteString(nextWord + "\t", results);
		for(t=morphs.size()-1; t>=0; t--)
			WriteString(morphs.get(t) + " ", results);
		WriteString("\n", results);
		
		psi=null;
		delta=null;
		morphs.clear();
	}
	CloseFile(results);
	CloseFile(testFile);
}
	
	

/**
 * Computes the probability of the given morph in the model. 
 * @param driver
 * @param morphType : -1 stem, 1 suffix, 0 does not know yet.
 */
public void ComputeProb(Main driver, String morph, int morphType, ProbType pt){

	String punctuations = ".,:;!\"$#&*+-<>?%@=";
	
	if(morph.length()==1 && punctuations.contains(morph)){
		pt.prob=1;
		pt.type='s';
		return;
		
	}
	
	Iterator<Map.Entry<Integer, Tree>> entries = driver.getTrees().entrySet().iterator();
	double prob=0, temp1=0, temp2=0;
	
	if(morphType==-1){ //it is a stem..
		while(entries.hasNext())
		{
			Map.Entry<Integer, Tree> entry = entries.next();
			Tree nextTree = entry.getValue();
			prob += nextTree.getRoot().CalStemGivenRoot(morph, driver);
		}
		pt.prob = prob;
		pt.type = 's';
	}
	else if(morphType==1){ //it is a suffix..
		while(entries.hasNext())
		{
			Map.Entry<Integer, Tree> entry = entries.next();
			Tree nextTree = entry.getValue();
			prob += nextTree.getRoot().CalSuffixGivenRoot(morph, driver);
		}
		pt.prob = prob;
		pt.type = 1;
	} //we don't know what type it is.
	else{
		while(entries.hasNext())
		{
			Map.Entry<Integer, Tree> entry = entries.next();
			Tree nextTree = entry.getValue();
			temp1 += nextTree.getRoot().CalStemGivenRoot(morph, driver);
			temp2 += nextTree.getRoot().CalSuffixGivenRoot(morph, driver);
		}
		if(temp1>temp2){
			pt.prob = temp1;
			pt.type = 's';
		}
		else{
			pt.prob = temp2;
			pt.type = 'm';
		}

	}
}

	
public void PrintTrees(HashMap<Integer, Tree> trees){
	
	RandomAccessFile file = OpenFile("trees");
	Iterator<Map.Entry<Integer, Tree>> entries = trees.entrySet().iterator();
	while(entries.hasNext())
	{
		Map.Entry<Integer, Tree> entry = entries.next();
		Tree nextTree = entry.getValue();
		WriteString("\n" + "Tree " + nextTree.id + "\n", file);
		PrintPretty(file, nextTree.getRoot(), " ", true);
	}
	CloseFile(file);
}

public void PrintPretty(RandomAccessFile file, TreeNode node, String indent, boolean last)
{
	
	if (last){
		WriteString(indent, file);
		WriteString(indent + "\\----", file);
		WriteString(node.getId() + " ", file);
		
		if(node.getParent() != null)
			WriteString(" (parent:" + node.getParent().getId() + "-" + ")", file);
		
		else
			WriteString(" (" + " cost: " + "-" + ")", file);

		for(int j=0; j<node.getWordSet().size(); j++){
					
			Word nextWord = node.getWordSet().get(j);
			WriteString("("+nextWord.getWord()+")" + nextWord.getStem().morpheme + "+" + nextWord.getSuffix().morpheme, file);
			WriteString(", ", file);
		}
		
		WriteString("(", file);
		Iterator<Morpheme> it = node.getStemSet().values().iterator();
		while(it.hasNext()){
			Morpheme nextMorph = it.next();
			WriteString(nextMorph.getMorpheme() + ",", file);
		}
		WriteString(")", file);
		
		WriteString("(", file);
		it = node.getSuffixSet().values().iterator();
		while(it.hasNext()){
			Morpheme nextMorph = it.next();
			WriteString(nextMorph.getMorpheme() + ",", file);
		}
		WriteString(")", file);
		
		
		indent += "           ";
		       
		if(node.getLeftNode() != null)
			PrintPretty(file, node.getLeftNode(), indent, true);
		else
			PrintPretty(file, node.getLeftNode(), indent, false);
	    	   
		if(node.getRightNode() != null)
			PrintPretty(file, node.getRightNode(), indent, true);
		else
			PrintPretty(file, node.getRightNode(), indent, false);
		indent += "           ";
	}
  }
}
