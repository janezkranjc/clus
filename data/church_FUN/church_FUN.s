
% HMC: Average AUROC: 0.5343507405843365, Average AURPC: 0.02855423573980789, Average AURPC (weighted): 0.1294195148197721, Pooled AURPC: 0.1697051282808009
% HSC: Average AUROC: 0.5244585733730156, Average AURPC: 0.02902416018088555, Average AURPC (weighted): 0.1268332350313958, Pooled AURPC: 0.1309560003304250
% SC:  Average AUROC: 0.5278170279406131, Average AURPC: 0.03108210312641788, Average AURPC (weighted): 0.1280239438871272, Pooled AURPC: 0.1275119210526612

[General]
Compatibility = MLJ08

[Data]
File = church_FUN.train.arff.zip
PruneSet = church_FUN.valid.arff.zip
TestSet = church_FUN.test.arff.zip

[Attributes]
ReduceMemoryNominalAttrs = yes

[Hierarchical]
Type = TREE
WType = ExpAvgParentWeight
HSeparator = /
%ClassificationThreshold = [20,50,80]

[Tree]
ConvertToRules = No
%FTest = [0.001,0.005,0.01,0.05,0.1,0.125]
FTest = 0.1

[Model]
MinimalWeight = 5.0

[Output]
TrainErrors = No
ValidErrors = No
ShowModels = {Pruned}
