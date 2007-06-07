package clus.ext.ilevelc;

import java.io.PrintWriter;
import java.util.*;

import clus.data.rows.*;
import clus.data.type.*;
import clus.error.*;
import clus.main.*;
import clus.statistic.*;

public class ILevelCRandIndex extends ClusError {

	protected double m_RandIndex;
	protected boolean m_Invalid;
	protected boolean m_IsComputed;
	protected NominalAttrType m_Attr;
	protected ArrayList m_Exs = new ArrayList();
	protected int m_Count;
	
	public ILevelCRandIndex(ClusErrorParent par, NominalAttrType nom) {
		super(par, 1);
		m_Attr = nom;
	}
	
	public double computeRandIndex() {
		int a = 0;
		int b = 0;
		int nbex = m_Exs.size();
		if (nbex == 0) return 0.0;
		for (int i = 0; i < nbex; i++) {
			int[] ti = (int[])m_Exs.get(i);
			for (int j = i+1; j < nbex; j++) {
				int[] tj = (int[])m_Exs.get(j);
				if (ti[0] == tj[0] && ti[1] == tj[1]) a++;
				if (ti[0] != tj[0] && ti[1] != tj[1]) b++;
			}
		}
		double rand = 1.0 * (a+b) / (nbex*(nbex-1)/2);
		System.out.println("Rand = "+rand+" (nbex = "+nbex+")");
		return rand;
	}
	
	public boolean isInvalid() {
		return m_Invalid;
	}
	
	public double getRandIndex() {
		if (!m_IsComputed) {
			m_RandIndex = computeRandIndex();
			m_IsComputed = true;
		}
		return m_RandIndex;		
	}	
	
	public void reset() {
		m_IsComputed = false;
		m_Exs.clear();
	}
		
	public void add(ClusError other) {
		ILevelCRandIndex ri = (ILevelCRandIndex)other;
		if (!ri.isInvalid()) {
			m_RandIndex += ri.getRandIndex();
			m_Count++;
		}
	}
	
	public void addInvalid(DataTuple tuple) {
		m_Invalid = true;
	}
	
	public void addExample(DataTuple tuple, ClusStatistic pred) {
		m_IsComputed = false;
		int[] store = new int[2];
		ILevelCStatistic ilstat = (ILevelCStatistic)pred;
		store[0] = tuple.getIntVal(m_Attr.getArrayIndex());		
		store[1] = ilstat.getClusterID();
		m_Exs.add(store);
	}
		
	public double getModelErrorComponent(int i) {
		if (m_Count > 0) {
			return m_RandIndex / m_Count;
		}
		return getRandIndex();
	}
	
	public void showModelError(PrintWriter out, int detail) {
		if (isInvalid()) {
			out.println("?");
		} else if (m_Count > 0) {
			out.println(String.valueOf(1.0*m_RandIndex / m_Count)+" (cnt = "+m_Count+")");
		} else {
			out.println(getRandIndex());
		}
	}	

	public ClusError getErrorClone(ClusErrorParent par) {
		return new ILevelCRandIndex(getParent(), m_Attr);
	}	

	public String getName() {
		return "Rand index";
	}
}
