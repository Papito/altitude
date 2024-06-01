SHELL=/bin/sh

clean:
	rm -rf data/db
	rm -rf data/p/*
	rm -rf data/sorted/*
	rm -rf data/triage/*
	rm -rf data/trash/*

up:
	docker-compose -f docker-compose.yml -f docker-compose.test.yml up
