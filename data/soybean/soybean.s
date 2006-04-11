
[Attributes]
Target = 36
% Constraints = soybean.constr

[Model]
MinimalWeight = 5

[Tree]
BinarySplit = TRUE

[Beam]
SizePenalty = 0.01
MaxSize = 100
BeamWidth = 100
BeamBestN = 10
PostPrune = FALSE
% FastSearch = FALSE

