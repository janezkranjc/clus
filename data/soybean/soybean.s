
[Data]
Target = 36
% Constraints = soybean.constr

[Tree]
MinimalWeight = 5
BinarySplit = TRUE
SizeConstraintPrune = 20

[Beam]
SizePenalty = 0.01
MaxSize = 100
BeamWidth = 100
BeamBestN = 10
PostPrune = FALSE
% FastSearch = FALSE

[Output]
Rules = TRUE
