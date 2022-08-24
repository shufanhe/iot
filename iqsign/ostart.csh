#! /bin/csh -f

pm2 stop iqsign

cat < /dev/null > oserver.log

pm2 start --log oserver.log --name oauth runo.sh
