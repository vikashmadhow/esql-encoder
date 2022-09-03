package ma.vi.esql.encoder;

import ma.vi.base.config.Configuration;
import ma.vi.esql.database.EsqlConnection;
import ma.vi.esql.exec.Result;
import ma.vi.esql.translation.Translatable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static ma.vi.esql.encoder.ResultEncoder.TARGET;
import static ma.vi.esql.translation.Translatable.Target.ESQL;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

/**
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class JsonEncoderTest extends DataTest {
  @TestFactory
  Stream<DynamicTest> encodeSimpleSelect() {
    return Stream.of(databases)
                 .map(db -> dynamicTest(db.target().toString(), () -> {
                   try (EsqlConnection con = db.esql(db.pooledConnection())) {
                     con.exec("drop table test.X");
                     con.exec("""
                            create table test.X drop undefined({
                                xc: 'Result Metadata'
                              }
                              _id uuid not null,
                              a int {
                                m1: 1
                              },
                              b int not null {
                                m1: 'abc',
                                m2: b + c,
                                m3: 2 * b,
                                m4: $(a.m1 * 5)
                              },
                              c int default 5,
                              d int,
                              e = b+c+d {
                                m3: 10,
                                "values": {"any": {en: 'Any', fr: 'Une ou plusieurs'}, "all": {en: 'All', fr: 'Toutes'}}
                              },
                              primary key(_id),
                              unique(a),
                              unique(a, b),
                              unique(b, c, d)
                            )""");

                     con.exec("""
                              insert into test.X(_id, a, b, c, d)
                              values (newid(), 1, 2, 3, 4),
                                     (newid(), 5, 6, 7, 8),
                                     (newid(), 3, 4, default, 5)
                              """);
                     ResultEncoder encoder = new JsonResultEncoder();

                     Result rs = con.exec("select * from t:test.X order by a");
                     // System.out.println(encoder.encode(rs));
                     assertTrue(new JSONObject(hideUuids(loadTextResource("/testout1.json")))
                                  .similar(new JSONObject(hideUuids(encoder.encode(rs)))));

                     rs = con.exec("select * from test.X order  by a");
                     assertTrue(new JSONObject(hideUuids(loadTextResource("/testout1.json"))).getJSONArray("rows")
                                  .similar(new JSONArray(hideUuids(encoder.encode(rs, Configuration.of(ResultEncoder.ROWS_ONLY, true))))));
                   }
                 }));
  }

  @TestFactory
  Stream<DynamicTest> encodeRelation() {
    return Stream.of(databases)
                 .map(db -> dynamicTest(db.target().toString(), () -> {
                   try (EsqlConnection con = db.esql(db.pooledConnection())) {
                     con.exec("drop table test.X");
                     con.exec("""
                            create table test.X drop undefined({
                                xc: 'Result Metadata'
                              }
                              _id uuid not null,
                              a int not null {
                                m1: 1
                              },
                              b int not null {
                                m1: 'abc',
                                m2: b + c,
                                m3: 2 * $a('m1'),
                                m4: from s:S select max(s.a) where s.a > this.b
                              },
                              c int default 5,
                              d int,
                              e = b+c+d {
                                m3: 10,
                                "values": {"any": {en: 'Any', fr: 'Une ou plusieurs'}, "all": {en: 'All', fr: 'Toutes'}}
                              },

                              check 0 < a < 10,
                              check b * 5 < 100,
                              check b > 0,
                              
                              unique(a),
                              unique(a, b),
                              unique(b, c, d),
                              
                              primary key(_id)
                            )""");

                     con.exec("""
                            create table test.Y drop undefined(
                              _id uuid not null,
                              a int not null,
                              b int not null,
                              x_id uuid,

                              foreign key (a) references test.X(a),
                              foreign key (a, b) references test.X(a, b),
                              foreign key (x_id) references test.X(_id),
                              primary key(_id)
                            )""");

                     con.exec("""
                            create table test.Z drop undefined(
                              _id uuid not null,
                              a int not null,
                              b int not null,
                              x_id uuid,
                              y_id uuid,

                              foreign key (x_id) references test.X(_id),
                              foreign key (y_id) references test.Y(_id),
                              primary key(_id)
                            )""");

                     con.exec("""
                              insert into test.X(_id, a, b, c, d)
                              values (newid(), 1, 2, 3, 4),
                                     (newid(), 5, 6, 7, 8),
                                     (newid(), 3, 4, default, 5)
                              """);

                     con.exec("""
                              insert into test.Y(_id, a, b, x_id)
                              values (newid(), 1, 2, (select _id from test.X where a=1)),
                                     (newid(), 5, 6, (select _id from test.X where a=5)),
                                     (newid(), 3, 4, (select _id from test.X where a=3))
                              """);

                     ResultEncoder encoder = new JsonResultEncoder();


                     assertTrue(new JSONObject(loadTextResource("/x_struct.json"))
                                      .similar(new JSONObject(encoder.encode(db.structure().relation("test.X")))));

                     assertTrue(new JSONObject(loadTextResource("/x3_struct.json"))
                                      .similar(new JSONObject(encoder.encode(db.structure().relation("test.X"),
                                                                             Configuration.of(TARGET, ESQL)))));

                     System.out.println(encoder.encode(db.structure().relation("test.Y")));
                     assertTrue(new JSONObject(loadTextResource("/y_struct.json"))
                                      .similar(new JSONObject(encoder.encode(db.structure().relation("test.Y")))));
                     assertTrue(new JSONObject(loadTextResource("/z_struct.json"))
                                      .similar(new JSONObject(encoder.encode(db.structure().relation("test.Z")))));

//                     System.out.println(encoder.encode(db.structure().relation("test.Y")));
//                     System.out.println(encoder.encode(db.structure().relation("test.Z")));
                   }
                 }));
  }
}
