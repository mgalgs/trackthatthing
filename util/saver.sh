#!/bin/bash

for line in `cat gpsdata.csv`; do
    lat=$( echo "$line" | cut -f1 -d, );
    lon=$( echo "$line"|cut -f2 -d, );
    `echo "wget -O stuff.txt http://localhost:8080/put?secret=makemove747&lat=$lat&lon=$lon"`;
done
