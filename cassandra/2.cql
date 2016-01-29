CREATE TABLE devgym.user (
  name TEXT,
  uuid TIMEUUID,
  password TEXT,
  PRIMARY KEY (name)
);

CREATE TABLE devgym.solution (
  type TEXT,
  month TIMESTAMP,
  uuid TIMEUUID,
  task TEXT,
  blank TEXT,
  test TEXT,
  PRIMARY KEY ((type, month), uuid)
) WITH CLUSTERING ORDER BY (uuid DESC);
