#! /bin/csh

if ( $sqlrun == mysql ) then
   set DEF_$1 = ""
   set $1 = "ENUM $2"
else if ( $sqlrun == psql) then
   set DEF_$1 = "DROP TYPE IF EXISTS $1; CREATE TYPE $1 AS ENUM $2 ;"
   set $1 = "$1"
else if ($sqlrun == cat ) then
   set DEF_$1 "DROP TYPE IF EXISTS $1; CREATE TYPE $1 AS ENUM $2 ;"
   set $1 = "$1"
endif
