import datetime
import random

from simplewords import simplewords
from tracking.models import Secret

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


def secret(context):
    if context.user.is_authenticated():
        try:
            secret = Secret.objects.get(user__exact=context.user)
            secret_text = secret.secret_readable
        except Secret.DoesNotExist:
            secret = Secret()
            secret.user = context.user
            (secret.secret_readable, secret.secret, secret.niters) = generate_secret()
            secret.save()
            secret_text = secret.secret_readable

        return {'secret': secret_text}

    return {}

def this_year(context):
    return {'this_year': datetime.datetime.now().year}
