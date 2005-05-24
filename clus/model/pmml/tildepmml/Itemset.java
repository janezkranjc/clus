package clus.model.pmml.tildepmml;

import java.util.*;
import java.io.*;


public class Itemset{
	
private int id;
private String predicate;
private int numberOfItems; //nb of itemrefs
private Vector itemRefs; //vector van items
private String displayTerm=""; 

	public Itemset(int $id, String $predicate) {	
		id = $id;
		predicate = $predicate;
		numberOfItems = 0;
		itemRefs = new Vector();
	}

	public Itemset(int $id, String $predicate, int $numberOfItems, Vector $itemRefs) {
	
	id=$id;
	predicate=$predicate;
	numberOfItems=$numberOfItems;
	itemRefs=$itemRefs;

	}
	
	public Itemset(int $id, String $predicate, int $numberOfItems, Vector $itemRefs, String $displayTerm) {
	
	id=$id;
	predicate=$predicate;
	numberOfItems=$numberOfItems;
	itemRefs=$itemRefs;
	displayTerm=$displayTerm;

	}
	
	public void setId(int $id) {
	
	id=$id;
		
	}
	
	public void setPredicate(String $predicate) {
	
	predicate=$predicate;
	
	}
	
	public void setNumberOfItems(int $numberOfItems) {

	numberOfItems=$numberOfItems;
	
	}
	
/*
	public void increaseNumberOfItems() {
	
	numberOfItems++;
		
	}
*/
	
	public void addItemRef(Item itemObject) {
		itemRefs.add(itemObject);
		numberOfItems++;
	}
	
	
	public int getId() {
	
	return id;
	
	}
	
	public String getPredicate() {
	
	return predicate;
	
	}
	
	public int getNumberOfItems() {
	
	return numberOfItems;
	
	}
	
	public Vector getItemRefs() {
	
	return itemRefs;
	
	}
	
	public String getDisplayTerm() {
	
	return displayTerm;
	
	}
	
	public void print(PrintWriter outStream) {
		int nbItems = itemRefs.size();
		outStream.println("<Itemset id=\""+id+"\" predicate=\""+predicate+"\" numberOfItems=\""+nbItems+"\">");

		for (int i = 0; i < nbItems; i++) {
			Item item = (Item)itemRefs.elementAt(i);
			outStream.println(" <ItemRef itemRef=\""+item.getId()+"\" position=\""+i+"\"/>");
		}	
		
		if (displayTerm!="") {
			outStream.println(" <DisplayTerm value=\""+displayTerm+"\"/>");
		}
		
		outStream.println("</Itemset>");
		outStream.println();
	}
	
	
}
