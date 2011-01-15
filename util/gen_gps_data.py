#!/usr/bin/env python

import random

DEFAULT_START = (33.989135,-117.339123)
DEFAULT_LENGTH = 30            # number of points
DEFAULT_LAT_STEP = .0001
DEFAULT_LON_STEP = .0001

length = DEFAULT_LENGTH
(lat,lon) = DEFAULT_START
lat_step = DEFAULT_LAT_STEP
lon_step = DEFAULT_LON_STEP

for i in xrange(length):
    print "%s,%s" % (lat,lon)
    if random.choice(range(5)) == 1:
        lat_step = -lat_step
    if random.choice(range(5)) == 1:
        lon_step = -lon_step
    lat += lat_step
    lon += lon_step

