package clus.heuristic;

import java.util.ArrayList;

import clus.algo.tdidt.ClusNode;
import clus.data.rows.DataTuple;
import clus.data.rows.RowData;
import clus.data.type.ClusAttrType;

import org.apache.commons.math.special.Gamma;



public class EncodingCost {

	protected ArrayList<RowData> data;
	protected ArrayList<ClusNode> nodes;
	ClusAttrType[] attributes;
	double [ ][ ] alphaValues;
	double[] mixtureValues;
	
	public EncodingCost(ArrayList<ClusNode> listNodes, ArrayList<RowData> subsets, ClusAttrType[] attrs){
		data = subsets;
		nodes = listNodes;
		attributes = attrs;
		
		double [ ][ ] alphaValues1 = { 	{1.18065,  0.270671,  0.039848,  0.017576,  0.016415,  0.014268,  0.131916,  0.012391,  0.022599,  0.020358,  0.030727,  0.015315,  0.048298, 0.053803,  0.020662,  0.023612, 0.216147, 0.147226, 0.065438, 0.003758, 0.009621},
									{1.35583, 0.021465, 0.0103, 0.011741, 0.010883, 0.385651, 0.016416, 0.076196, 0.035329, 0.013921, 0.093517, 0.022034, 0.028593, 0.013086, 0.023011, 0.018866, 0.029156, 0.018153, 0.0361, 0.07177, 0.419641},
									{6.66436, 0.561459, 0.045448, 0.438366, 0.764167, 0.087364, 0.259114, 0.21494, 0.145928, 0.762204, 0.24732, 0.118662, 0.441564, 0.174822, 0.53084, 0.465529, 0.583402, 0.445586, 0.22705, 0.02951, 0.12109},
									{2.08141, 0.070143, 0.01114, 0.019479, 0.094657, 0.013162, 0.048038, 0.077, 0.032939, 0.576639, 0.072293, 0.02824, 0.080372, 0.037661, 0.185037, 0.506783, 0.073732, 0.071587, 0.042532, 0.011254, 0.028723},
									{2.08101, 0.041103, 0.014794, 0.00561, 0.010216, 0.153602, 0.007797, 0.007175, 0.299635, 0.010849, 0.999446, 0.210189, 0.006127, 0.013021, 0.019798, 0.014509, 0.012049, 0.035799, 0.180085, 0.012744, 0.026466},
									{2.56819, 0.115607, 0.037381, 0.012414, 0.018179, 0.051778, 0.017255, 0.004911, 0.796882, 0.017074, 0.285858, 0.075811, 0.014548, 0.015092, 0.011382, 0.012696, 0.027535, 0.088333, 0.94434, 0.004373, 0.016741},
									{1.76606, 0.093461, 0.004737, 0.387252, 0.347841, 0.010822, 0.105877, 0.049776, 0.014963, 0.094276, 0.027761, 0.01004, 0.187869, 0.050018, 0.110039, 0.038668, 0.119471, 0.065802, 0.02543, 0.003215, 0.018742},
									{4.98768, 0.452171, 0.114613, 0.06246, 0.115702, 0.284246, 0.140204, 0.100358, 0.55023, 0.143995, 0.700649, 0.27658, 0.118569, 0.09747, 0.126673, 0.143634, 0.278983, 0.358482, 0.66175, 0.061533, 0.199373},
									{0.0995, 0.005193, 0.004039, 0.006722, 0.006121, 0.003468, 0.016931, 0.003647, 0.002184, 0.005019, 0.00599, 0.001473, 0.004158, 0.009055, 0.00363, 0.006583, 0.003172, 0.00369, 0.002967, 0.002772, 0.002686}
		};
		
		alphaValues=alphaValues1;

		double[] mixtureValues1 = {0.178091, 0.056591, 0.0960191, 0.0781233, 0.0834977, 0.0904123, 0.114468, 0.0682132, 0.234585}; 
					
		mixtureValues=mixtureValues1;		
		
			
		
	}

	
	
