#!/bin/bash
THE_CLASSPATH=/home/ludab/aggregator/agr-webapp/target/agg.jar

for i in `ls /home/ludab/aggregator/agr-webapp/target/agg/WEB-INF/lib/*.jar`

  do
  THE_CLASSPATH=${THE_CLASSPATH}:${i}
done

/usr/bin/java -classpath $THE_CLASSPATH gov.lanl.agg.webapp.Main  -port=9999   -sdir=/home/ludab/wrapper/aggregator_03/webapp 

echo 
