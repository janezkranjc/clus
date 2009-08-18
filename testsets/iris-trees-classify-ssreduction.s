
% *RUN*
% clus -xval %f.s
% cdiff %f.out orig/%f.out.orig

% *CLEAN*
% rm -f %f.out %f.xval %f.model %f.test.pred.arff %f.train.*.pred.arff %f.train.*.id

[Data]
File = ../data/iris/iris.arff

[Tree]
Heuristic = SSReduction

[Output]
TrainErrors = Yes
UnknownFrequency = Yes
BranchFrequency = Yes

