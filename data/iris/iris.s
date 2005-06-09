
[Attributes]
Target = 5

[Constraints]
Syntactic = iris.constr

% [Beam]
% BeamWidth = 20
% BeamBestN = 20
% MaxSize = 3

[General]
Verbose = 1
RandomSeed = 0
XVal = 10

[Data]
File = iris.arff
TestSet = None
PruneSet = None

[Attributes]
Target = 5
Disable = None
Key = None
Weights = Normalize

[Model]
MinimalWeight = 2.0
ParamTuneNumberFolds = 10

[Tree]
Heuristic = Default
MaxDepth = Infinity
BinarySplit = Yes
PruningMethod = None
ConvertToRules = No

[Rules]
Ordered = Yes

[Output]
AllFoldModels = Yes
AllFoldErrors = No
TrainErrors = Yes
UnknownFrequency = No
BranchFrequency = No

[Numeric]
FTest = 1.0
Normalize = Yes
MultiScore = None

[Nominal]
GainRatio = No
MEstimate = 1.0

[Constraints]
Syntactic = iris.constr
MaxSize = Infinity
MaxError = Infinity
