#!/bin/sh
export CLUS_DIR="."
mkdir -p "$CLUS_DIR/bin"
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$CLUS_DIR/jars/commons-math-1.0.jar:$CLUS_DIR/jars/jgap.jar" clus/Clus.java
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$CLUS_DIR/jars/commons-math-1.0.jar:$CLUS_DIR/jars/jgap.jar" addon/hmc/HMCConvertToSC/HMCConvertToSC.java
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$CLUS_DIR/jars/commons-math-1.0.jar:$CLUS_DIR/jars/jgap.jar" addon/hmc/HMCAverageSingleClass/HMCAverageNodeWiseModels.java
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$CLUS_DIR/jars/commons-math-1.0.jar:$CLUS_DIR/jars/jgap.jar" addon/hmc/HMCAverageSingleClass/HMCAverageSingleClass.java
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$CLUS_DIR/jars/commons-math-1.0.jar:$CLUS_DIR/jars/jgap.jar" addon/hmc/HMCAverageSingleClass/HMCAverageTreeModel.java
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$CLUS_DIR/jars/commons-math-1.0.jar:$CLUS_DIR/jars/jgap.jar" addon/hmc/HMCConvertDAGData/HMCConvertDAGData.java
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$CLUS_DIR/jars/commons-math-1.0.jar:$CLUS_DIR/jars/jgap.jar" addon/hmc/HMCNodeWiseModels/hmcnwmodels/HMCNodeWiseModels.java
