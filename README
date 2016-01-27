# DevGym

### Cassandra in docker

Start cassandra:
```sh
docker run --name cassandra -d -p 9042:9042 cassandra
```

Start the CQL interactive terminal (note that you will lose your history):
```sh
docker run -it --link cassandra:cassandra --rm cassandra sh -c 'exec cqlsh "$CASSANDRA_PORT_9042_TCP_ADDR"'
```
