#!/usr/bin/env bash
#
# Rat Mutation Sequence Pipeline
#
. /etc/profile
APPNAME=rat-mutation-sequences-pipeline
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR

EMAILLIST=llamers@mcw.edu,mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAILLIST=llamers@mcw.edu,mtutaj@mcw.edu,jrsmith@mcw.edu,sjwang@mcw.edu
fi

java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/$APPNAME.jar"$@" > run.log 2>&1

mailx -s "[$SERVER] Rat Mutation Sequence Pipeline Run" $EMAILLIST < $APPDIR/logs/summary.log
