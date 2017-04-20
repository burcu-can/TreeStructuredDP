package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import driver.Main;


public class Tree {

	public TreeNode root;
	public int id;
	private static int idCounter=0;
	private ArrayList<TreeNode> allNodes;
	
	public Tree(TreeNode node, Word word, Main driver){
						
		this.root = node;
		node.addWordToNode(word, driver.getGammas(), driver.getGammam());
		node.setLogDkH1(node.calculatePDkGivenH1(driver));
		node.setLogDkTk(node.calculateLogPDkGivenTk());
		
		allNodes = new ArrayList<TreeNode>();
		allNodes.add(root);
 		
		this.id=idCounter;
		idCounter++;
	}
	
	/***************************************************************************/
	/***************************************************************************/
	/***************************GETTERS & SETTERS ******************************/
	/***************************************************************************/
	/***************************************************************************/
	
	public TreeNode getRoot() {
		return root;
	}

	public void setRoot(TreeNode root) {
		this.root = root;
	}

	public ArrayList<TreeNode> getallNodes() {
		return allNodes;
	}

	public void setallNodes(ArrayList<TreeNode> allNodes) {
		this.allNodes = allNodes;
	}
		
	/******************************************************************************************/
	/******************************************************************************************/
	
	
	public void AddNode(TreeNode brother, TreeNode nodeToInsert, Word word, Main driver){

		driver.setCurrentDkTk(driver.getCurrentDkTk()-this.getRoot().getLogDkTk());
		
		TreeNode spareNode = new TreeNode();
		spareNode.AddNodesContents(brother, driver.getGammas(), driver.getGammam());
		spareNode.AddNodesContents(nodeToInsert, driver.getGammas(), driver.getGammam());
		spareNode.setLeftNode(nodeToInsert);
		spareNode.setRightNode(brother);
		this.getallNodes().add(nodeToInsert);
		this.getallNodes().add(spareNode);
		
		
		if(brother.getParent() != null){
			spareNode.setParent(brother.getParent());
		
			if(brother.getParent().getRightNode().getId() == brother.getId())
				brother.getParent().setRightNode(spareNode);
			else
				brother.getParent().setLeftNode(spareNode);
			
			nodeToInsert.setParent(spareNode);
			brother.setParent(spareNode);
			
			//Add the data in the added node to every node on the path to the root
			//this.addWordToTree(word);
			TreeNode addToNode = spareNode.getParent();
			while(addToNode != null){
				addToNode.addWordToNode(word, driver.getGammas(), driver.getGammam());
				addToNode = addToNode.getParent();
			}
		}
		else{
			this.root = spareNode;
			brother.setParent(spareNode);
			nodeToInsert.setParent(spareNode);
			this.root.setLeftNode(brother);
			this.root.setRightNode(nodeToInsert);
		}
		
		if(spareNode.getParent()==null){ //the brother was the root node before..re-calculate its dkh1
			brother.setLogDkH1(brother.calculatePDkGivenH1(driver));
			brother.setLogDkTk(brother.calculateLogPDkGivenTk());	
		}
		
		nodeToInsert.setLogDkH1(nodeToInsert.calculatePDkGivenH1(driver));
		nodeToInsert.setLogDkTk(nodeToInsert.calculateLogPDkGivenTk());
		spareNode.UpdateNodeProbabilities(word, 0, driver, true);
		UpdateAllProbabilitiesTillTheRoot(spareNode.getParent(), word, 1, driver);
		driver.setCurrentDkTk(driver.getCurrentDkTk()+this.getRoot().getLogDkTk());
	}
	
	public TreeNode RemoveNodeFromTree(TreeNode node, Word word, Main driver){
		
		TreeNode brother = node.FindSibling(); 
		this.getallNodes().remove(node);
		
		if(node.getParent()==null){ //if the tree is made up of only one node..
			node.removeWordFromNode(word);
			node.setLogDkH1(0);
			node.setLogDkTk(0);
			return null;
		}
		
		//if the given node is the left child of the parent node
		if(node.getParent().getLeftNode().getId() == node.getId()){
			
			if(node.getParent().getParent() != null){
				
				brother = node.getParent().getRightNode();
				
				if(node.getParent().getParent().getLeftNode().getId() == node.getParent().getId())
					node.getParent().getParent().setLeftNode(brother);
				else
					node.getParent().getParent().setRightNode(brother);
				brother.setParent(brother.getParent().getParent());
			}
			else{
				if(node.getParent().getLeftNode().getId() == node.getId())
					root = node.getParent().getRightNode();
				else
					root = node.getParent().getLeftNode();
				root.setParent(null);
			}
		}
		
		//if the given node is the right child of the parent node
		else{
			
			if(node.getParent().getParent() != null){
				
				brother = node.getParent().getLeftNode();
				
				if(node.getParent().getParent().getLeftNode().getId() == node.getParent().getId())
					node.getParent().getParent().setLeftNode(brother);
				else
					node.getParent().getParent().setRightNode(brother);
				brother.setParent(brother.getParent().getParent());
			}
			else{
				if(node.getParent().getLeftNode().getId() == node.getId())
					root = node.getParent().getRightNode();
				else
					root = node.getParent().getLeftNode();
				root.setParent(null);
			}
		}
		
		this.getallNodes().remove(node.getParent());
		node.getParent().Clear(); 
		node.setParent(null);
		
		//Remove the data in the removed node from every node on the path to the root
		if(brother != null){
			TreeNode removeFromNode = brother.getParent();
			while(removeFromNode != null){
				removeFromNode.removeWordFromNode(word);
				removeFromNode = removeFromNode.getParent();
			}
			this.UpdateAllProbabilitiesTillTheRoot(brother.getParent(), word, -1, driver);
		}
	//	brother.calculatePDkGivenH1(driver);
		//if(brother!=null)
		//	System.out.println("brother: " + brother.getId());
		if(brother!=null && brother.getParent()==null){ //sibling becomes the root now..
		
			Enumeration<String> keys = Collections.enumeration(brother.getStemSet().keySet());
			while( keys.hasMoreElements()){
				Object key = keys.nextElement();
				Morpheme nextStem = brother.getStemSet().get(key);
				double p = Math.log(nextStem.probLength);
				brother.setLogDkH1(brother.getLogDkH1()-p);
			}
			
			keys = Collections.enumeration(brother.getSuffixSet().keySet());
			while( keys.hasMoreElements()){
				Object key = keys.nextElement();
				Morpheme nextSuffix = brother.getSuffixSet().get(key);
				double p = Math.log(nextSuffix.probLength);
				brother.setLogDkH1(brother.getLogDkH1()-p);
			}
			brother.setLogDkTk(brother.calculateLogPDkGivenTk());
		}
		
		return root;
	}
	
	/***************************************************************************/
	/***************************************************************************/
	/*************PROBABILITY CALCULATIONS (for metropolis hastings)************/
	/***************************************************************************/
	/***************************************************************************/
	
	public void UpdateAllProbabilitiesTillTheRoot(TreeNode node, Word word, int added, Main driver){

		while(node!=null){
			if(node.getParent()!=null)
				node.UpdateNodeProbabilities(word, added, driver, false);
			else
				node.UpdateNodeProbabilities(word, added, driver, true);
			node = node.getParent();
		}
	}
}
