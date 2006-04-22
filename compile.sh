#!/bin/sh
export WEKA_DIR=/home/jan/NoCsBack/weka-3-4
export CLUS_DIR=/home/jan/NoCsBack/Clus
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$WEKA_DIR/weka.jar:$CLUS_DIR/commons-math-1.0.jar" clus/Clus.java

