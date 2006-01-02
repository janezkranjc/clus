[Data]
TestSet = iris.arff
PruneSet = None

[Constraints]
Syntactic = None
MaxSize = 12
MaxError = 0.01

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
ConvertToRules = Yes
PruningMethod = None

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
PrintRuleWiseErrors = Yes

[Numeric]
FTest = 1.0
MultiScore = None

[Nominal]
GainRatio = No
MEstimate = 1.0

[Constraints]
Syntactic = iris.constr
MaxError = 0.34

[Output]
WriteTestSetPredictions = Yes
PrintModelAndExamples = Yes
% ShowInfo = {Count, Index}

