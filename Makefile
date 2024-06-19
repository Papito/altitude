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

test-psql:
	sbt testPostgres

test-sqlite:
	sbt testSqlite

lint:
	sbt scalafixAll

clean:
	rm -rf data/db
	rm -rf data/p/*
	rm -rf data/sorted/*
	rm -rf data/triage/*
	rm -rf data/trash/*

up:
	docker-compose -f docker-compose.yml -f docker-compose.test.yml up
