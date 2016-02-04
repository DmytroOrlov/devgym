CREATE KEYSPACE devgym
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

CREATE TABLE devgym.user (
  name TEXT,
  timeuuid TIMEUUID,
  password TEXT,
  PRIMARY KEY (name)
);

CREATE TABLE devgym.task (
  year TIMESTAMP,
  type TEXT,
  timeuuid TIMEUUID,
  task_description TEXT,
  solution_template TEXT,
  reference_solution TEXT,
  test TEXT,
  PRIMARY KEY ((year, type), timeuuid)
) WITH CLUSTERING ORDER BY (timeuuid DESC);
