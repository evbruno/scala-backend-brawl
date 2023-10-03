# Scala Backend Brawl

_envisioned by [rinhas de backend](https://github.com/zanfranceschi/rinha-de-backend-2023-q3/blob/main/INSTRUCOES.md)_

## Board

### Baseline

**Running locally from a Macbook PRO (13-inch, M1, 2020)**

| Tech | Total  | OKs    | KOs     | % KOs | Entries | Report                                         |
|------|--------|--------|---------|-------|---------|------------------------------------------------| 
| rust | 98.126 | 69.769 | 28.3567 | 29    | 30.260  | [report](.gatling-results/baseline/index.html) |


### Results

**[🐳 images](https://hub.docker.com/r/educhaos/scala-brawl/tags)**

| #   | Tech                           | Total  | OKs    | KOs    | % KOs | Entries | Report                                     |
|-----|--------------------------------|--------|--------|--------|-------|---------|--------------------------------------------|
| 0.1 | scala 2.13 + http4s w/ ember   | 86.779 | 33.737 | 53.042 | 61    | 19.994  | [report](.gatling-results/r001/index.html) |
| 0.1 | scala 2.3.30 + http4s w/ ember | 86.783 | 33.822 | 52.961 | 61    | 20.520  | [report](.gatling-results/r002/index.html) |
| 0.1 | native (graalvm - jdk17)       | 73.222 | 7.780  | 65.442 | 89    | 4.911   | [report](.gatling-results/r003/index.html) |


### Progress

#### v0.1
[cats effect starvation](https://typelevel.org/cats-effect/docs/core/starvation-and-tuning)
```
scala-brawler-api02-1  | 2023-10-03T01:09:59.506Z [WARNING] Your app's responsiveness to a new asynchronous event (such as a new connection, an upstream response, or a timer) was in excess of 100 milliseconds. Your CPU is probably starving. Consider increasing the granularity of your delays or adding more cedes. This may also be a sign that you are unintentionally running blocking I/O operations (such as File or InetAddress) without the blocking combinator.
```

## Build

### jvm

```bash
$ make build docker
```

### native

```bash
# (term 1)
$ make build docker
$ make docker-pg graal-agent
# (term 2)
$ make routes
$ sbt app-warmup/run
# fun...
# (kill terms)
$ make docker-native 
#... go make some ☕️
```


### Extras

**Fun copy and pasta:**

```bash
$ docker exec -it scala-brawler-api02-1 bash
$ apt update && apt install openjdk-17-jdk-headless -y
$ jmap --heap --pid 1
$ kill -3 1
$ jhsdb jinfo --flags --pid 1
$ jcmd 1 VM.flags
$ jps -v localhost:1
```

### native

```bash
make build 

```