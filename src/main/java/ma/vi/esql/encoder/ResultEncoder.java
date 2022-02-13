package ma.vi.esql.encoder;

import ma.vi.base.config.Configuration;
import ma.vi.esql.exec.Result;
import org.apache.commons.io.output.WriterOutputStream;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public interface ResultEncoder {
  default void encode(Result        result,
                      OutputStream  out,
                      Configuration params) {
    encode(result, new OutputStreamWriter(out, UTF_8), params);
  }

  default void encode(Result        result,
                      OutputStream  out) {
    encode(result, out, Configuration.EMPTY);
  }

  default void encode(Result        result,
                      Writer        out,
                      Configuration params) {
    encode(result, new WriterOutputStream(out, UTF_8), params);
  }

  default void encode(Result result,
                      Writer out) {
    encode(result, out, Configuration.EMPTY);
  }
}
