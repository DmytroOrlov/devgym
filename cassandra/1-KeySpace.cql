CREATE KEYSPACE devgym
  WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
/
CREATE TABLE devgym.user (
  name TEXT,
  timeuuid TIMEUUID,
  password TEXT,
  PRIMARY KEY (name)
);
/
CREATE TABLE devgym.task (
  year TIMESTAMP,
  lang TEXT,
  timeuuid TIMEUUID,
  name TEXT,
  description TEXT,
  solution_template TEXT,
  reference_solution TEXT,
  suite TEXT,
  solution_trait TEXT,
  PRIMARY KEY ((year, lang), timeuuid)
) WITH CLUSTERING ORDER BY (timeuuid DESC);
