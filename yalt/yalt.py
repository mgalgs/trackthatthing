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

# local imports
from simplewords import simplewords

# max number of words in secret:
max_secret_length = 4

# helper functions:
def get_a_secret(length):
    salt = ''.join([random.choice('0123456789ABC') for i in xrange(3)])
    return ' '.join([random.choice(simplewords) for i in xrange(length)]) + ' ' + salt

def generate_secret():
    cnt = 1
    # start by trying a short secret:
    st = get_a_secret(2)
    while Secret.gql("WHERE secret = :1", st).count():
        # need to try a different secret:
        st = get_a_secret(max_secret_length)
        cnt += 1
        if cnt > 1000:
            raise(BaseException("We were unable to generate a secret... Weird."))
    return st,cnt

# data models:
class Location(db.Model):
    user = db.UserProperty()
    latitude = db.FloatProperty()
    longitude = db.FloatProperty()
    date = db.DateTimeProperty(auto_now_add=True)

class Secret(db.Model):
    user = db.UserProperty()
    secret = db.StringProperty()
    niters = db.IntegerProperty() # number of iterations it took to generate this unique secret


class MyBaseHandler(webapp.RequestHandler):
    """
    This class will be extended by my controllers. Gives some nice
    functionality.
    """
    def __init__(self):
        self.current_user = users.get_current_user()

    def render_me(self, template_name, template_values={}):
        'Renders a template with given template values'
        template_path = os.path.join(os.path.dirname(__file__), 'templates', template_name)
        # some default template values:
        if self.current_user:
            secretq = Secret.gql("WHERE user = :1", self.current_user)
            if not secretq.count():
                secret = Secret()
                secret.user = self.current_user
                secret.secret,secret.niters = generate_secret()
                secret.put()
                secret_text = secret.secret
            else:
                secret_text = secretq.get().secret
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
    def debug_string(self, st):
        'Prints string to output'
        self.response.headers['Content-Type'] = 'text/plain'
        self.response.out.write(st)
    def debug_var(self, var):
        'Pretty prints variable to output'
        st = StringIO.StringIO()
        pprint(var, st)
        self.debug_string(st.getvalue())
        

# controllers:
class MainPage(MyBaseHandler):
    def get(self):
        self.render_me('index.html')


class GetData(MyBaseHandler):
    def get(self):
        self.render_me("get.html")

class PutData(MyBaseHandler):
    def get(self):
        self.render_me("put.html")

class Admin(MyBaseHandler):
    def get(self):
        user = self.current_user
        if users.is_current_user_admin():
            self.render_me('admin.html', {
                    'secrets': Secret.all(),
                    'user': user
                    })
        else:
            self.debug_string('Hilo there ' + str(user) + '\n')
            self.debug_var(user)

class Live(MyBaseHandler):
    def get(self):
        self.render_me("live.html")


application = webapp.WSGIApplication(
    [('/', MainPage),
     ('/put', PutData),
     ('/get', GetData),
     ('/live', Live),
     ('/admin', Admin)],
    debug=True)

def main():
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
