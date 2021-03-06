[Data]
File = church_FUN.trainvalid.arff.zip
TestSet = church_FUN.test.arff.zip

[Tree]
ConvertToRules = No
% 0.1 is the optimal value according to our HMC paper!
FTest = 0.1

[Model]
MinimalWeight = 5.0

[Attributes]
ReduceMemoryNominalAttrs = yes

[Hierarchical]
Type = TREE
WType = ExpAvgParentWeight
HSeparator = /

[Output]
WritePredictions = {Test}



