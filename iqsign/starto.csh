#! /bin/csh -f

pm2 delete oauth

cat < /dev/null > servero.log

pm2 start --log servero.log --name oauth runo.sh
