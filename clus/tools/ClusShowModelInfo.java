/*
 * Created on Jun 8, 2005
 */
package clus.tools;

import clus.model.modelio.*;
import clus.main.*;
import clus.error.*;

import java.io.*;

public class ClusShowModelInfo {
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java clus.tools.ClusShowModelInfo somefile.model");
			System.exit(1);
		}
		try {
			PrintWriter output = new PrintWriter(System.out);			
			ClusModelCollectionIO dot_model_file = ClusModelCollectionIO.load(args[0]);
			int nb_models = dot_model_file.getNbModels();
			output.println("This .model file contains: "+nb_models+" models.");
//			for (int i = 0; i < nb_models; i++) {
			for (int i = 0; i < 1; i++) {			
				ClusModelInfo model_info = dot_model_file.getModelInfo(i);
				ClusModel model = model_info.getModel();				
				output.println("Model: "+i+", Name: "+model_info.getName());
				output.println("Size: "+model.getModelSize());
				output.println();
				model.printModel(output);
				ClusErrorParent train_error = model_info.getTrainingError();
				output.println();				
				if (train_error != null) {
					output.println("Training Error:");
					train_error.showError(output);
				} else {
					output.println("No Training Error Available");
				}				
				ClusErrorParent test_error = model_info.getTestError();
				output.println();				
				if (test_error != null) {
					output.println("Testing Error:");
					test_error.showError(output);					
					/* One can also get an individual error, or iterate over all error measures */
					ClusError err = test_error.getErrorByName("Classification Error");
					TargetSchema targets = model_info.getStatManager().getTargetSchema();
					if (err != null) {
						for (int j = 0; j < err.getDimension(); j++) {
							output.println("Target: "+j+" ("+targets.getNomName(j)+") error: "+err.getModelErrorComponent(j));
						}
					}					
				} else {
					output.println("No Testing Error Available");
				}
			}
			output.flush();
		} catch (IOException e) {
			System.err.println("IO Error: "+e.getMessage());
		} catch (ClassNotFoundException e) {
			System.err.println("Error: "+e.getMessage());
		}
	}
}