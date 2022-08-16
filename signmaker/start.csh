#! /bin/csh -f

pm2 stop signmaker

rm signmaker.log

pm2 start --log signmaker.log --name signmaker ../bin/signmakerserver.sh



















