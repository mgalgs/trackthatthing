#!/bin/bash

PIDFILE=run/gunicorn.pid
LOGFILE=logs/gunicorn.log
GUNICORN_PORT=${GUNICORN_PORT:-8000}
GUNICORN_HOST=${GUNICORN_HOST:-127.0.0.1}
VENV=${VENV:-/home/django/virtualenvs/ttt}
NUM_GUNICORN_WORKERS=${NUM_GUNICORN_WORKERS:-2}
TIMEOUT=120

cd $(dirname $0)

source ${VENV}/bin/activate
mkdir -p run logs
# kill any existing servers:
if [[ -e $PIDFILE ]]; then
    if ! kill -0 $(cat $PIDFILE) 2>/dev/null; then
	# process doesn't exist anymore
	rm -fv $PIDFILE
    else
	echo -n "killing existing gunicorn ($(cat $PIDFILE))..."
	kill $(cat $PIDFILE)
	sleep 2
	echo 'done!'
    fi
fi
echo -n "starting gunicorn..."
gunicorn ttt.wsgi:application \
	--bind ${GUNICORN_HOST}:${GUNICORN_PORT} \
	--pid=$PIDFILE \
	--log-file=$LOGFILE \
	--workers=$NUM_GUNICORN_WORKERS \
    --timeout=$TIMEOUT
echo 'done!'
