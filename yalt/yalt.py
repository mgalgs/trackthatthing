import os
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db
from google.appengine.ext.webapp import template

from hymnslist import hymns

# data models:
class Hymn(db.Model):
    number = db.IntegerProperty()
    title = db.StringProperty()

# controllers:
class MainPage(webapp.RequestHandler):
    def get(self):
        path = os.path.join(os.path.dirname(__file__), 'templates', 'index.html')
        template_values = {
            'foo': 'bar'
            }
        self.response.out.write(template.render(path, template_values))

class HymnNumberer(webapp.RequestHandler):
    def get(self):
        self.response.headers['Content-Type'] = 'text/plain'
        self.response.out.write('Updating hymns data store...\n')
        allhymns = Hymn.all()
        stored_hymns = [h.number for h in allhymns]
        for h in hymns.iteritems():
            if h[0] not in stored_hymns:
                hymn = Hymn()
                hymn.number = h[0]
                hymn.title = h[1]
                hymn.put()
        self.response.out.write('Done!')

class ListHymns(webapp.RequestHandler):
    def get(self, ):
        allhymns = Hymn.all()
        allhymns.order('number')

        path = os.path.join(os.path.dirname(__file__), 'templates', 'list.html')
        template_values = {
            'allhymns': allhymns
            }
        self.response.out.write(template.render(path, template_values))




application = webapp.WSGIApplication(
    [('/', MainPage),
     ('/numbering', HymnNumberer),
     ('/listhymns', ListHymns)],
    debug=True)

def main():
    run_wsgi_app(application)

if __name__ == '__main__':
    main()
