[Data]
File = church_FUN.train.arff.zip
PruneSet = church_FUN.valid.arff.zip
TestSet = church_FUN.test.arff.zip

[Tree]
ConvertToRules = No
PruningMethod = None
FTest = [0.001, 0.005, 0.01, 0.05, 0.1, 1.0]

[Model]
MinimalWeight = 5.0

[Attributes]
ReduceMemoryNominalAttrs = yes

[Hierarchical]
Type = TREE
WType = ExpAvgParentWeight
HSeparator = /

