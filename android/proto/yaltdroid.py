import time
import urllib
import urllib2
import json
import android
from itertools import count
from pprint import pprint

DEFAULT_SECRET = 'rain bend 9a5'
DEFAULT_SERVER = 'http://firsthome.homelinux.org:8080'
DEFAULT_UPDATE_RATE = 10
DEFAULT_MIN_ACC = 10

def hr():
  print "================"

def event_loop(droid):
  for i in xrange(10):
    e = droid.receiveEvent()
    if e.result is not None:
      return e
    print 'still waiting for an event (%d)...' % i
    time.sleep(2)
  return None

def get_location(droid, min_acc=DEFAULT_MIN_ACC):
  droid.startLocating()
  try:
    for i in xrange(10):
      e = event_loop(droid)
      if e is None:
        print "couldn't get location..."
        continue
      else:
        hr()
        print 'from event_loop: ', e
        hr()

      # get the best provider:
      providers = ['gps', 'network'] # list them here in order of precedence!
      provider = None
      for p in providers:
        if p in e.result['data'].keys():
          provider = p
          break
      if provider is None:
        hr()
        print "Couldn't get a provider from this thing:"
        print e.result['data']
        hr()
        continue

      # see if the accuracy is good enough:
      if e.result['data'][provider]['accuracy'] < min_acc:
        # it's good enough! Break out.
        break
      else:
        print "Accuracy (%d) isn't less than min acc: (%d). Retrying... (%d)\n" % (e.result['data'][provider]['accuracy'], min_acc, i)

    return e.result['data'][provider] if e is not None else None
  finally:
    droid.stopLocating()

def get_lat_lon(droid, min_acc=DEFAULT_MIN_ACC):
  g = get_location(droid, min_acc=min_acc)
  if g is None: return None

  data = g


  # return the data:
  return {'lat':data['latitude'],
          'lon':data['longitude'],
          'speed':data['speed'],
          'acc':data['accuracy']}

def put_loc(droid,base_url,params):
  'params should contain lat,lon,acc, and secret'
  
  url = base_url + "/put?" + urllib.urlencode(params)
  req = urllib2.Request(url)
  opener = urllib2.build_opener()
  try:
    f = opener.open(req)
    j = json.load(f)
    hr()
    print 'got server response from url %s: %s' % (url, str(j))
    hr()
  except:
    hr()
    print 'yaltdroid broke. url was: ' + url
    print 'req was: ' + str(req)
    hr()
    droid.notify('yaltdroid broke.', 'url was: '+url)

if __name__ == "__main__":
  droid = android.Android()

  # set up some parameters:
  params = {'secret':DEFAULT_SECRET,
            'base_url':DEFAULT_SERVER,
            'update_freq':DEFAULT_UPDATE_RATE,
            'min_acc': DEFAULT_MIN_ACC}
  secret = droid.dialogGetInput('Secret', 'Please enter your secret', params['secret'])
  base_url = droid.dialogGetInput('Base URL', 'Please enter the base url of the server', params['base_url'])
  update_freq = droid.dialogGetInput('Update Frequency', 'Please enter the desired update frequency (seconds)', str(params['update_freq']))
  min_acc = droid.dialogGetInput('Minimum Accuracy', 'Please enter the desired minimum accuracy (meters)', str(params['min_acc']))
  if secret.result is not None:
    params['secret'] = secret.result
  if base_url.result is not None:
    params['base_url'] = base_url.result
  if update_freq.result is not None:
    params['update_freq'] = float(update_freq.result)
  if min_acc.result is not None:
    params['min_acc'] = float(min_acc.result)


  # strip of trailing slash if necessary:
  if base_url[-1] == '/': base_url = base_url[:-1]

  # the forever loop of location-sending:
  for cnt in count():
    print "%d: trying to get location" % cnt
    ll = get_lat_lon(droid, min_acc=params['min_acc'])

    if ll is not None:
      hr()
      print 'got location:', ll
      hr()
      ll.update({'secret':params['secret']})
      put_loc(droid, params['base_url'], ll)
    else:
      print "couldn't get location"
    time.sleep(params['update_freq'])