	public EncodingCost(ArrayList<RowData> subsets, ClusAttrType[] attrs){
		data = subsets;
		attributes = attrs;
		
		double [ ][ ] alphaValues1 = { 	{1.18065,  0.270671,  0.039848,  0.017576,  0.016415,  0.014268,  0.131916,  0.012391,  0.022599,  0.020358,  0.030727,  0.015315,  0.048298, 0.053803,  0.020662,  0.023612, 0.216147, 0.147226, 0.065438, 0.003758, 0.009621},
				{1.35583, 0.021465, 0.0103, 0.011741, 0.010883, 0.385651, 0.016416, 0.076196, 0.035329, 0.013921, 0.093517, 0.022034, 0.028593, 0.013086, 0.023011, 0.018866, 0.029156, 0.018153, 0.0361, 0.07177, 0.419641},
				{6.66436, 0.561459, 0.045448, 0.438366, 0.764167, 0.087364, 0.259114, 0.21494, 0.145928, 0.762204, 0.24732, 0.118662, 0.441564, 0.174822, 0.53084, 0.465529, 0.583402, 0.445586, 0.22705, 0.02951, 0.12109},
				{2.08141, 0.070143, 0.01114, 0.019479, 0.094657, 0.013162, 0.048038, 0.077, 0.032939, 0.576639, 0.072293, 0.02824, 0.080372, 0.037661, 0.185037, 0.506783, 0.073732, 0.071587, 0.042532, 0.011254, 0.028723},
				{2.08101, 0.041103, 0.014794, 0.00561, 0.010216, 0.153602, 0.007797, 0.007175, 0.299635, 0.010849, 0.999446, 0.210189, 0.006127, 0.013021, 0.019798, 0.014509, 0.012049, 0.035799, 0.180085, 0.012744, 0.026466},
				{2.56819, 0.115607, 0.037381, 0.012414, 0.018179, 0.051778, 0.017255, 0.004911, 0.796882, 0.017074, 0.285858, 0.075811, 0.014548, 0.015092, 0.011382, 0.012696, 0.027535, 0.088333, 0.94434, 0.004373, 0.016741},
				{1.76606, 0.093461, 0.004737, 0.387252, 0.347841, 0.010822, 0.105877, 0.049776, 0.014963, 0.094276, 0.027761, 0.01004, 0.187869, 0.050018, 0.110039, 0.038668, 0.119471, 0.065802, 0.02543, 0.003215, 0.018742},
				{4.98768, 0.452171, 0.114613, 0.06246, 0.115702, 0.284246, 0.140204, 0.100358, 0.55023, 0.143995, 0.700649, 0.27658, 0.118569, 0.09747, 0.126673, 0.143634, 0.278983, 0.358482, 0.66175, 0.061533, 0.199373},
				{0.0995, 0.005193, 0.004039, 0.006722, 0.006121, 0.003468, 0.016931, 0.003647, 0.002184, 0.005019, 0.00599, 0.001473, 0.004158, 0.009055, 0.00363, 0.006583, 0.003172, 0.00369, 0.002967, 0.002772, 0.002686}
		};

		alphaValues=alphaValues1;

		double[] mixtureValues1 = {0.178091, 0.056591, 0.0960191, 0.0781233, 0.0834977, 0.0904123, 0.114468, 0.0682132, 0.234585}; 

		mixtureValues=mixtureValues1;		

		
		
	}
	
	
	public int getNbClusters(){
		return data.size();
	}	

	
	
	public int printAlphaValues(){
	
		// Iterating over rows
		for(int i=0;i<alphaValues.length;i++){
			// Iterating over columns
			for(int j=0;j<alphaValues[i].length;j++){
				System.out.print(alphaValues[i][j]+"\t");
			}
			System.out.print("\n");
		}
		return 0;
	}
	
	
	
