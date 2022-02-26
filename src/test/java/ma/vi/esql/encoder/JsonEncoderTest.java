package ma.vi.esql.encoder;

import ma.vi.esql.exec.EsqlConnection;
import ma.vi.esql.exec.Result;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.StringWriter;
import java.util.stream.Stream;

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
                              },
                              _id uuid not null,
                              a int {
                                m1: 1
                              },
                              b int not null {
                                m1: 'abc',
                                m2: b + c,
                                m3: 2 * b
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
                     Result rs = con.exec("select * from test.X order  by a");
                     ResultEncoder encoder = new JsonResultEncoder();

                     StringWriter sw = new StringWriter();
                     encoder.encode(rs, sw);
                     System.out.println(sw);
                     assertTrue(new JSONObject(hideUuids(loadTextResource("/testout1.json")))
                                      .similar(new JSONObject(hideUuids(sw.toString()))));
                   }
                 }));
  }
}
