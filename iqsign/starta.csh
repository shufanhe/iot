#! /bin/csh -f

pm2 stop iqsignapp

cat < /dev/null > servera.log

pm2 start --log servera.log --name iqsignapp runa.sh
