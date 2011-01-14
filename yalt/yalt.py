import os
from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db
from google.appengine.ext.webapp import template

# data models:
class Location(db.Model):
    user = db.UserProperty()
    latitude = db.FloatProperty()
    longitude = db.FloatProperty()
    date = db.DateTimeProperty(auto_now_add=True)


def render_me(request_handler, template_name, template_values={}):
    template_path = os.path.join(os.path.dirname(__file__), 'templates', template_name)
    request_handler.response.out.write( template.render(template_path, template_values) )

# controllers:
class MainPage(webapp.RequestHandler):
    def get(self):
        template_values = {
            'foo': 'bar'
            }
        render_me(self, 'index.html')


class GetData(webapp.RequestHandler):
    def get(self):
        self.response.out.write(template.render("get.html"))

class PutData(webapp.RequestHandler):
    def get(self):
        self.response.out.write(template.render("put.html"))



application = webapp.WSGIApplication(
    [('/', MainPage),
     ('/put', PutData),
     ('/get', GetData)],
    debug=True)

def main():
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
