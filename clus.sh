#!/bin/sh
export WEKA_DIR=$HOME/weka-3-4
export CLUS_DIR=$HOME/Clus
java -Xmx300000000 -cp "$CLUS_DIR/.:$WEKA_DIR/weka.jar:$CLUS_DIR/commons-math-1.0.jar" clus.Clus $*
