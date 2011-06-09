#!/bin/bash

# python2.5 ./google_appengine/dev_appserver.py --datastore_path=./dev_appserver.datastore -a 0.0.0.0 yalt
python2.5 ./google_appengine/dev_appserver.py --datastore_path=./tmp.db -a 0.0.0.0 yalt
