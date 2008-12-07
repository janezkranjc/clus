[Data]
File = church_FUN.trainvalid.arff.zip
TestSet = church_FUN.test.arff.zip

[Tree]
ConvertToRules = No
FTest = 0.1
% FTest = 0.01

[Model]
MinimalWeight = 5.0

[Attributes]
ReduceMemoryNominalAttrs = yes

[Hierarchical]
Type = TREE
WType = ExpAvgParentWeight
HSeparator = /

[Ensemble]
EnsembleMethod = Boosting
Iterations = 100

[Output]
WritePredictions = {Test}
