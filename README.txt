
Copyright Notice
----------------

Clus - Software for Predictive Clustering
Copyright (C) 2007
    Katholieke Universiteit Leuven, Leuven, Belgium
    Jozef Stefan Institute, Ljubljana, Slovenia    

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.

Contact information: <http://www.cs.kuleuven.be/~dtai/clus/>.

Running Clus
------------

Clus is based on Java from <http://java.sun.com>. You will need Java 2 version 1.5.x or above to run Clus. Clus is a command line application and should be started from the command prompt (Windows) or a console/terminal (Unix).

To start Clus, enter the command:

java -jar $CLUS_DIR/Clus.jar appfile.s

With $CLUS_DIR/Clus.jar the location of Clus.jar in your Clus distribution and "appfile.s" the name of your settings file. E.g.,

(Windows)

cd C:\Clus\data\iris
java -jar ..\..\Clus.jar iris.s

(Unix)

cd /home/john/Clus/data/iris
java -jar ../../Clus.jar iris.s

The input to Clus is always a settings file (used to set various parameters of the algorithms in Clus) and a data set in Weka's ARFF format. The settings are briefly discussed below.

In the example above, Clus will read its settings from the input file "iris.s" and its input data from the file "iris.arff". It will then construct (with these settings) a classification tree, which it will write to the output file "iris.out".

Try Clus first on the example data sets in the "data" directory.

Note: The above instructions are for running the pre-compiled version of Clus (Clus.jar), which is included with the Clus download. If you have modified and recompiled Clus, or if you are using the CVS version, then you should run Clus in a different way, which is explained below.

Compiling Clus
--------------

Note: The Clus download comes with a pre-compiled version of Clus stored in the file Clus.jar. So, if you just want to run Clus as it is on a data set, then you do not need to compile Clus. You can run it using the above instructions. On the other hand, if you wish to modify the source code of Clus, or if you are using the CVS version, then you will need to compile the source code of Clus. This can be done using the commands below or using the IDE called Eclispe as pointed out in the next section.

(Windows)

cd C:\Clus\src
javac -d "bin" -cp ".;jars\commons-math-1.0.jar;jars\jgap.jar" clus/Clus.java

(Unix)

cd /home/john/Clus
javac -d "bin" -cp ".:jars/commons-math-1.0.jar:jars/jgap.jar" clus/Clus.java

Alternatively, use the "./compile.sh" script provided in the Clus main directory.

Compiling Clus with Eclipse
---------------------------

In Eclips, create a new project for Clus as follows:

Choose File | New | Project.

Select "Java Project" in the dialog box.

In the "New Java Project" dialog box:
   * Enter "Clus" in the field "Project Name".
   * Choose "Create project from existing source" and browse to the location where you unzipped Clus.
     E.g., /home/john/Clus or C:\Clus.
   * Click "Next".
   * Select the "Source" tab of the build settings dialog box.
     Change "Default output folder" (where the class files are generated) to: "Clus/bin".
   * Select the "Libraries" tab of the build settings dialog box.
     Click "Add external jars" and add in this way these three jars:
        Clus/jars/commons-math-1.0.jar
        Clus/jars/jgap.jar
        Clus/jars/weka.jar
   * Click "Finish".

Select the "Navigator" view (Choose Window | Show View | Navigator)
   * Right click the "Clus" project in this view.
   * Select "Properties" from the context menu.
   * Select the "Java Compiler" tab.
   * Set the "Java Compliance Level" to 5.0.

Now Clus should be automatically compiled by Eclipse.

To run Clus from Eclipse:
   * Set as main class "clus.Clus".
   * Set as arguments the name of your settings file (appfile.s).
   * Set as working directory, the directory on the file system where your data set is located.

Running Clus (If you compiled from the source code)
---------------------------------------------------

Note: The following instructions are for running Clus after you compiled it from its source code (using the instructions "Compiling Clus" or "Compiling Clus with Eclipse"). To run the pre-compiled version that is available in the file "Clus.jar", use the instructions at the top of this README file.

(Windows)

cd path\to\appfile.s
java -cp "C:\Clus\bin;C:\Clus\jars\commons-math-1.0.jar;C:\Clus\jars\jgap.jar" clus.Clus appfile.s

(Unix)

cd path/to/appfile.s
java -cp "$HOME/Clus/bin:$HOME/Clus/jars/commons-math-1.0.jar:$HOME/Clus/jars/jgap.jar" clus.Clus appfile.s

Alternatively, use the "./clus.sh" script provided in the Clus main directory after adjusting the line that defines CLUS_DIR at the top of the script.

Settings overview
-----------------

The parameters of the algorithms in Clus can be assigned specific values by means of the settings file "appfile.s". In most cases you only need to include a few settings in this file; all other settings will be set to their default values. If your settings file is named "appfile.s", then Clus will assume the data is in a file called "appfile.arff" (unless specfied otherwise with the "File" setting). The resulting model is written to "appfile.out". The file "appfile.out" also contains the values of all settings; these can be copy & pasted to "dataset.s" and modified. Below is a brief description of the most common settings.

[General]
RandomSeed = 0
         % Initializes the random number generator, used e.g. for
         % computing the cross-validation partition.

[Data]
File = iris.arff
         % Sets the training data file
TestSet = None
         % Sets the test data file (if it is a valid file name)
         % or test set proportion (if it is a number, e.g., 0.33)
PruneSet = None
         % Sets the prune data file (if it is a valid file name)
         % or prune set proportion (if it is a number, e.g., 0.33)
XVal = 10
         % Sets the number of folds for cross-validation
         % To perform cross-validation, run: clus -xval appfile

[Attributes]
Target = 5
         % Sets the index of the target attribute
         % (Run clus -info appfile to list all attributes.)
Disable = 4
         % Disables some attributes (e.g., "5,7-8")
Key = None
         % Sets the index of the key attribute
Weights = Normalize
         % Normalize numeric attributes

[Model]
MinimalWeight = 2.0
         % Do not generate splits with less than 2 examples in
         % one of the subtrees

[Tree]
FTest = 1.0
         % Sets the f-test stopping criterion for multi-objective
         % regression
MaxDepth = Infinity
         % Stop building the tree at the given depth
ConvertToRules = No
         % Convert the tree to a set of rules

[Constraints]
Syntactic = None
         % Sets the file with syntactic constraints
         % (i.e., a partial tree)
MaxSize = Infinity
         % Sets the maximum size for Garofalakis pruning
MaxError = Infinity
         % Sets the maximum error for Garofalakis pruning

[Output]
AllFoldModels = Yes
         % Output the model built in each of the cross-validation folds
AllFoldErrors = No
         % Output error measures for each fold
TrainErrors = Yes
         % Output training error measures
UnknownFrequency = No
         % Show in each node of the tree the proportion of examples
         % that had a missing value for the test stored in the node
BranchFrequency = No
         % Show in each node, the proportion of examples for which
         % the test stored in the node succeeds
WritePredictions = {Train,Test}
         % Write the predictions obtained on the test set to a
         % file

[Beam]
SizePenalty = 0.1
         % Sets the size penalty parameter used in the beam heuristic
BeamWidth = 10
         % Sets the width of the beam (number of trees)
MaxSize = Infinity
         % Sets the maximum size constraint
		 