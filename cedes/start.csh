#! /bin/csh -f

pm2 delete cedes

cat < /dev/null > cedes.log

pm2 start --log cedes.log --name cedes run.sh
