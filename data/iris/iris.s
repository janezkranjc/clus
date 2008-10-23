[Tree]
Heuristic = RDispersionMlt

[Rules]
CoveringWeight = 0.1
RuleAddingMethod = Always
PrintRuleWiseErrors = Yes
CoveringMethod = Standard
%CoveringMethod = WeightedError
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
Verbose = 2

[Data]
XVal = 10

[Model]
MinimalWeight = 2

