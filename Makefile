SHELL=/bin/sh

watch:
	ENV=dev sbt watch

test:
	ENV=test sbt test

test-focused:
	ENV=test sbt testFocused

test-focused-psql:
	ENV=test sbt testFocusedPostgres

test-focused-sqlite:
	ENV=test sbt testFocusedSqlite

# See: https://github.com/papito/altitude/wiki/How-the-tests-work#controller-tests-and-the-forced-postgres-config
test-focused-controller:
	@# The FORCE directive is CaSe SeNsItIvE
	ENV=test CONFIG_FORCE_db_engine=postgres sbt testFocusedController

# See: https://github.com/papito/altitude/wiki/How-the-tests-work#controller-tests-and-the-forced-postgres-config
test-controller:
	@# The FORCE directive is CaSe SeNsItIvE
	ENV=test CONFIG_FORCE_db_engine=postgres sbt testController

test-psql:
	ENV=test sbt testPostgres

test-sqlite:
	ENV=test sbt testSqlite

test-unit:
	ENV=test sbt testUnit

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
