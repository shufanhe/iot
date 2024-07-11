#! /bin/csh -f

pm2 delete cedes

cat < /dev/null > cedes.log
cat < /dev/null > alds.log
cat < /dev/null > aldsdata.json

pm2 start --log cedes.log --name cedes run.sh

pm2 save











































