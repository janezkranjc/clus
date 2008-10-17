[Tree]
Heuristic = RDispersionMlt

[Rules]
CoveringWeight = 0.1
RuleAddingMethod = Always
PrintRuleWiseErrors = Yes
CoveringMethod = WeightedError
ComputeDispersion = Yes

[Attributes]
ClusteringWeights = 
  TargetWeight = 1
  NonTargetWeight = 0
  NumericWeight = 1.0
  NominalWeight = 1.0 
Clustering = 1-5

[Output]
AllFoldErrors = Yes

[General]
Verbose = 1

[Data]
XVal = 10
%XVal = iris.arff.1.folds


[Model]
MinimalWeight = 2

