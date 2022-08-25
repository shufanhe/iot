#! /bin/csh -f

pm2 stop oauth

cat < /dev/null > oserver.log

pm2 start --log oserver.log --name oauth runo.sh
