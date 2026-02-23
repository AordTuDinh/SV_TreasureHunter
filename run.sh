#!/bin/bash
cd `dirname $0`

proc_name="gameserver1"
pkill -f "$proc_name"

export MAVEN_OPTS=${MAVEN_OPTS}" -Duser.timezone=Asia/Ho_Chi_Minh"

jrebel=false
if [[ "$jrebel" = true ]] ; then
    JREBEL_JAR="/opt/jrebel/jrebel.jar"
    export MAVEN_OPTS=${MAVEN_OPTS}" -javaagent:$JREBEL_JAR -noverify"
    export MAVEN_OPTS=${MAVEN_OPTS}" -Drebel.spring_data_plugin=true"
fi

hotswap=true
if [[ "$hotswap" = true ]] ; then
    HOTSWAP_JAR="/opt/java/jbr/lib/hotswap-agent.jar"
    export MAVEN_OPTS=${MAVEN_OPTS}" -XX:+AllowEnhancedClassRedefinition"
    export MAVEN_OPTS=${MAVEN_OPTS}" -XX:HotswapAgent=fatjar"
fi

/usr/bin/mvn exec:java -Dexec.mainClass="game.dragonhero.server.App" -Dexec.args="$proc_name"

#screen -fn -dm -U -S ${proc_name} /opt/maven/bin/mvn exec:java -Dexec.mainClass="game.dragonhero.server.App" -Dexec.args="$proc_name"
#screen -ls
#screen -r ${proc_name}
#sed -i -e 's/\r$//' run.sh