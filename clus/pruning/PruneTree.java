package clus.pruning;

import clus.data.rows.RowData;
import clus.main.*;

public abstract class PruneTree {

	public abstract void prune(ClusNode node);
	
	public void setTrainingData(RowData data) {
	}
	
	public int getNbResults() {
		return 1;
	}
	
	public void prune(int result, ClusNode node) {
		prune(node);
	}
}
