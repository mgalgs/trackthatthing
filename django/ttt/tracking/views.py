from django.shortcuts import render, get_object_or_404
from django.http import HttpResponseRedirect, HttpResponse
from django.contrib.auth.decorators import login_required
from django.contrib import messages
from django.conf import settings
from django.utils.simplejson import dumps

import datetime

from simplewords import simplewords
from tracking.models import Secret, Location

DEFAULT_NUM_POINTS = 30
DATE_FORMAT = '%Y-%m-%d %H:%M:%S'

# helper functions:
def get_a_secret(length):
    salt = ''.join([random.choice('123456789abc') for i in xrange(3)])
    return ' '.join([random.choice(simplewords) for i in xrange(length)]) + ' ' + salt

def generate_secret():
    # max number of words in secret:
    max_secret_length = 4
    cnt = 1
    # start by trying a short secret:
    full_secret = get_a_secret(2)
    trimmed_secret = full_secret.replace(' ', '')
    while Secret.objects.filter(secret__exact=trimmed_secret).count():
        # need to try a different secret:
        full_secret = get_a_secret(max_secret_length)
        trimmed_secret = full_secret.replace(' ','')
        cnt += 1
        if cnt > 1000:
            raise(BaseException("We were unable to generate a secret... Weird."))
    return full_secret, trimmed_secret, cnt

def index(request):
    return render(request, 'index.html')

def ttt_put(request):
    required_args = ['lat', 'lon', 'acc', 'secret', 'speed']
    valid_args = True
    for a in required_args:
        if a not in request.GET:
            valid_args = False
            break
    if not valid_args:
        obj = {
            'msg':'You must supply "lat", "lon", "acc", "speed", and "secret" arguments.',
            'success':False
        }
        json = dumps(obj)
        return HttpResponse(json, mimetype='application/json')

    sec = request.GET['secret']
    try:
        secret = Secret.objects.filter(secret__exact=sec).get()
    except Secret.DoesNotExist:
        obj = {'msg':'Bad secret: "%s"...' % sec, 'success':False}
        json = dumps(obj)
        return HttpResponse(json, mimetype='application/json')

    l = Location()
    l.latitude = float(request.GET['lat'])
    l.longitude = float(request.GET['lon'])
    l.accuracy = float(request.GET['acc'])
    l.speed = float(request.GET['speed'])
    l.user = secret.user
    if 'date' in request.GET:
        l.date = datetime.datetime.fromtimestamp(
            int(request.GET['date']))
    l.save()
    obj = {'msg':'Success!','success':True}
    json = dumps(obj)
    return HttpResponse(json, mimetype='application/json')

def ttt_get(request):
    return render(request, 'get.html')

def live(request):
    return render(request, 'live.html')

def help(request):
    return render(request, 'help.html')

def about(request):
    return render(request, 'about.html')

def download(request):
    return render(request, 'download.html')

def secret(request):
    return render(request, 'secret.html')

def new_test_point(request):
    return render(request, 'new_test_point.html')

def admin(request):
    return render(request, 'admin.html')

def view_data(request):
    return render(request, 'view_data.html')