	private int printMatrix(int[][] frequency ){
		// Iterating over rows
		for(int i=0;i<frequency.length;i++){
			// Iterating over columns
			for(int j=0;j<frequency[i].length;j++){
				System.out.print(frequency[i][j]+"\t");
			}
			System.out.print("\n");
		}
		return 0;
	}
	
	
	// This method returns the index for alpha value matrix given in the constructor
	// Note that it assumes that the following order was follow in the matrix
	// {Sum, A, C, D, E, F, G, H, I , K, L , M , N, P , Q , R , T , V, W, Y}
	private int returnIndexAlphaMatrix(char character){

		int index=0;
		
		switch(character){
			case 'A':
				index=1;
				break;
			case 'C':
				index=2;
				break;
			case 'D':
				index=3;
				break;
			case 'E':
				index=4;
				break;
			case 'F':
				index=5;
				break;
			case 'G':
				index=6;
				break;
			case 'H':
				index=7;
				break;
			case 'I':
				index=8;
				break;
			case 'K':
				index=9;
				break;
			case 'L':
				index=10;
				break;
			case 'M':
				index=11;
				break;
			case 'N':
				index=12;
				break;
			case 'P':
				index=13;
				break;
			case 'Q':
				index=14;
				break;
			case 'R':
				index=15;
				break;
			case 'S':
				index=16;
				break;
			case 'T':
				index=17;
				break;
			case 'V':
				index=18;
				break;
			case 'W':
				index=19;
				break;
			case 'Y':
				index=20;
				break;
		}

		return index;
		
	}
		
	
	// This is currently being used to generate the output
	private int printIntanceLabels(int nbSubsets){
		
		for(int i=0;i<nbSubsets;i++){
			int nbRows = data.get(i).getNbRows();
			for(int r=0;r<nbRows;r++){
				String[] parts = data.get(i).getTuple(r).toString().split(",");
				System.out.print(parts[0]+" ");
			}
			System.out.print("\n");
		}
		return 0;
	}

	
	// Generate the vector Ncs, which contains the number of occurrence of each amino acid
	// Gaps are not included in this counting
	protected int[][] calculateFrequency(int Attr,int nbSubsets){
		int [ ] [ ] frequency = new int  [ nbSubsets ] [21];  
	
		int j=Attr;

		for(int i=0;i<nbSubsets;i++){
			
			int nbRows = data.get(i).getNbRows();
			for(int r=0;r<nbRows;r++){
				String[] parts = data.get(i).getTuple(r).toString().split(",");
			
				int index=returnIndexAlphaMatrix(parts[j+1].charAt(0));
				if(index!=0){
					frequency[i][index]++;
					frequency[i][0]++;
				}
			}
		}
		
		return frequency;
	}
	
	
	// Returns value for the gamma function,
	// which is the exponential of the library function logGamma
	public double functionGamma(double x){
		return Math.exp(Gamma.logGamma(x));
	}
	
	
	// returns value for the function Z defined in the SCI-PHY encoding cost function
	// it allows to specify the range of the vector that is going to be given as input
	// to the function
	protected double functionZ(double[] vector, int start, int end){
		
		double valueFunction=1;
		double sum=0;
		
		for(int i=start;i<=end;i++){
			valueFunction=valueFunction*functionGamma(vector[i]);	
			sum=sum+vector[i];
		}
		
		valueFunction = valueFunction/functionGamma(sum);
		
		return valueFunction;
	}
	
	
	
	// This function uses the log properties to deal with the large values 
	// outputted by the gamma function 
	protected double functionZAlternative(double[] vector){
		
		double valueFunction=1;
		double sum=0;
		
		double logpart1=0;

		// log(prod(X))=log(X1)+log(X2)+.........+log(Xn)	
		for(int i=0;i<vector.length;i++){
			logpart1=logpart1+Gamma.logGamma(vector[i]);	
			sum=sum+vector[i];
		}
		
		double logpart2 = Gamma.logGamma(sum);
		
		valueFunction = Math.exp(logpart1-logpart2);
		
		return valueFunction;
	}
	
	
	// This function does not return the value of the function Z
	// but instead it returns the value of log(function Z)
	protected double functionLogZ(double[] vector){
	
		double valueFunction;
		double sum=0;
		double logpart1=0;
	
		// log(prod(X))=log(X1)+log(X2)+.........+log(Xn)	
		for(int i=0;i<vector.length;i++){
			logpart1=logpart1+Gamma.logGamma(vector[i]);	
			sum=sum+vector[i];
		}	
	
		double logpart2 = Gamma.logGamma(sum);
	
		valueFunction = logpart1-logpart2;
	
		return valueFunction;
	}
	
	
	// This function adds a line of the alpha matrix with the vector ncs
	// and returns a vector containing the sum of these vectors
	// Observe that the returned vector has only 20 positions
	protected double[] addAlphaVectorAndFrequencyvector(double[] alphaVector, int[] frequencyVector){
		
		double[] addedVector = new double[20];
		
		int j=0;
		
		for(int i=1;i<21;i++){
			addedVector[j]=frequencyVector[i]+alphaVector[i];
			j++;
			
		}
		return addedVector;
		
	}
	
	
	
