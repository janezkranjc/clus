[Data]
File = church_FUN.trainvalid.arff.zip
TestSet = church_FUN.test.arff.zip

[Tree]
ConvertToRules = No
FTest = 0.1

[Model]
MinimalWeight = 5.0

[Attributes]
ReduceMemoryNominalAttrs = yes

[Hierarchical]
Type = TREE
ClassificationTreshold = [90,95]
WType = ExpAvgParentWeight
HSeparator = /
