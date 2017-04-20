# TreeStructuredDP

Sample usage:

	java -jar HDP.jar trainSet trainSize resultsFile temp betaS betaM gammaS gammaM alpha wordFreq goldSet

-trainSet: the name of the input file, where each line involves the frequency of the word followed by the word itself

-trainSize: If the full training file is not to be used, the size of the training set that will be randomly sampled from the trainSet.

-resultsFile: The final segmentation of words will be written to resultsFile

-temp: The temperature decreasing intervals in simulated annealing.

-betaS: DP concentration parameter for stems

-betaM:DP concentration parameter for suffixes

-gammaS: The base distribution parameter for stems

-gammaM: The base distribution parameter for suffixes

-alpha: The DP concentration parameter for the DP that generates trees. 

-wordFreq: If any filtering is to be done on the trainingSet, the frequency threshold that will be used for filtering. 

-goldSet: If the gold set will be included in the trainingSet, goldSet could be provided. 
