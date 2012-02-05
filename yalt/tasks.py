import os
import datetime

from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app

from models import Location

# are we on the cloud or a local dev server:
is_dev_server = os.environ['SERVER_SOFTWARE'].startswith('Dev')


class CleanUp(webapp.RequestHandler):
    def get(self):
        print 'hello, world'
        one_week_ago = datetime.datetime.now() - datetime.timedelta(7,0,0)
        lq = Location.all().filter('date < ', one_week_ago)
        cnt = 0
        for l in lq:
            cnt += 1
            l.delete()
        print 'finished cleaning up stuff older than a week (deleted ' \
            + str(cnt) + ' items)'

url_spec = [('/tasks/cleanup', CleanUp)]

application = webapp.WSGIApplication(
    url_spec,
    debug=is_dev_server)

def main():
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
