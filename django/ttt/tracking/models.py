from datetime import datetime
from django.db import models
from django.contrib.auth.models import User


class Location(models.Model):
    user = models.ForeignKey(User)
    latitude = models.FloatField()
    longitude = models.FloatField()
    accuracy = models.FloatField()
    speed = models.FloatField()
    date = models.DateTimeField(default=datetime.now)

    def __unicode__(self):
        return "u=%s, lat=%f, lon=%f, acc=%f, speed=%f, date=%s" % (
            str(self.user),
            self.latitude,
            self.longitude,
            self.accuracy,
            self.speed,
            str(self.date),
        )

class Secret(models.Model):
    user = models.ForeignKey(User)
    secret_readable = models.CharField(max_length=200)
    secret = models.CharField(max_length=200)
    niters = models.IntegerField() # number of iterations it took to generate this unique secret
