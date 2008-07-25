
% *RUN*
% clus %f.s
% cdiff %f.out orig/%f.out.orig

% *CLEAN*
% rm -f %f.out %f.model

[Data]
File = ../data/church_FUN/church_FUN.trainvalid.arff.zip
TestSet = ../data/church_FUN/church_FUN.test.arff.zip

[Tree]
ConvertToRules = No
FTest = 0.125

[Model]
MinimalWeight = 5.0

[Attributes]
ReduceMemoryNominalAttrs = yes

[Hierarchical]
Type = TREE
ClassificationTreshold = [90,95]
WType = ExpAvgParentWeight
HSeparator = /
