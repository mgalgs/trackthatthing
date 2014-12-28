from telnetlib import Telnet
from random import random
from time import sleep

tn = Telnet('localhost', 5554)
lon = -117.139121 + random()
lat = 32.976918 + random()
while True:
    ret = tn.read_until('OK')
    if not ret.endswith('OK'):
        print "Didn't get OK... Got:", ret
        print "bailing..."
        break
    fix = 'geo fix %g %g' % (lon, lat)
    print 'sending fix: ', fix
    tn.write(fix + '\n')
    lon += random() / 1000
    lat += random() / 1000
    sleep(3)
tn.close()
