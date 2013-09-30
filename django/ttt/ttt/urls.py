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

    url(r'^accounts/login/$', 'django.contrib.auth.views.login'),
    url(r'^accounts/logout/$', 'django.contrib.auth.views.logout'),
    url(r'^accounts/register/$', 'ttt.views.register'),
    url(r'^accounts/profile/$', 'ttt.views.profile'),
    url(r'^accounts/password_change/$', 'django.contrib.auth.views.password_change'),
    url(r'^accounts/password_change_done/$', 'django.contrib.auth.views.password_change_done'),
    url(r'^accounts/password_reset/$', 'django.contrib.auth.views.password_reset'),
    url(r'^accounts/password_reset_done/$', 'django.contrib.auth.views.password_reset_done'),
    url(r'^accounts/password_reset_confirm/(?P<uidb36>[0-9A-Za-z]+)-(?P<token>.+)/$', 'django.contrib.auth.views.password_reset_confirm'),
    url(r'^accounts/password_reset_complete/$', 'django.contrib.auth.views.password_reset_complete'),
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
    url(r'^ttt_admin/$', 'admin'),
    url(r'^view_data/$', 'view_data'),
)
