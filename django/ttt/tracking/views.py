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


def json_response(msg, success, data=None):
    obj = {'msg': msg, 'success', success}
    if data is not None:
        obj['data'] = data
    json = dumps(obj)
    return HttpResponse(json, mimetype='application/json')

def json_failure(msg, data=None):
    return json_response(msg, False)

def json_success(msg, data=None):
    return json_response(msg, False)

def ttt_put(request):
    required_args = ['lat', 'lon', 'acc', 'secret', 'speed']
    valid_args = True
    for a in required_args:
        if a not in request.GET:
            valid_args = False
            break
    if not valid_args:
        msg = 'You must supply "lat", "lon", "acc", "speed", and "secret" arguments.'
        return json_failure(msg)

    sec = request.GET['secret']
    try:
        secret = Secret.objects.filter(secret__exact=sec).get()
    except Secret.DoesNotExist:
        return json_failure('Bad secret: "%s"...' % sec)

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
    return json_success('Success!')

def ttt_get(request):
    if 'secret' not in request.GET():
        return json_failure('You must supply a secret.')

    clean_secret = request.GET['secret'].replace(' ','')
    try:
        secret = Secret.objects().filter(secret__exact=clean_secret).get()
    except Secret.DoesNotExist:
        return json_failure('Invalid secret.')

    user = sq.get().user
    num_points = int(request.GET.get('n', DEFAULT_NUM_POINTS))
    lq = Location.objects().filter(user__exact=user)
    if 'last' in request.GET:
        lq.filter(date__lt=str_to_date(request.GET['last']))
    if 'oldness' in request.GET:
        oldness = int(request.get['oldness'])
        if oldness > 0 and oldness <= 2592000:
            lq.filter(date__lt=
                      datetime.datetime.now() -
                      datetime.timedelta(0,oldness,0)
            )
    locations = lq.order('-date')[:num_points]
    location_dicts = [{
        'latitude': l.latitude,
        'longitude': l.longitude,
        'accuracy': l.accuracy,
        'speed': l.speed,
        'date': l.date,
    }] for l in locations
    return json_success('Succcess!', data={'locations': location_dicts})

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

