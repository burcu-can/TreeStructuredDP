package io;

import java.util.HashMap;

public class State {
	
	String str;
	HashMap<String, State> nextStates;
	State pre;
	double survivor;
	char type;
	
	public State(String s, State p){
		this.str = s;
		nextStates = new HashMap<String,State>();
		this.pre = p;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public HashMap<String, State> getNextStates() {
		return nextStates;
	}

	public void setNextStates(HashMap<String, State> nextStates) {
		this.nextStates = nextStates;
	}

	public State getPre() {
		return pre;
	}

	public void setPre(State pre) {
		this.pre = pre;
	}

	public double getSurvivor() {
		return survivor;
	}

	public void setSurvivor(double survivor) {
		this.survivor = survivor;
	}

	public char getType() {
		return type;
	}

	public void setType(char type) {
		this.type = type;
	}
}
