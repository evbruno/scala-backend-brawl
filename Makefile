GRAAL_JDK := ${HOME}/Downloads/graalvm-jdk-17.0.8+9.1/Contents/Home
OPENJDK7  := /usr/local/opt/openjdk@17
APP_PORT  := 9999

build:
	sbt -java-home ${OPENJDK7} -v +app-http4s/assembly

graal-agent:
	${GRAAL_JDK}/bin/java \
		-agentlib:native-image-agent=config-output-dir=./app-http4s/src/main/resources/META-INF/native-image-2.13/ \
		-jar ./app-http4s/target/scala-2.13/app.jar

docker-native:
	#TODO didnt work on scala 3.x
	#docker build --build-arg SCALA_VER=3.3.0 -t scala-brawl:http4s-native-scala_3.3.0 -f Dockerfile.native app-http4s
	docker build --build-arg SCALA_VER=2.13 -t scala-brawl:http4s-native-scala_2.13 -f ./docker/Dockerfile.native app-http4s

docker-jvms:
	docker build --build-arg SCALA_VER=3.3.0 -t scala-brawl:http4s-jvm-scala_3.3.0 -f ./docker/Dockerfile.jvm app-http4s
	docker build --build-arg SCALA_VER=2.13  -t scala-brawl:http4s-jvm-scala_2.13  -f ./docker/Dockerfile.jvm app-http4s

docker-run:
	#docker run --network=container:postgres --name scala-brawler --rm -p 9999:9999 scala-brawl:http4s-jvm-scala_2.13
	docker run --network=container:postgres --name scala-brawler --rm -p 9999:9999 scala-brawl:http4s-jvm-scala_2.13
# TODO
routes:
	http post localhost:${APP_PORT}/pessoas apelido=fooo_0 nome=FooNome nascimento=1981-12-31
	http get localhost:${APP_PORT}/pessoas/12345
	http get localhost:${APP_PORT}/pessoas/076968FD-C695-4D61-AC0B-8FE8F59ECCA1

	http post localhost:${APP_PORT}/pessoas apelido=fooo_2 nome=FooNome nascimento=1981-12-31 stack:='["scala","rust"]'

	http post localhost:${APP_PORT}/pessoas apelido=fooo_1 nome=FooNome nascimento=1981-12-31
	http post localhost:${APP_PORT}/pessoas apelido=fooo_1 nome=FooNome nascimento=1981-12-31

	http get localhost:${APP_PORT}/pessoas t==scala
	http get localhost:${APP_PORT}/contagem-pessoas



