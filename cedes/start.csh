#! /bin/csh -f

pm2 delete cedes

cat < /dev/null > server.log

pm2 start --log server.log --name cedes run.sh
