#!/bin/bash
#
# Usage
#   sh ./runmvn.sh
#


TACAA_HOME=`pwd`
echo $TACAA_HOME
echo $CLASSPATH

# mvn install -Dmaven.test.skip=true -Pdev
mvn package -Dmaven.test.skip=true -Pdev