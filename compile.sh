#!/bin/sh
export WEKA_DIR=/home/leander/weka-3-4/
export CLUS_DIR=/home/leander/Clus/
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$WEKA_DIR/weka.jar:$CLUS_DIR/commons-math-1.0.jar" clus/Clus.java

