#! /bin/csh -f

pm2 stop iqsignapp

cat < /dev/null > aserver.log

pm2 start --log oserver.log --name iqsignapp runa.sh
