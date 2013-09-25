from django.conf.urls import patterns, include, url

urlpatterns = patterns(
    'tracking.views',
    url(r'^$', 'index'),
)
