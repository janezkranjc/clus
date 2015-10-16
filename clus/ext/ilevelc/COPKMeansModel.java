/*************************************************************************
 * Clus - Software for Predictive Clustering                             *
 * Copyright (C) 2007                                                    *
 *    Katholieke Universiteit Leuven, Leuven, Belgium                    *
 *    Jozef Stefan Institute, Ljubljana, Slovenia                        *
 *                                                                       *
 * This program is free software: you can redistribute it and/or modify  *
 * it under the terms of the GNU General Public License as published by  *
 * the Free Software Foundation, either version 3 of the License, or     *
 * (at your option) any later version.                                   *
 *                                                                       *
 * This program is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 * GNU General Public License for more details.                          *
 *                                                                       *
 * You should have received a copy of the GNU General Public License     *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. *
 *                                                                       *
 * Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.         *
 *************************************************************************/

package clus.ext.ilevelc;

import java.io.PrintWriter;
import java.util.HashSet;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import clus.algo.tdidt.*;
import clus.data.rows.*;
import clus.data.type.ClusAttrType;
import clus.main.*;
import clus.statistic.*;

public class COPKMeansModel extends ClusNode {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;

	protected int m_K, m_Iterations, m_CSets, m_AvgIter;
	protected boolean m_Illegal;
	protected double m_RandIndex;
	protected COPKMeansCluster[] m_Clusters;

	public void setK(int k) {
		m_K = k;
	}

	public int getModelSize() {
		return m_K;
	}

	public ClusStatistic predictWeighted(DataTuple tuple) {
		if (m_Illegal) {
			return null;
		} else {
			int best_cl = -1;
			double min_dist = Double.POSITIVE_INFINITY;
			for (int j = 0; j < m_K; j++) {
				double dist = m_Clusters[j].computeDistance(tuple);
				if (dist < min_dist) {
					best_cl = j;
					min_dist = dist;
				}
			}
			return m_Clusters[best_cl].getCenter();
		}
	}

	public void printModel(PrintWriter wrt, StatisticPrintInfo info) {
		wrt.println("COPKMeans("+m_K+", iter = "+m_Iterations+", max = "+m_AvgIter+", csets = "+m_CSets+")");
		if (m_Illegal) {
			wrt.println("   Illegal");
		} else {
			for (int j = 0; j < m_K; j++) {
				wrt.println("  "+m_Clusters[j].getCenter().getString(info));
			}
		}
	}
	@Override
	public Element printModelToXML(Document doc, StatisticPrintInfo info,
			RowData examples)
	{		
		Element kmeans = doc.createElement("COPKMeans");
		Attr clusters = doc.createAttribute("clusters");
		Attr iter = doc.createAttribute("iter");
		Attr max = doc.createAttribute("max");
		Attr csets = doc.createAttribute("csets");
		kmeans.setAttributeNode(clusters);
		kmeans.setAttributeNode(iter);
		kmeans.setAttributeNode(max);
		kmeans.setAttributeNode(csets);
		clusters.setValue(m_K+"");
		iter.setValue(m_Iterations+"");
		max.setValue(m_AvgIter+"");
		csets.setValue(m_CSets+"");
		
		if(!m_Illegal)
		{
			Element stats = doc.createElement("ILevelCStat");
			kmeans.appendChild(stats);
			for (int j = 0; j < m_K; j++) {
				Element predict = m_Clusters[j].getCenter().getPredictElement(doc);
				stats.appendChild(predict);
			}	
		}
		else
		{
			Element unkn = doc.createElement("UnknownStat");
			kmeans.appendChild(unkn);
		}
		
		if (examples!=null && examples.getNbRows()>0)
		{
			Element examplesEl = doc.createElement("Examples");				
			kmeans.appendChild(examplesEl);
			Attr n_examples = doc.createAttribute("examples");
			n_examples.setValue(examples.getNbRows()+"");
			examplesEl.setAttributeNode(n_examples);
			String[] attributes = examples.getSchema().toString().split(",");
			ClusAttrType[] targets = examples.getSchema().getTargetAttributes();
			HashSet<Integer> targetIds = new HashSet<Integer>();
			for(ClusAttrType t:targets)
			{
				targetIds.add(t.getIndex());
			}
			for(int i=0;i<examples.getNbRows();i++)
			{					
				Element example = doc.createElement("Example");
				examplesEl.appendChild(example);					
				String[] values = examples.getTuple(i).toString().split(",");
				
				for(int j=0;j<values.length;j++)
				{
					Element attribute = doc.createElement("Attribute");
					example.appendChild(attribute);
					Attr name = doc.createAttribute("name");
					attribute.setAttributeNode(name);
					Attr type = doc.createAttribute("type");
					attribute.setAttributeNode(type);
					if(targetIds.contains(j))
					{
						//target attribute
						type.setValue("class");
					}
					else
					{
						//regular attribute
						type.setValue("regular");
					}
					name.setValue(attributes[j]+"");
					attribute.setTextContent(values[j]);
				}					
			}				
		}
		return kmeans;
	}

	public String getModelInfo() {
		if (m_Illegal) {
			return "Rand Index = ?";
		} else {
			return "Rand Index = "+m_RandIndex;
		}
	}

	public void setCSets(int sets) {
		m_CSets = sets;
	}

	public int getCSets() {
		return m_CSets;
	}

	public void setAvgIter(int avg) {
		m_AvgIter = avg;
	}

	public void setIllegal(boolean illegal) {
		m_Illegal = illegal;
	}

	public void setRandIndex(double value) {
		m_RandIndex = value;
	}

	public void setClusters(COPKMeansCluster[] clusters) {
		m_Clusters = clusters;
	}

	public void setIterations(int i) {
		m_Iterations = i;
	}

	public int getIterations() {
		return m_Iterations;
	}

	public boolean isIllegal() {
		return m_Illegal;
	}
}
