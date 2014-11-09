from django.db.models.signals import post_save
from django.dispatch import receiver
from django.contrib.auth.models import User
from models import Secret, OldSecret

@receiver(post_save, sender=User)
def connect_old_secrets(sender, instance, created, **kwargs):
    """This is used to connect secrets from the legacy ttt site to this
    instance.  Not required if you're setting up a fresh instance of
    ttt.

    """
    if not created:
        return

    try:
        old_secret = OldSecret.objects.get(nickname=instance.username)
    except OldSecret.DoesNotExist:
        print "No old secrets with nickname", instance.username, "Not connecting any old secrets."
        return

    print "Connecting old secret for legacy user:", old_secret.nickname

    Secret(user=instance,
           secret_readable=old_secret.secret_readable,
           secret=old_secret.secret, niters=1).save()
