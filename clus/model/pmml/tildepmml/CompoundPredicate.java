
package clus.model.pmml.tildepmml;

import java.util.*;
import java.io.*;

import jeans.util.*;

public class CompoundPredicate {

	protected ArrayList m_ItemSets = new ArrayList();	

	public void addItemset(Itemset set) {
		m_ItemSets.add(set);
	}

	public void print(PrintWriter out, int tabs) {
		StringUtils.printTabs(out, tabs);
		out.println("<CompoundPredicate booleanOperator=\"and\">");
		
		for (int i = 0; i < m_ItemSets.size(); i++) {
			Itemset set = (Itemset)m_ItemSets.get(i);
			
			StringUtils.printTabs(out, tabs);
			out.println("<ItemsetRef itemsetRef=\""+set.getId()+"\" />");
		}
		
		StringUtils.printTabs(out, tabs);		
		out.println("</CompoundPredicate>"); 
	}
}
