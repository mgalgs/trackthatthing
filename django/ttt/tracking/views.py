from django.shortcuts import render, get_object_or_404
from django.http import HttpResponseRedirect, HttpResponse
from django.contrib.auth.decorators import login_required
from django.contrib import messages
from django.conf import settings
from django.utils.simplejson import dumps
from django.contrib.admin.views.decorators import staff_member_required

import datetime

from tracking.models import Secret, Location

DEFAULT_NUM_POINTS = 30
DATE_FORMAT = '%Y-%m-%d %H:%M:%S'

def json_response(msg, success, **kwargs):
    obj = {'msg': msg, 'success': success}
    for k,v in kwargs.iteritems():
        obj[k] = v
    json = dumps(obj)
    return HttpResponse(json, mimetype='application/json')

def json_failure(msg, **kwargs):
    return json_response(msg, False, **kwargs)

def json_success(msg, **kwargs):
    return json_response(msg, True, **kwargs)


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
    } for l in locations]
    return json_success('Succcess!', data={'locations': location_dicts})

def live(request):
    sec = request.GET.get('secret')
    return render(request, 'live.html', {'lsecret':sec})

def help(request):
    return render(request, 'help.html')

def about(request):
    return render(request, 'about.html')

def download(request):
    return render(request, 'download.html')

def secret(request):
    return render(request, 'secret.html')

@staff_member_required
def new_test_point(request):
    # check for required args:
    required_args = ['lat_step', 'lon_step', 'acc_step', 'speed_step']
    valid_args = True
    for a in required_args:
        if a not in request.GET:
            valid_args = False
            break
    if not valid_args:
        msg = 'You must supply ' + ', '.join(required_args) + ' arguments.'
        return json_failure(msg)

    user = request.user
    newl = Location()

    try:
        l = Location.objects().filter(user__exact=user).latest('date')
        newl.latitude = l.latitude + float(request.GET['lat_step'])
        newl.longitude= l.longitude + float(request.GET['lon_step'])
        newl.accuracy = l.accuracy + float(request.GET['acc_step'])
        newl.speed = l.speed + float(request.GET['speed_step'])
    except Location.DoesNotExist:
        newl.latitude = 32.95736
        newl.longitude = -117.233133
        newl.accuracy = 10.0
        newl.speed = 42.0

    newl.user = user
    newl.save()

    msg = 'Test point added: ' + str(newl.to_dict())
    return json_success(msg, title='Success!')

@staff_member_required
def admin(request):
    user = request.user
    data = []
    secrets = Secret.objects.all()
    for s in secrets:
        data.append({
            'secret': s,
            'num_locations': Location.objects.filter(user__exact=s.user).count()
        })

    return render(request, 'admin.html', {
        'secrets': secrets,
        'data': data,
        'user': user
    })

@staff_member_required
def view_data(request):
    user = request.user

    if 'secret' not in request.GET:
        return render(request, 'view_data.html', {
            'success': False
        })

    secret_txt = request.GET['secret']
    try:
        secret = Secret.objects().filter(secret__exact=secret_txt).get()
    except Secret.DoesNotExist:
        return render(request, 'view_data.html', {
            'success': False
        })

    return render(request, 'view_data.html', {
            'data': Location.objects().filter(user__exact=secret.user).order('-date')[:500],
            'secret_for_page': secret_txt,
            'success': True
            })
