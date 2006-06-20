#!/bin/sh
export WEKA_DIR=/home/elisa/ACE-CLUS/Weka/
export CLUS_DIR=/home/elisa/ACE-CLUS/Clus
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$WEKA_DIR/weka.jar:$CLUS_DIR/commons-math-1.0.jar" clus/Clus.java

