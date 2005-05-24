package clus.model.pmml.tildepmml;

import java.util.*;

import java.io.*;
import clus.main.*;
import clus.statistic.*;

import jeans.util.*;

import clus.model.modelio.tilde.*;

public class Tilde2PMMLv1 {

	static final String COPYRIGHT="Jan Struyf, Darek Krzywania";
	static final String DESCRIPTION="Predicting skidding for the SolEuNet Challenge";
	static final String APPLICATIONNAME="SolEuNet Challenge";
	static final String APPLICATIONVERSION="1.0";
	static final String XMLVERSION="1.0";
        static final String XMLENCODING="ISO-8859-1";
	
	static Vector itemSets=new Vector();
	static Vector items=new Vector();	
	static HashMap itemsmap = new HashMap();
	static int nbOfItems=0;
	static int itemSetCount=0;
	
	
	public static ClusNode loadTildeTree(InputStream strm) throws IOException {	
		TildeOutReader reader = new TildeOutReader(strm);
		reader.doParse();
		ClusNode root = reader.getTree();
		reader.close();	
		return root;
	}		

	public static void main(String[] args) {
		try {
			ClusNode root = loadTildeTree(new FileInputStream(args[0]));
					
			//output to pmmlcode.pmml							
			File outFile = new File("pmmlcode.pmml");
			FileOutputStream outFileStream = new FileOutputStream(outFile);
			PrintWriter outStream = new PrintWriter(outFileStream);
			
			//header
	                String header = "<?xml version=\""+XMLVERSION+"\" encoding=\""+XMLENCODING+"\"?>\n<PMML>\n<Header copyright=\""+COPYRIGHT+"\" description=\""+DESCRIPTION+"\">\n<Application name=\""+APPLICATIONNAME+"\" version=\""+APPLICATIONVERSION+"\">\n</Header>\n";
			outStream.write(header);
			
			depthFirstInit(root, outStream);
			printItems(itemSets, outStream);
			printItemsets(itemSets, outStream);			
			depthFirstPrint(root, outStream, 0);
						
			//endfile			
			outStream.write("</PMML>");
					
			//closing outStream						
			outStream.close();
			
		} 
		
		catch (IOException e) {
			System.out.println("Error: "+e.getMessage());
		}
		
	} //main
	
	
//depth-first traversal of the tree to initialize
	static void depthFirstInit(ClusNode $currentNode, PrintWriter $outStream) throws IOException {
	try{		
		String test = $currentNode.getTest().toString();
		MStreamTokenizer tokens = MStreamTokenizer.createStringParser(test);		
		tokens.setCharTokens(",[]():");
		
		while (tokens.hasMoreTokens()) {
			String name = tokens.readToken();

			Vector TempVector = new Vector();			
			Itemset TempItemSet = new Itemset(itemSets.size(), name, 0, TempVector);
							
			tokens.readChar('(');
				
			do {				
			String arg = tokens.readToken();
						
						
			Item TempItem = (Item)itemsmap.get(arg);
			if (TempItem == null) {
				TempItem = new Item(nbOfItems, false, arg);
				itemsmap.put(arg, TempItem);
				items.add(TempItem);
				nbOfItems++;
			}
			
			TempItemSet.addItemRef(TempItem);			
			
			
			} while(tokens.isNextToken(','));
			
			itemSets.add(TempItemSet);
								
		tokens.readChar(')');
		tokens.readChar('?');
		tokens.isNextToken(',');			

		}
		
		
	       	for(int idx = 0; idx < $currentNode.getNbChildren(); idx++ ) {
		
		ClusNode $childNode= (ClusNode)$currentNode.getChild(idx);
		if (!$childNode.atBottomLevel()) depthFirstInit($childNode, $outStream);
		
					
        	}
	} //try

	catch (IOException e) {
			System.out.println("Error: "+e.getMessage());
	}
					
	} //depthFirstInit
	
	//***************************************************************************************************
	
	
	static void printItems(Vector $itemSets, PrintWriter $outStream) {

		for (int i = 0; i < items.size(); i++) {
		
			((Item)items.elementAt(i)).print($outStream);
		}
	
	}
	
		
	//***************************************************************************************************
	
	
	static void printItemsets(Vector $itemSets, PrintWriter $outStream) {
	
	int nbOfItemSets=$itemSets.size();
	int counter=nbOfItemSets;
	int pointer=0;
	
	while (counter>0) {
	
	((Itemset)$itemSets.elementAt(pointer)).print($outStream);
		
	counter--;
	pointer++;	
	}
			
	}
	
	
	//***************************************************************************************************
	
	//depth-first traversal of the tree to print to PMML file
	
	static void depthFirstPrint(ClusNode $currentNode, PrintWriter $outStream, int tabs) throws IOException {
	try{		
		String test = $currentNode.getTest().toString();
		MStreamTokenizer tokens = MStreamTokenizer.createStringParser(test);		
		tokens.setCharTokens(",[]():");
		
		while (tokens.hasMoreTokens()) {
			String name = tokens.readToken();

			int counter1 = tabs;
			
			while (counter1>0) {
			$outStream.write("\t");
			counter1--;
			}
			
			ClassificationStat stat = (ClassificationStat)$currentNode.getTotalStat();
			System.out.println(stat);
			$outStream.write("<Node recordCount=\""+(int)stat.getTotalWeight()+"\" score=\""+stat.getPredictedClassName(0)+"\">\n");
			
			for(int idx = 0; idx < stat.getNbClasses(0); idx++ ) {

			int counter2 = tabs;
			
			while (counter2>0) {
			$outStream.write("\t");
			counter2--;
			}
			
			$outStream.write("<ScoreDistribution value=\""+stat.getClassName(0,idx)+"\" recordCount=\""+(int)stat.getCount(0,idx)+"\" />\n");
			
			}
			
			tokens.readChar('(');
				
			do {				
			String arg = tokens.readToken();
			//$outStream.write("  Arg "+arg);
			} while(tokens.isNextToken(','));
			
			int counter3 = tabs;
			
			while (counter3>0) {
			$outStream.write("\t");
			counter3--;
			}
			
			$outStream.write("<ItemsetRef itemsetRef=\""+itemSetCount+"\" />\n");
			itemSetCount++;
											
		tokens.readChar(')');
		tokens.readChar('?');
		tokens.isNextToken(',');			

		} //no more tokens
		
		
		$outStream.write("\n");
		
		int newTab = tabs+1;
			
	       	for(int idx = 0; idx < $currentNode.getNbChildren(); idx++ ) {
		
		ClusNode $childNode= (ClusNode)$currentNode.getChild(idx);
		if (!$childNode.atBottomLevel()) depthFirstPrint($childNode, $outStream, newTab);
		
			if (idx==$currentNode.getNbChildren()-1) {
		
			int counter4 = tabs;
		
			while (counter4>0){
			$outStream.write("\t");
			counter4--;
			}
		 
		
			$outStream.write("</Node>\n");
						
			}
					
        	}
	} //try

	catch (IOException e) {
			System.out.println("Error: "+e.getMessage());
	}
					
	}	
	
} //class
