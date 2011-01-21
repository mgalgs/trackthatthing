import time
import urllib
import urllib2
import json
import android
from itertools import count
from pprint import pprint

DEFAULT_SECRET = 'drive skin 226'
DEFAULT_SERVER = 'http://firsthome.homelinux.org:8080'
DEFAULT_UPDATE_RATE = 10

def event_loop(droid):
  for i in xrange(10):
    e = droid.receiveEvent()
    if e.result is not None:
      return e
    time.sleep(2)
  return None

def get_location(droid):
  droid.startLocating()
  try:
    return event_loop(droid)
  finally:
    droid.stopLocating()

def get_lat_lon(droid):
  g = get_location(droid)
  if g is None: return None

  print 'the result'
  print g
  print
  data = g.result['data']

  # get the best provider:
  providers = ['gps', 'network'] # list them here in order of precedence!
  provider = None
  for p in providers:
    if p in data.keys():
      provider = p
      break
  if provider is None:
    print "Couldn't get a provider from this thing:"
    print data
    return None

  # return the data:
  return {'lat':g.result['data'][provider]['latitude'],
          'lon':g.result['data'][provider]['longitude'],
          'speed':g.result['data'][provider]['speed'],
          'acc':g.result['data'][provider]['accuracy']}

def put_loc(droid,base_url,params):
  'params should contain lat,lon,acc, and secret'
  
  url = base_url + "/put?" + urllib.urlencode(params)
  req = urllib2.Request(url)
  opener = urllib2.build_opener()
  try:
    f = opener.open(req)
    j = json.load(f)
    print 'got server response from url %s: %s' % (url, str(j))
  except:
    print 'yaltdroid broke. url was: ' + url
    print 'req was: ' + str(req)
    droid.notify('yaltdroid broke.', 'url was: '+url)

if __name__ == "__main__":
  droid = android.Android()

  # set up some parameters:
  params = {'secret':DEFAULT_SECRET,
            'base_url':DEFAULT_SERVER,
            'update_freq':DEFAULT_UPDATE_RATE}
  secret = droid.dialogGetInput('Secret', 'Please enter your secret', params['secret'])
  base_url = droid.dialogGetInput('Base URL', 'Please enter the base url of the server', params['base_url'])
  update_freq = droid.dialogGetInput('Update Frequency', 'Please enter the desired update frequency (seconds)', str(params['update_freq']))
  if secret.result is not None:
    params['secret'] = secret.result
  if base_url.result is not None:
    params['base_url'] = base_url.result
  if update_freq.result is not None:
    params['update_freq'] = float(update_freq.result)


  # strip of trailing slash if necessary:
  if base_url[-1] == '/': base_url = base_url[:-1]

  # the forever loop of location-sending:
  for cnt in count():
    print "%d: trying to get location" % cnt
    ll = get_lat_lon(droid)
    if ll is not None:
      print 'got location:', ll
      ll.update({'secret':params['secret']})
      put_loc(droid, params['base_url'], ll)
    else:
      print "couldn't get location"
    time.sleep(params['update_freq'])

