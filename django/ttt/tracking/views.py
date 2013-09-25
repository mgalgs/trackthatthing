from django.shortcuts import render, get_object_or_404
from django.http import HttpResponseRedirect, HttpResponse
from django.contrib.auth.decorators import login_required
from django.contrib import messages
from django.conf import settings

from simplewords import simplewords

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
