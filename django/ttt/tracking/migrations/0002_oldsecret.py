# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models, migrations


class Migration(migrations.Migration):

    dependencies = [
        ('tracking', '0001_initial'),
    ]

    operations = [
        migrations.CreateModel(
            name='OldSecret',
            fields=[
                ('id', models.AutoField(verbose_name='ID', serialize=False, auto_created=True, primary_key=True)),
                ('email', models.CharField(max_length=200)),
                ('user_id', models.CharField(max_length=200)),
                ('nickname', models.CharField(max_length=200)),
                ('auth_domain', models.CharField(max_length=200)),
                ('federated_identity', models.CharField(max_length=200)),
                ('federated_provider', models.CharField(max_length=200)),
                ('secret', models.CharField(max_length=200)),
                ('secret_readable', models.CharField(max_length=200)),
            ],
            options={
            },
            bases=(models.Model,),
        ),
    ]
