/*
 * Copyright (c) 2020 Vikash Madhow
 */

package ma.vi.esql.encoder;

import ma.vi.esql.database.Database;
import ma.vi.esql.database.Postgresql;
import ma.vi.esql.database.SqlServer;

import java.util.Map;

import static ma.vi.esql.database.Database.*;

/**
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class Databases {
  public static Postgresql Postgresql() {
    if (postgresql == null) {
      postgresql = new Postgresql(Map.of(
          CONFIG_DB_NAME, "test",
          CONFIG_DB_USER, "test",
          CONFIG_DB_PASSWORD, "test",
          CONFIG_DB_CREATE_CORE_TABLES, true));
//      createTestTables(postgresql);
    }
    return postgresql;
  }

  public static SqlServer SqlServer() {
    if (sqlServer == null) {
      sqlServer = new SqlServer(Map.of(
          CONFIG_DB_NAME, "test",
          CONFIG_DB_USER, "test",
          CONFIG_DB_PASSWORD, "test",
          CONFIG_DB_CREATE_CORE_TABLES, true));
//      createTestTables(sqlServer);
    }
    return sqlServer;
  }

  public static Database[] databases() {
    return new Database[] {
        Postgresql(),
        SqlServer()
    };
  }

  private static SqlServer sqlServer;
  private static Postgresql postgresql;
}
