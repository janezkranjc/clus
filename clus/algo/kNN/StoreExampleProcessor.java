package clus.algo.kNN;

import clus.main.ClusModelProcessor;
import clus.main.ClusModel;
import clus.main.ClusSchema;
import clus.data.rows.DataTuple;

import java.io.IOException;

/**
 * This class represents a ModelProcessor that is used for storing tuples in
 * the nodes of a KNNTree which are used for prediction of target values.
 */
public class StoreExampleProcessor extends ClusModelProcessor {

	public void initialize(ClusModel model, ClusSchema schema) throws IOException{
		//does nothing
	}

	public void terminate(ClusModel model) throws IOException{
		//does nothing
	}

	/**
	 * This method adds the given tuple to the given KNNTree.
	 * is used in the correct way by the method applyModelProcessors in ClusNode
	 * Required
	 *		model: instance of KNNTree
	 */
	public void modelUpdate(DataTuple tuple, ClusModel model) throws IOException {
		//this fails when model isn't a KNNTree
		KNNTree tree_node = (KNNTree) model;
		//add the tuple to the node (leaf).
		tree_node.addTuple(tuple);
	}

}