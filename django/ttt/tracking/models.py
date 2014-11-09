from django.utils.timezone import now as django_now
from django.db import models
from django.contrib.auth.models import User


class Location(models.Model):
    user = models.ForeignKey(User)
    latitude = models.FloatField()
    longitude = models.FloatField()
    accuracy = models.FloatField()
    speed = models.FloatField()
    date = models.DateTimeField(default=django_now)

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


# These were imported from the legacy ttt site.  If you are setting up your
# own instance of ttt you don't need any of these.
class OldSecret(models.Model):
    email              = models.CharField(max_length=200)
    user_id            = models.CharField(max_length=200)
    nickname           = models.CharField(max_length=200)
    auth_domain        = models.CharField(max_length=200)
    federated_identity = models.CharField(max_length=200)
    federated_provider = models.CharField(max_length=200)
    secret             = models.CharField(max_length=200)
    secret_readable    = models.CharField(max_length=200)
