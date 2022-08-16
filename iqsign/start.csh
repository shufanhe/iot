#! /bin/csh -f

pm2 stop iqsign

rm server.log

pm2 start --log server.log --name iqsign run.sh
