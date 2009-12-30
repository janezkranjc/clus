[Tree]
PruningMethod = None
FTest = 1.0
AlternativeSplits = true
Heuristic = GeneticDistance

[Attributes]
Key = 1
Descriptive = 2-897
Target = 2-897
Weights = 1
ReduceMemoryNominalAttrs = yes

[Model]
MinimalWeight = 1

[Data]
File = chimp.arff

[Output]
TrainErrors = No
PrintModelAndExamples = true

[Phylogeny]
DistanceMeasure = JC
OptimizationCriterion = BranchLengths
Sequence = DNA