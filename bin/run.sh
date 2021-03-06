#!/bin/bash
#unset LANG

# $Id$

# Usage with profiler:
usage="run.sh [-a architecture-and-cpu] [-y yjp-home-path] class-to-run\n run.sh -a linux-x86-32 -y /home/ljo/bin/yjp-8.0.13 org.exist.xquery.XPathQueryTest\n\nYou need yjp-8 now to profile since we are using java5+"

index=0
while getopts "a:y:" option
do 
  case $option in
      y ) yjp_home=${OPTARG}; echo ${yjp_home};;
      a ) arch=${OPTARG}; echo ${arch};;
  esac
  index=${OPTIND}
done

if [ $index -gt 0 ]; then
    LD_LIBRARY_PATH="$LD_LIBRARY_PATH:${yjp_home}/bin/${arch}/";
    export LD_LIBRARY_PATH

    shift $(($index - 1)) 
fi

case "$0" in
	/*)
		SCRIPTPATH=$(dirname "$0")
		;;
	*)
		SCRIPTPATH=$(dirname "$PWD/$0")
		;;
esac

# source common functions and settings
source "${SCRIPTPATH}"/functions.d/eXist-settings.sh
source "${SCRIPTPATH}"/functions.d/jmx-settings.sh
source "${SCRIPTPATH}"/functions.d/getopt-settings.sh

if [ -z "$EXIST_HOME" ]; then
    EXIST_HOME_1=`dirname "$0"`
    EXIST_HOME=`dirname "$EXIST_HOME_1"`
fi

if [ ! -f "$EXIST_HOME/conf.xml" ]; then
    EXIST_HOME_1="$EXIST_HOME/.."
    EXIST_HOME=$EXIST_HOME_1
fi

if [ -z "$EXIST_BASE" ]; then
    EXIST_BASE=$EXIST_HOME
fi

check_java_home;

# set java options
set_java_options;

# save LANG
set_locale_lang;

JAVA_ENDORSED_DIRS="$EXIST_HOME"/lib/endorsed

PROFILER_OPTS=-agentlib:yjpagent



if [ "x${yjp_home}" != "x" ]; then
"${JAVA_RUN}" $JAVA_OPTIONS \
	-Djava.endorsed.dirs=$JAVA_ENDORSED_DIRS \
	-Dexist.home=$EXIST_HOME $PROFILER_OPTS \
	-jar ${EXIST_HOME}/start.jar "$@"
else
"${JAVA_RUN}" $JAVA_OPTIONS \
	-Djava.endorsed.dirs=$JAVA_ENDORSED_DIRS \
	-Dexist.home=$EXIST_HOME \
	-jar ${EXIST_HOME}/start.jar "$@"
fi

restore_locale_lang;
