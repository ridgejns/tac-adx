#!/bin/bash
#
# Usage
#   sh ./compile.sh
#

RELV_PATH=`pwd`
echo $RELV_PATH
AGENT_LIB_PATH=${RELV_PATH}/adx-agnet/lib
TARGET_PATH=${RELV_PATH}/target
echo $AGENT_LIB_PATH
echo $TARGET_PATH
echo 

mvn package -Dmaven.test.skip=true -Pdev

echo [Yao Info] --------------------------------------------
echo [Yao Info] copy job
rm -f ${AGENT_LIB_PATH}/adx*.jar
cp ${TARGET_PATH}/adx*.jar ${AGENT_LIB_PATH}
echo -e "\033[32m[Yao Info] copy finish \033[0m"
echo [Yao Info] ---------------------------------------------
echo 
echo -e "\033[33m[Yao Noti] You much change the file 'SampleAdNetwork.java' \033[0m"
echo -e "\033[33m[Yao Noti] The location is ./src/main/java/tau/tac/adx/agents/SampleAdNetwork.java \033[0m"
echo -e "\033[31m[Yao Noti] You have to change the config file befor run the agent. \033[0m"
echo -e "\033[31m[Yao Noti] Please read the \'Readme\' under folder \'adx-agents\'. \033[0m"

 