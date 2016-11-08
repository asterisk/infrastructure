#!/usr/bin/env bash

#
# Create an Alembic configuration file and upgrade a Postgresql database to HEAD
#
# Copyright 2016 (C), Digium, Inc.
# Joshua Colp <jcolp@digium.com>
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#	http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

DB_USER=$1
DB_HOST=$2
DB_NAME=$3

if [ -z "$DB_USER" ]; then
    echo "The database username must be the first argument."
    exit 1
fi

if [ -z "$DB_HOST" ]; then
    echo "The database host must be the second argument."
    exit 1
fi

if [ -z "$DB_NAME" ]; then
    echo "The database name must be the third argument."
    exit 1
fi

# Just in case the user and db don't already exist, let's create them.
# Don't check for failure. Failure likely means the role and DB already
# exist.
#
# XXX These lines are ignoring the DB_HOST parameter. However, in my
# local tests, specifying the host ran afoul of ph_hba.conf.
su -c "createuser --createdb" postgres
su -c "createdb $DB_NAME --owner=$DB_USER" postgres

# Drop any existing tables we have created from a previous run to ensure the alembic has to run all revisions
psql --username=${DB_USER} --host=${DB_HOST} --db=${DB_NAME} --command="DROP OWNED BY ${DB_USER} CASCADE"

cat > config.ini << EOF
[alembic]
script_location = asterisk/contrib/ast-db-manage/config
sqlalchemy.url = postgresql://${DB_USER}@${DB_HOST}/${DB_NAME}

[loggers]
keys = root,sqlalchemy,alembic

[handlers]
keys = console

[formatters]
keys = generic

[logger_root]
level = WARN
handlers = console
qualname =

[logger_sqlalchemy]
level = WARN
handlers =
qualname = sqlalchemy.engine

[logger_alembic]
level = INFO
handlers =
qualname = alembic

[handler_console]
class = StreamHandler
args = (sys.stderr,)
level = NOTSET
formatter = generic

[formatter_generic]
format = %(levelname)-5.5s [%(name)s] %(message)s
datefmt = %H:%M:%S

EOF

echo "Creating database tables"
alembic -c config.ini upgrade head

rm -rf config.ini

echo "*** Asterisk Realtime Database Ready ***"

