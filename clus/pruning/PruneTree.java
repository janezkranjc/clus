package clus.pruning;

import clus.data.rows.RowData;
import clus.main.*;
import clus.util.ClusException;

public abstract class PruneTree {

	public abstract void prune(ClusNode node) throws ClusException;
	
	public void setTrainingData(RowData data) {
	}
	
	public int getNbResults() {
		return 1;
	}
	
	public void prune(int result, ClusNode node) throws ClusException {
		prune(node);
	}
}
