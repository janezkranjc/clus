
[General]
RandomSeed = 0
         % Initializes the random number generator, used e.g. for
         % computing the cross-validation partition.

[Data]
File = iris.arff
         % Sets the training data file
TestSet = None
         % Sets the test data file (if it is a valid file name)
         % or test set proportion (if it is a number, e.g., 0.33)
PruneSet = None
         % Sets the prune data file (if it is a valid file name)
         % or prune set proportion (if it is a number, e.g., 0.33)
XVal = 10
         % Sets the number of folds for cross-validation
         % To perform cross-validation, run: clus -xval appname

[Attributes]
Target = 5
         % Sets the index of the target attribute
         % (Run clus -info appname to list all attributes.)
Disable = 4
         % Disables some attributes (e.g., "5,7-8")
Key = None
         % Sets the index of the key attribute
Weights = Normalize
         % Normalize numeric attributes

[Model]
MinimalWeight = 2.0
         % Do not generate splits with less than 2 examples in
         % one of the subtrees

[Tree]
FTest = 1.0
         % Sets the f-test stopping criterion for multi-objective
         % regression
MaxDepth = Infinity
         % Stop building the tree at the given depth
ConvertToRules = AllNodes
         % Convert the tree to a set of rules

[Constraints]
Syntactic = None
         % Sets the file with syntactic constraints
         % (i.e., a partial tree)
MaxSize = Infinity
         % Sets the maximum size for Garofalakis pruning
MaxError = Infinity
         % Sets the maximum error for Garofalakis pruning

[Output]
AllFoldModels = Yes
         % Output the model built in each of the cross-validation folds
AllFoldErrors = No
         % Output error measures for each fold
TrainErrors = Yes
         % Output training error measures
UnknownFrequency = No
         % Show in each node of the tree the proportion of examples
         % that had a missing value for the test stored in the node
BranchFrequency = No
         % Show in each node, the proportion of examples for which
         % the test stored in the node succeeds
WritePredictions = {Train,Test}
         % Write the predictions obtained on the test set to a
         % file

[Beam]
SizePenalty = 0.1
         % Sets the size penalty parameter used in the beam heuristic
BeamWidth = 10
         % Sets the width of the beam (number of trees)
MaxSize = Infinity
         % Sets the maximum size constraint

