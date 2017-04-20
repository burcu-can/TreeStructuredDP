package various;

import java.util.Random;

public class RandomGenerator {

	Random generator = new Random();
	
	public int GeneratePosIntSmallerThan(int smallThan){
		
		int posInt = generator.nextInt()%smallThan;
		
		if(posInt<0) //if negative
			posInt *= -1;
		return posInt;
	}
	
	public int GenerateInteger(){
		return generator.nextInt();
	}
	
	public double GenerateDouble(){
		return generator.nextDouble();
	}
}
