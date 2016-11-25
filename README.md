# DevGym

### Cassandra in Docker

Start cassandra:
```sh
docker run --name cassandra -d -p 9042:9042 cassandra
```

Start the CQL interactive terminal (note that you will lose your history):
```sh
docker run -it --link cassandra:cassandra --rm cassandra sh -c 'exec cqlsh "$CASSANDRA_PORT_9042_TCP_ADDR"'
```

Load data to cassandra db:
```sh
sbt "runMain data.DataLoader"
or to drop schema before loading
sbt "runMain data.DataLoader drop"
```
