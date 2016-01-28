CREATE TABLE devgym.user (
  name TEXT,
  uuid TIMEUUID,
  password TEXT,
  PRIMARY KEY (name)
);

CREATE TABLE devgym.solution (
  type TEXT,
  week TIMESTAMP,
  task TEXT,
  blank TEXT,
  test TEXT,
  PRIMARY KEY ((type), week, task)
) WITH CLUSTERING ORDER BY (week DESC);
