#!/bin/sh
export WEKA_DIR=$HOME/weka-3-4
export CLUS_DIR=$HOME/Clus
java -Xmx300000000 -cp "c:/Users/Bernard/eclipse_ws/Clus/;c:/Users/Bernard/eclipse_ws/Weka/" clus.Clus $*