	// function without mathematical tricks using normalizers
	// It does not return exactly the same value as when we use normalizers
	public double getEncodingCostValueStandard(){
		
		int nbAttr = attributes.length;
		int nbSubsets = data.size();
		double encodingCostValue= nbSubsets*(Math.log(nbSubsets)/Math.log(2));

		double[] zAlpha = new double[alphaValues.length];

		
		//printIntanceLabels(nbSubsets);
		
		// calculating function z for alpha vectors
		for(int k=0;k<alphaValues.length;k++){
			// here we use 1 as the start and 20 as the end
			// remember that the first index (0 - zero) contains the sum of the vector
			zAlpha[k] = functionZ(alphaValues[k],1,20);
		}		

		// we are going to produce a matrix with the frequency of occurrence of each amino acid
		// the columns are going to be the amino acids
		// the lines are going to be the subsets

		// iterating over all attributes
		for(int j=0;j<nbAttr;j++){
			int [ ] [ ] frequency = calculateFrequency(j,nbSubsets);
			//printMatrix(frequency);
			
			//System.out.print("Attribute "+j+"\n");
			for(int i=0;i<nbSubsets;i++){

				// calculate probability
					double probability=0;
				
					for(int k=0;k<alphaValues.length;k++){
						
							double mixture = mixtureValues[k];
						
							//calculate z(alpha+frequency)
							double ZAlphaFreq;
							ZAlphaFreq = functionZAlternative(addAlphaVectorAndFrequencyvector(alphaValues[k], frequency[i]));
							//System.out.print("ZAlphaFreq is "+ZAlphaFreq+"\n");
		
							probability = probability + mixture*(ZAlphaFreq/zAlpha[k]);
					}
		
					double logProb = Math.log(probability)/Math.log(2);
					
					encodingCostValue = encodingCostValue- logProb;
			}
		}
		
		return encodingCostValue;
		
	}
	
	
	
	
	public double getEncodingCostValueWithNormalizer(){
		
		
		int nbAttr = attributes.length;
		
		int nbSubsets = data.size();
		
		printIntanceLabels(nbSubsets);
		
		double encodingCostValue= nbSubsets*(Math.log(nbSubsets)/Math.log(2));
		
		// we do it here because we do not need to calculate it again every time
		double[] zAlpha = new double[alphaValues.length];
		
		for(int k=0;k<alphaValues.length;k++){
			// here we use 1 as the start and 20 as the end
			// remember that the first index (0 - zero) contains the sum of the vector
			zAlpha[k] = functionZ(alphaValues[k],1,20);
		}		

		
		
		// we are going to produce a matrix with the frequency of occurrence of each amino acid
		// the columns are going to be the amino acids
		// the lines are going to be the subsets
		
	
		// iterating over all attributes
		for(int j=0;j<nbAttr;j++){
	
			//System.out.print("Column "+j+"\n");
				int [ ] [ ] frequency = calculateFrequency(j,nbSubsets);
			//printMatrix(frequency);
			
			//System.out.print("Attribute "+j+"\n");
			for(int i=0;i<nbSubsets;i++){
					
					// calculate probability
					double[] logPJ = new double[9];
					double normalizer=0;
					
					for(int k=0;k<alphaValues.length;k++){
							double mixture = mixtureValues[k];
						
							//calculate log(z(alpha+frequency))
							double logZAlphaFreq;
							logZAlphaFreq = functionLogZ(addAlphaVectorAndFrequencyvector(alphaValues[k], frequency[i]));
							
							logPJ[k] = Math.log(mixture)+logZAlphaFreq-Math.log(zAlpha[k]);
		
							// Calculating normalizer
							if(k==0){
								normalizer=logPJ[k];
							}else{
								if(normalizer<logPJ[k]){
									normalizer=logPJ[k];
								}	
							}
					}
					
					double sumAllPJ=0;
					for(int k=0;k<alphaValues.length;k++){
						double convertingBackValue = Math.exp(logPJ[k]-normalizer);
						sumAllPJ = sumAllPJ+convertingBackValue;
					}
					
					double logProb = Math.log(sumAllPJ)+normalizer;
					
					encodingCostValue = encodingCostValue- logProb;
			}
		}
		return encodingCostValue;
	}
	
	
	public double getEncodingCostValue(){
	
		return getEncodingCostValueWithNormalizer();
		//return getEncodingCostValueStandard();
	}
	

}
