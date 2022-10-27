/*
 * Copyright (c) 2020 Vikash Madhow
 */

package ma.vi.esql.encoder;

import ma.vi.base.io.IO;
import ma.vi.esql.database.Database;
import ma.vi.esql.database.EsqlConnection;
import ma.vi.esql.exec.ColumnMapping;
import ma.vi.esql.exec.Result;
import ma.vi.esql.exec.ResultColumn;
import ma.vi.esql.syntax.Parser;
import ma.vi.esql.syntax.Program;
import ma.vi.esql.syntax.expression.literal.UuidLiteral;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.rightPad;

public class DataTest {
  public static Database[] databases;


  @BeforeAll
  static void setup() {
    databases = new Database[] {
        Databases.Postgresql(),
        Databases.SqlServer(),
//        Databases.HSqlDb(),
//        Databases.MariaDb(),
    };

    for (Database db: databases) {
      System.out.println(db.target());
      Parser p = new Parser(db.structure());
      try (EsqlConnection con = db.esql(db.pooledConnection())) {
        Program s = p.parse("""
                              create table S drop undefined({
                                name: 'S',
                                description: 'S test table',
                                validate_unique: [['a', 'b', 'e']],
                                dependents: {
                                  links: {
                                    _type: 'a.b.T',
                                    referred_by: 's_id',
                                    label: 'S Links'
                                  }
                                }
                              }
                              _id uuid not null,
                              a int {
                                m1: b > 5,
                                m2: 10,
                                m3: a != 0
                              },
                              b int {
                                m1: b < 0
                              },
                              c=a+b {
                                m1: a > 5,
                                m2: a + b,
                                m3: b > 5
                              },
                              d=b+c {
                                m1: 10
                              },
                              e bool {
                                m1: c
                              },
                              f=from S select max(a) {
                                m1: from S select min(a)
                              },
                              g=from S select distinct a+b where d>5 {
                                m1: from a.b.T select min(a)
                              },
                              h []text {
                                m1: 5
                              },
                              i string default 'Aie',
                              j []int,
                              k interval,
                              l int,
                              primary key(_id)
                            )""");
        con.exec(s);

        s = p.parse("create table a.b.T drop undefined(" +
                    "  {" +
                    "    name: 'T'," +
                    "    description: 'T test table'" +
                    "  } " +
                    "  _id uuid not null," +
                    "  a int {" +
                    "    m1: b > 5," +
                    "    m2: 10," +
                    "    m3: a != 0" +
                    "  }," +
                    "  b int {" +
                    "    m1: b < 0" +
                    "  }," +
                    "  c=a+b {" +
                    "    m1: a > 5," +
                    "    m2: a + b," +
                    "    m3: b > 5" +
                    "  }," +
                    "  x int {" +
                    "    x1: b > 5," +
                    "    x2: a != 0" +
                    "  }," +
                    "  y int {" +
                    "    y1: b > 5," +
                    "    y2: a != 0" +
                    "  }," +
                    "  s_id uuid {" +
                    "    link_table: 'S', " +
                    "    link_table_code: '_id', " +
                    "    link_table_label: 'a' " +
                    "  }," +
                    "  foreign key(s_id) references S(_id)," +
                    "  primary key(_id)" +
                    ")");
        con.exec(s);

        s = p.parse("create table a.b.X drop undefined(" +
                    "  {" +
                    "    name: 'X'," +
                    "    description: 'X test table'" +
                    "  } " +
                    "  _id uuid not null," +
                    "  a int {" +
                    "    m1: b > 5," +
                    "    m2: 10," +
                    "    m3: a != 0" +
                    "  }," +
                    "  b int {" +
                    "    m1: b < 0" +
                    "  }," +
                    "  c=a+b {" +
                    "    m1: a > 5," +
                    "    m2: a + b," +
                    "    m3: b > 5" +
                    "  }," +
                    "  s_id uuid {" +
                    "    link_table: 'S', " +
                    "    link_table_code: '_id', " +
                    "    link_table_label: 'a' " +
                    "  }," +
                    "  t_id uuid {" +
                    "    link_table: 'a.b.T', " +
                    "    link_table_code: '_id', " +
                    "    link_table_label: 'b' " +
                    "  }," +
                    "  foreign key(s_id) references S(_id)," +
                    "  foreign key(t_id) references a.b.T(_id) on update cascade on delete cascade," +
                    "  primary key(_id)" +
                    ")");
        con.exec(s);

        s = p.parse("create table b.Y drop undefined(" +
                    "  {" +
                    "    name: 'Y'," +
                    "    description: 'Y test table'" +
                    "  } " +
                    "  _id uuid not null," +
                    "  a int {" +
                    "    m1: b > 5," +
                    "    m2: 10," +
                    "    m3: a != 0" +
                    "  }," +
                    "  b int {" +
                    "    m1: b < 0" +
                    "  }," +
                    "  c=a+b {" +
                    "    m1: a > 5," +
                    "    m2: a + b," +
                    "    m3: b > 5" +
                    "  }," +
                    "  s_id uuid {" +
                    "    link_table: 'S', " +
                    "    link_table_code: '_id', " +
                    "    link_table_label: 'a' " +
                    "  }," +
                    "  t_id uuid {" +
                    "    link_table: 'a.b.T', " +
                    "    link_table_code: '_id', " +
                    "    link_table_label: 'b' " +
                    "  }," +
                    "  x_id uuid {" +
                    "    link_table: 'a.b.X', " +
                    "    link_table_code: '_id', " +
                    "    link_table_label: 'b' " +
                    "  }," +
                    "  foreign key(s_id) references S(_id)," +
                    "  foreign key(t_id) references a.b.T(_id) on update cascade on delete cascade," +
                    "  foreign key(x_id) references a.b.X(_id)," +
                    "  primary key(_id)" +
                    ")");
        con.exec(s);
      }
    }
  }

