
[Data]
File = multitest_norm.arff
TestSet = multitest_norm.arff

[Attributes]
Weights = Normalize
Disable = 4,5
Target = 4,5

[Data]
PruneSet = .33

[Tree]
PruningMethod = GarofalakisVSB

[Output]
WriteTestSetPredictions = Yes

