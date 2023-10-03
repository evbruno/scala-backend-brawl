GRAAL_JDK   := ${HOME}/Downloads/graalvm-jdk-17.0.8+9.1/Contents/Home
OPENJDK7    := /usr/local/opt/openjdk@17
APP_PORT    := 9999
APP_VER     := $(shell grep "ThisBuild / version := " build.sbt | cut -c 25-27)
SCALA_VER   := 2.13
APP_IMPL    := http4s
DOCKER_REPO := educhaos/scala-brawl
NOW			:= $(shell date +%s)

.PHONY: docker

build:
	sbt -java-home ${OPENJDK7} -v +app-http4s/assembly

graal-agent:
	${GRAAL_JDK}/bin/java \
		-agentlib:native-image-agent=config-output-dir=./app-http4s/src/main/resources/META-INF/native-image-2.13/ \
		-jar ./app-http4s/target/scala-2.13/app.jar

docker:
	@$(MAKE) _docker_build
	@$(MAKE) _docker_build SCALA_VER=3.3.0

docker-native:
	#TODO didnt work on scala 3.x
	docker build --build-arg SCALA_VER=2.13 \
		-t $(DOCKER_REPO):http4s-native-scala_2.13_$(APP_VER) \
		-f ./docker/Dockerfile.native app-http4s

_docker_build:
	docker build --build-arg SCALA_VER=${SCALA_VER} \
		-t $(DOCKER_REPO):${APP_IMPL}-jvm-scala_${SCALA_VER}_$(APP_VER) \
		-t $(DOCKER_REPO):${APP_IMPL}-jvm-scala_${SCALA_VER}_latest \
		-f ./docker/Dockerfile.jvm app-http4s

run:
	docker compose -f ./docker/docker-compose.yml up

stop:
	docker compose -f ./docker/docker-compose.yml down --remove-orphans

push:
	docker push ${DOCKER_REPO} -a

docker-pg:
	docker run --name postgres -e POSTGRES_PASSWORD=brawler -e POSTGRES_USER=brawler -e POSTGRES_DB=brawler -p 5432:5432 -d postgres

routes:
	http post localhost:${APP_PORT}/pessoas apelido=fooo_0_$(NOW) nome=FooNome nascimento=1981-12-31
	http get localhost:${APP_PORT}/pessoas/12345
	http get localhost:${APP_PORT}/pessoas/076968FD-C695-4D61-AC0B-8FE8F59ECCA1

	http post localhost:${APP_PORT}/pessoas apelido=fooo_2_$(NOW) nome=FooNome nascimento=1981-12-31 stack:='["scala","rust"]'

	http post localhost:${APP_PORT}/pessoas apelido=fooo_1_$(NOW) nome=FooNome nascimento=1981-12-31
	http post localhost:${APP_PORT}/pessoas apelido=fooo_1_$(NOW) nome=FooNome nascimento=1981-12-31

	http get localhost:${APP_PORT}/pessoas t==scala
	http get localhost:${APP_PORT}/contagem-pessoas



