#!/bin/sh
export CLUS_DIR="."
javac -d "$CLUS_DIR/bin" -cp "$CLUS_DIR/.:$CLUS_DIR/jars/commons-math-1.0.jar:$CLUS_DIR/jars/jgap.jar" clus/Clus.java
