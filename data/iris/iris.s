[Data]
TestSet = None
PruneSet = None

[Attributes]
Target = 5
Weights = Normalize
ClusteringWeights =
  TargetWeight = 0.5
  NonTargetWeight = 0.5
  NumericWeight = 0.5
  NominalWeight = 0.5

[Model]
MinimalWeight = 2.0

[Tree]
Heuristic = Default
ConvertToRules = No

[Rules]
OrderedRules = Yes
CoveringMethod = Standard
CoveringWeight = 0.9
ComputeCompactness = No
CompactnessWeights = 
  TargetWeight = 0.5
  NonTargetWeight = 0.5
  NumericWeight = 0.5
  NominalWeight = 0.5
RandomRules = No

[Numeric]
FTest = 1.0
MultiScore = None

[Nominal]
GainRatio = No
MEstimate = 1.0
