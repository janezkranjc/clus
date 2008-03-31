
Copyright Notice
----------------

Clus - Software for Predictive Clustering
Copyright (C) 2007
    Katholieke Universiteit Leuven, Leuven, Belgium
    Jozef Stefan Institute, Ljubljana, Slovenia    

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.

Running Clus
------------

Clus is based on Java from <http://java.sun.com>. You will need Java 2 version 1.5.x or above to run Clus. Clus is a command line application and should be started from a command prompt (Windows) or X-Terminal (Unix).

To start Clus, enter the command:

java -jar /path/to/Clus.jar dataset

With /path/to/Clus.jar the location of Clus.jar in your Clus distribution and "dataset" the name of your settings file. The settings are briefly discussed below.

Try Clus first on the examples in the "data" folder.

E.g., as follows:

cd data/iris
clus -jar ../../Clus.jar iris

More information about Clus is available here:
<http://www.cs.kuleuven.be/~dtai/clus/>

Compiling Clus
--------------

(Windows)

cd C:\Clus\src
javac -cp ".;jars\commons-math-1.0.jar;jars\jgap.jar" clus/Clus.java

(Unix)

cd /home/john/Clus
javac -cp ".:jars/commons-math-1.0.jar:jars/jgap.jar" clus/Clus.java

Alternatively, use the "./compile.sh" script provided in the Clus main directory.

Compiling Clus with Eclipse
---------------------------

In Eclips, create a new project for Clus as follows:

Choose File | New | Project.

Select "Java Project" in the dialog box.

In the "New Java Project" dialog box:
   * Enter "Clus" in the field "Project Name".
   * Choose "Create project from existing source" and browse to the location where you unzipped Clus.
     E.g., /home/jan/Clus-1.3 or C:\Clus-1.3.
   * Click "Next".
   * Select the "Libraries" tab of the build settings dialog box.
     Click "Add external jars" and add in this way these three jars:
        Clus-1.3/jars/commons-math-1.0.jar
        Clus-1.3/jars/jgap.jar
        Clus-1.3/jars/weka.jar
   * Select the "Source" tab and exclude the directory "addon" from the build path.
     That is, exclude "addon/**" from the source code.
   * Click "Finish".

Now Clus should be automatically compiled by Eclipse.

To run Clus from Eclipse:
   * Set as main class "clus.Clus".
   * Set as arguments the name of your settings file (.s).
   * Set as working directory, the directory on the file system where your data set is.

Running Clus (if you compiled from the source code)
---------------------------------------------------

(Windows)

cd path\to\appfile.s
java -cp "C:\Clus;C:\Clus\jars\commons-math-1.0.jar;C:\Clus\jars\jgap.jar" clus.Clus appfile

(Unix)

cd path/to/appfile.s
java -cp "$HOME/Clus:$HOME/Clus/jars/commons-math-1.0.jar:$HOME/Clus/jars/jgap.jar" clus.Clus appfile


Settings overview
-----------------

All settings with their default values are set in "dataset.s" with "dataset" the name of your data set (assuming your data set is "dataset.arff"). The resulting model is written to "dataset.out". The file "dataset.out" also contains the values of all settings; these can be copy & pasted to "dataset.s".

[General]
RandomSeed = 0
 	// Initializes the random number generator, used e.g. for
 	// computing the cross-validation partition.
XVal = 10
 	// Sets the number of folds for cross-validation
 	// To perform cross-validation, run: clus -xval appname

[Data]
File = weather.arff
 	// Sets the training data file
TestSet = None
 	// Sets the test data file (if it is a valid file name)
 	// or test set proportion (if it is a number, e.g., 0.33)
PruneSet = None
 	// Sets the prune data file (if it is a valid file name)
 	// or prune set proportion (if it is a number, e.g., 0.33)

[Attributes]
Target = 5
 	// Sets the index of the target attribute
 	// (Run clus -info appname to list all attributes.)
Disable = 6
 	// Disables some attributes (e.g., "5,7-8")
Key = None
 	// Sets the index of the key attribute
Weights = Normalize
 	// Normalize numeric attributes

[Model]
MinimalWeight = 2.0
 	// Do not generate splits with less than 2 examples in
 	// one of the subtrees

[Tree]
MaxDepth = Infinity
 	// Stop building the tree at the given depth
ConvertToRules = No
 	// Convert the tree to a set of rules

[Numeric]
FTest = 1.0
 	// Sets the f-test stopping criterion for multi-objective
 	// regression

[Constraints]
Syntactic = None
 	// Sets the file with syntactic constraints
 	// (i.e., a partial tree)
MaxSize = Infinity
 	// Sets the maximum size for Garofalakis pruning
MaxError = Infinity
 	// Sets the maximum error for Garofalakis pruning

[Output]
AllFoldModels = Yes
 	// Output the model built in each of the cross-validation folds
AllFoldErrors = No
 	// Output error measures for each fold
TrainErrors = Yes
 	// Output training error measures
UnknownFrequency = No
 	// Show in each node of the tree the proportion of examples
 	// that had a missing value for the test stored in the node
BranchFrequency = No
 	// Show in each node, the proportion of examples for which
 	// the test stored in the node succeeds
WriteTestSetPredictions = No
 	// Write the predictions obtained on the test set to a
 	// file

[Beam]
SizePenalty = 0.1
 	// Sets the size penalty parameter used in the beam heuristic
BeamWidth = 10
 	// Sets the width of the beam (number of trees)
MaxSize = Infinity
 	// Sets the maximum size constraint

