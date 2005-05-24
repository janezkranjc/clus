
package clus.gui.statvis;

import jeans.graph.swing.drawable.*;
import jeans.graph.plot.*;

import java.awt.*;

import clus.statistic.*;

public class ClassStatVis implements ClusStatVisualizer, MDistrInfo {

	public final static Color[] m_Colors = {Color.red, Color.yellow, Color.cyan, Color.blue, Color.green, Color.white, Color.black};
	
	ClassificationStat m_Stat;
	
	public ClassStatVis() {
	}
	
	public ClassStatVis(ClassificationStat stat) {
		m_Stat = stat;
	}	

	public Drawable createInstance(ClusStatistic stat) {
		ClassStatVis sv = new ClassStatVis((ClassificationStat)stat);
		return new DrawableDistrGraph(0, 0, sv, (float)stat.m_SumWeight);
	}
	
	public int getNbBins() {
		return m_Stat.getNbClasses(0);
	}
	
	public float getBinCount(int idx) {
		return (float)m_Stat.getCount(0, idx);	
	}
	
	public Color getBinColor(int idx) {
		return m_Colors[idx % m_Colors.length];
	}
	
	public static Color getBinColorStatic(int idx) {
		return m_Colors[idx % m_Colors.length];
	}	
}
