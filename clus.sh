#!/bin/sh
export WEKA_DIR=$HOME/weka-3-4-10
export CLUS_DIR=$HOME/Clus/bin
java -Xmx800m -cp "$HOME/Clus/.:$CLUS_DIR/.:$WEKA_DIR/weka.jar:$CLUS_DIR/commons-math-1.0.jar"  -Djava.library.path=$HOME/Clus/. clus.Clus $*
