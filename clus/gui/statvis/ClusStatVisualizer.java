
package clus.gui.statvis;

import jeans.graph.swing.drawable.*;

import clus.statistic.*;

public interface ClusStatVisualizer {

	public Drawable createInstance(ClusStatistic stat);

}
