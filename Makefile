SHELL=/bin/sh

test:
	sbt test

test-focused:
	sbt testFocused

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
