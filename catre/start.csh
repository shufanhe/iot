#! /bin/csh -f

pm2 stop catre

rm catre.log

pm2 start --log catre.log --name catre ../bin/catreserver.sh



