  public static void printResult(Result rs, int columnWidth) {
    printResult(rs, columnWidth, false);
  }

  public static void printResult(Result rs, int columnWidth, boolean showMetadata) {
    boolean first = true;
    while(rs.toNext()) {
      if (first) {
        System.out.println('+' + repeat(repeat('-', columnWidth) + '+', rs.columnsCount()));
        System.out.print('|');
        for (ColumnMapping col: rs.columns()) {
          System.out.print(rightPad(col.column().name(), columnWidth) + '|');
        }
        System.out.println();
        System.out.println('+' + repeat(repeat('-', columnWidth) + '+', rs.columnsCount()));
        first = false;
      }
      System.out.print('|');
      for (int i = 0; i < rs.columnsCount(); i++) {
        ResultColumn<Object> col = rs.get(i + 1);
        Object value = col.value();
        int spaceLeft = columnWidth;
        if (value != null) {
          spaceLeft -= value.toString().length();
          if (value.getClass().isArray()) {
            System.out.print(Arrays.deepToString((Object[])value));
          } else {
            System.out.print(value);
          }
        }
        if (showMetadata) {
          if (col.metadata() != null && !col.metadata().isEmpty()) {
            for (Map.Entry<String, Object> e: col.metadata().entrySet()) {
              if (e.getValue() != null) {
                spaceLeft -= e.getKey().length() + e.getValue().toString().length() - 2;
                System.out.print(',' + e.getKey() + ':' + e.getValue().toString());
              }
            }
          }
        }
        if (spaceLeft > 0) {
          System.out.print(repeat(' ', spaceLeft));
        }
        System.out.print('|');
      }
      System.out.println();
    }
    if (!first) {
      System.out.println('+' + repeat(repeat('-', columnWidth) + '+', rs.columnsCount()));
    }
  }

  public static String loadTextResource(String res) {
    try {
      return IO.readAllAsString(DataTest.class.getResourceAsStream(res));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static String hideUuids(String s) {
    return UuidLiteral.PATTERN.matcher(s).replaceAll("00000000-0000-0000-0000-000000000000");
  }

  private static String lengthen(String val, int length) {
    return rightPad(val == null ? " " : val, length, " ").substring(0, length);
  }
}
