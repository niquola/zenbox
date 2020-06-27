.EXPORT_ALL_VARIABLES:
.PHONY: test deploy

SHELL = bash

VERSION = $(shell cat VERSION)
DATE = $(shell date)

include .env

repl:
	clj -A:test:nrepl -m nrepl.cmdline

up:
	docker-compose up -d

stop:
	docker-compose stop

down:
	docker-compose down

jar:
	clj -A:build

docker:
	docker build -t ${IMG} .

pub:
	docker push ${IMG}

deploy:
	cd deploy && cat srv.tpl.yaml | ./envtpl.mac | kubectl apply -f -

all: jar docker pub deploy
	echo "Done"

test:
	clj -A:test:runner

