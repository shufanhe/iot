#! /bin/bash -f

SRD=$(dirname "${BASH_SOURCE[0]}")
pushd $SRD > /dev/null
SRD1=`pwd`
popd > /dev/null

java -jar $SRD1/catre.jar edu.brown.cs.catre.catmain.CatmainMain












