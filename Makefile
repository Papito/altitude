SHELL=/bin/sh

watch:
	sbt watch

test:
	sbt test

test-focused:
	sbt testFocused

test-focused-psql:
	sbt testFocusedPostgres

test-focused-sqlite:
	sbt testFocusedSqlite

# WEB tests (controllers) do need the ENV explicitly set,
# as this is picked up by the testing server that is spun up automatically.
#
# Other tests run in a single process and force the test environment themselves.
test-focused-controller:
	ENV=test sbt testFocusedController

test-controller:
	ENV=test sbt testController

test-psql:
	sbt testPostgres

test-sqlite:
	sbt testSqlite

test-unit:
	sbt testUnit

lint:
	sbt scalafixAll

clean:
	rm -rf data/*
	mkdir -p data/db


publish:
	rm -rf release
	sbt assembly
	# we don't need this
	rm -rf target
	mkdir -p release/data/db


up:
	docker-compose -f docker-compose.yml -f docker-compose.test.yml up
