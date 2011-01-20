# system imports
import os
import StringIO
from pprint import pprint
import random

# appengine imports
from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db
from google.appengine.ext.webapp import template
from django.utils import simplejson as json

# local imports
from simplewords import simplewords

# max number of words in secret:
max_secret_length = 4
DEFAULT_NUM_POINTS = 30

# helper functions:
def get_a_secret(length):
    salt = ''.join([random.choice('123456789abc') for i in xrange(3)])
    return ' '.join([random.choice(simplewords) for i in xrange(length)]) + ' ' + salt

def generate_secret():
    cnt = 1
    # start by trying a short secret:
    st_r = get_a_secret(2)
    st = st_r.replace(' ', '')
    while Secret.gql("WHERE secret = :1", st).count():
        # need to try a different secret:
        st_r = get_a_secret(max_secret_length)
        st = st_r.replace(' ','')
        cnt += 1
        if cnt > 1000:
            raise(BaseException("We were unable to generate a secret... Weird."))
    return st_r,st,cnt

### data models: ###
# base class that helps us serialize stuff later:
class DictModel(db.Model):
    def to_dict(self):
        return dict([(p, unicode(getattr(self,p))) for p in self.properties()])

class Location(DictModel):
    user = db.UserProperty()
    latitude = db.FloatProperty()
    longitude = db.FloatProperty()
    accuracy = db.FloatProperty()
    date = db.DateTimeProperty(auto_now_add=True)
    

class Secret(DictModel):
    user = db.UserProperty()
    secret_readable = db.StringProperty()
    secret = db.StringProperty()
    niters = db.IntegerProperty() # number of iterations it took to generate this unique secret


class MyBaseHandler(webapp.RequestHandler):
    """
    This class will be extended by my controllers. Gives some nice
    functionality.
    """
    def __init__(self):
        self.current_user = users.get_current_user()
        self._already_output_headers = False

    def render_me(self, template_name, template_values={}):
        'Renders a template with given template values'
        template_path = os.path.join(os.path.dirname(__file__), 'templates', template_name)
        # some default template values:
        if self.current_user:
            secretq = Secret.gql("WHERE user = :1", self.current_user)
            if not secretq.count():
                secret = Secret()
                secret.user = self.current_user
                (secret.secret_readable, secret.secret, secret.niters) = generate_secret()
                secret_text = secret.secret_readable
                secret.put()
            else:
                secret_text = secretq.get().secret_readable
        else:
            secret_text = None
        template_values.update({
                'current_user': self.current_user,
                'logout_url': users.create_logout_url("/"),
                'login_url': users.create_login_url("/"),
                'is_admin': users.is_current_user_admin(),
                'secret': secret_text
                })
        self.response.out.write( template.render(template_path, template_values) )
    def write_string(self, st):
        'Prints string to output'
        if not self._already_output_headers:
            self.response.headers['Content-Type'] = 'text/plain'
            self._already_output_headers = True
        self.response.out.write(st)
    def debug_var(self, var):
        'Pretty prints variable to output'
        st = StringIO.StringIO()
        pprint(var, st)
        self.write_string(st.getvalue())


# controllers:
class MainPage(MyBaseHandler):
    def get(self):
        self.render_me('index.html')


class GetData(MyBaseHandler):
    def get(self):
        if 'secret' not in self.request.arguments():
            obj = {'msg':'You must supply a secret.','success':False}
        else:
            clean_secret = self.request.get('secret').replace(' ','')
            sq = Secret.all().filter('secret =', clean_secret)
            if not sq.count():
                obj = {'msg':'Invalid secret.','success':False}
            else:
                user = sq.get().user
            num_points = int(self.request.get('n')) if 'n' in self.request.arguments() else DEFAULT_NUM_POINTS
            locations = Location.all().filter('user = ', user).order('-date').fetch(num_points)
            obj = {
                'msg':'Success!',
                'success':True,
                'data':{'locations':[l.to_dict() for l in locations]}
                }
        self.write_string(json.dumps(obj))

class PutData(MyBaseHandler):
    def get(self):
        required_args = ['lat', 'lon', 'acc', 'secret']
        valid_args = True
        for a in required_args:
            if a not in self.request.arguments():
                valid_args = False
                break
        if not valid_args:
            obj = {'msg':'You must supply "lat", "lon", "acc", and "secret" arguments.','success':False}
        else:
            sec = self.request.get('secret').replace(' ','')
            sq = Secret.gql("WHERE secret = :1", sec)
            if not sq.count():
                obj = {'msg':'Bad secret: "%s"...' % sec, 'success':False}
            else:
                u = sq.get().user
                l = Location()
                l.latitude = float(self.request.get('lat'))
                l.longitude = float(self.request.get('lon'))
                l.accuracy = float(self.request.get('acc'))
                l.user = u
                l.put()
                obj = {'msg':'Success!','success':True}

        self.write_string(json.dumps(obj))

class Admin(MyBaseHandler):
    def get(self):
        user = self.current_user
        if users.is_current_user_admin():
            self.render_me('admin.html', {
                    'secrets': Secret.all(),
                    'user': user
                    })
        else:
            self.write_string('Hilo there ' + str(user) + '\n')
            self.debug_var(user)

class Live(MyBaseHandler):
    def get(self):
        sec = None
        if 'secret' in self.request.arguments():
            sec = self.request.get('secret')
        self.render_me("live.html", {'lsecret':sec})

class Help(MyBaseHandler):
    def get(self):
        self.render_me('help.html')

class About(MyBaseHandler):
    def get(self):
        self.render_me('about.html')

class Download(MyBaseHandler):
    def get(self):
        self.render_me('download.html')

application = webapp.WSGIApplication(
    [('/', MainPage),
     ('/put', PutData),
     ('/get', GetData),
     ('/live', Live),
     ('/help', Help),
     ('/about', About),
     ('/download', Download),
     ('/admin', Admin)],
    debug=True)

def main():
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
