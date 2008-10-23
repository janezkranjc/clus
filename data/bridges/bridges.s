[Tree]
Heuristic = RDispersionMlt

[Rules]
CoveringWeight = 0
PrintRuleWiseErrors = Yes
%CoveringMethod = Standard
CoveringMethod = WeightedError
ComputeDispersion = Yes

[Attributes]
ClusteringWeights = 
  TargetWeight = 1
  NonTargetWeight = 0
  NumericWeight = 1.0
  NominalWeight = 1.0 
Target = 9,10,11,12,13
Disable = 1,9-13
Clustering = 2-8,9,10,11,12,13

[Output]
AllFoldErrors = Yes

[General]
Verbose = 1

[Model]
MinimalWeight = 2

[Data]
RemoveMissingTarget = Yes
