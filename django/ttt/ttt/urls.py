from django.conf.urls import patterns, include, url

# Uncomment the next two lines to enable the admin:
from django.contrib import admin
admin.autodiscover()

urlpatterns = patterns(
    '',
    # Examples:
    # url(r'^$', 'ttt.views.home', name='home'),
    # url(r'^ttt/', include('ttt.foo.urls')),

    # Uncomment the admin/doc line below to enable admin documentation:
    # url(r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    url(r'^admin/', include(admin.site.urls)),
)

urlpatterns += patterns(
    'tracking.views',
    url(r'^$', 'index'),
    url(r'^put/$', 'ttt_put'),
    url(r'^get/$', 'ttt_get'),
    url(r'^live/$', 'live'),
    url(r'^help/$', 'help'),
    url(r'^about/$', 'about'),
    url(r'^download/$', 'download'),
    url(r'^secret/$', 'secret'),
    url(r'^new_test_point/$', 'new_test_point'),
    url(r'^admin/$', 'admin'),
    url(r'^view_data/$', 'view_data'),
)
