#! /bin/csh -f

set WD = /pro/iot/devices

pm2 stop devices

cat < /dev/null > $WD/devices.log

pm2 start --log $WD/devices.log --name devices $WD/rundevices.sh





