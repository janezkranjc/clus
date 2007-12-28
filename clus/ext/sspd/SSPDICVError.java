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

package clus.ext.sspd;

import java.io.*;

import clus.error.*;
import clus.algo.tdidt.ClusNode;
import clus.data.rows.*;
import clus.main.*;
import clus.model.ClusModel;
import clus.model.test.*;

public class SSPDICVError extends ClusError {

	public final static long serialVersionUID = Settings.SERIAL_VERSION_ID;
	
	protected double m_Value;
	protected SSPDDistance m_Dist;
	
	public SSPDICVError(ClusErrorList par, SSPDDistance dist) {
		super(par);
		m_Dist = dist;
	}
	
	public void computeRecursive(ClusNode node, RowData data) {
		int nb = node.getNbChildren();
		if (nb == 0) {
			double variance = SSPD.computeSSPDVariance(m_Dist, data);
			double sumweight = data.getSumWeights();
			m_Value += sumweight * variance;
		} else {
			NodeTest tst = node.getTest();
			for (int i = 0; i < node.getNbChildren(); i++) {
				ClusNode child = (ClusNode)node.getChild(i);
				RowData subset = data.applyWeighted(tst, i);
				computeRecursive(child, subset);
			}
		}
	}
	
	public void compute(RowData data, ClusModel model) {
		if (model instanceof ClusNode) {
			ClusNode tree = (ClusNode)model;
			computeRecursive(tree, data);
			m_Value /= data.getSumWeights();
		}
	}
	
	public void showModelError(PrintWriter wrt, int detail) {
		wrt.println("SSPD-ICV: "+m_Value);
	}
	
	public ClusError getErrorClone(ClusErrorList par) {
		return new SSPDICVError(getParent(), m_Dist);
	}
	
	public String getName() {
		return "SSPDICV";		
	}
}
