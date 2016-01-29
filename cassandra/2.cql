CREATE TABLE devgym.user (
  name TEXT,
  timeuuid TIMEUUID,
  password TEXT,
  PRIMARY KEY (name)
);

CREATE TABLE devgym.task (
  type TEXT,
  month TIMESTAMP,
  timeuuid TIMEUUID,
  task_description TEXT,
  solution_template TEXT,
  reference_solution TEXT,
  PRIMARY KEY ((type, month), timeuuid)
) WITH CLUSTERING ORDER BY (timeuuid DESC);
