#! /bin/csh -f

pm2 stop iqsign

pm2 start --log server.log --name iqsign run.sh
