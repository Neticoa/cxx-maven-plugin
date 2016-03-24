TEMPLATE = subdirs
SUBDIRS = utils application tests
application.depends = utils
tests.depends = utils
