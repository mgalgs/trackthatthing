from google.appengine.ext import db

### data models: ###
# base class that helps us serialize stuff later:
class DictModel(db.Model):
    def to_dict(self):
        d = dict([(p, unicode(getattr(self,p))) for p in self.properties()])
        d.update({'id':self.key().id()})
        return d

class Location(DictModel):
    user = db.UserProperty()
    latitude = db.FloatProperty()
    longitude = db.FloatProperty()
    accuracy = db.FloatProperty()
    speed = db.FloatProperty()
    date = db.DateTimeProperty(auto_now_add=True)

class Secret(DictModel):
    user = db.UserProperty()
    secret_readable = db.StringProperty()
    secret = db.StringProperty()
    niters = db.IntegerProperty() # number of iterations it took to generate this unique secret

