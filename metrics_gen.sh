#!/usr/bin/env bash

metric_dest_name=localhost
metric_dest_port=2003
RANDOM=$$

log()
{
    echo -e "`date` $1" >&1
}

for i in `seq 1000000`
do
  pod1_metric="pod1.uconsole.test.data $RANDOM `date +%s`"
  pod2_metric="pod2.uconsole.test.data $RANDOM `date +%s`"
  log "$pod1_metric"
  log "$pod2_metric"
  sleep 60
  echo -e ${pod1_metric} | nc -u -w 1 ${metric_dest_name} ${metric_dest_port}
done
exit 0
