package ma.vi.esql.encoder;

import ma.vi.base.config.Configuration;
import ma.vi.esql.exec.Result;
import ma.vi.esql.semantic.type.Relation;
import org.apache.commons.io.output.WriterOutputStream;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Interface implemented by encoders which can produce a representation of a
 * result or relation that can be transmitted or stored. Encoders can produce
 * text or binary representations as needed.
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public interface ResultEncoder {
  /**
   * Encodes the query result into the outputstream, applying any of the specified
   * parameters.
   * @param result Query result to encode.
   * @param out Outputstream to write encoded result to.
   * @param params Parameters to control the encoding.
   */
  default void encode(Result        result,
                      OutputStream  out,
                      Configuration params) {
    encode(result, new OutputStreamWriter(out, UTF_8), params);
  }

  /**
   * Encodes the query result into the outputstream.
   * @param result Query result to encode.
   * @param out Outputstream to write encoded result to.
   */
  default void encode(Result       result,
                      OutputStream out) {
    encode(result, out, Configuration.EMPTY);
  }

  /**
   * Encodes the query result into the writer, applying any of the specified
   * parameters.
   * @param result Query result to encode.
   * @param out Writer to write encoded result to.
   * @param params Parameters to control the encoding.
   */
  default void encode(Result        result,
                      Writer        out,
                      Configuration params) {
    encode(result, new WriterOutputStream(out, UTF_8), params);
  }

  /**
   * Encodes the query result into the writer.
   * @param result Query result to encode.
   * @param out Writer to write encoded result to.
   */
  default void encode(Result result,
                      Writer out) {
    encode(result, out, Configuration.EMPTY);
  }

  /**
   * Utility method that uses a {@link StringWriter} to encode the result into
   * a String.
   * @param result Result to encode.
   * @return The encoded result as a string.
   */
  default String encode(Result result) {
    return encode(result, Configuration.EMPTY);
  }

  /**
   * Utility method that uses a {@link StringWriter} to encode the result into
   * a String.
   * @param result Result to encode.
   * @return The encoded result as a string.
   * @param params Parameters to control the encoding.
   */
  default String encode(Result result, Configuration params) {
    StringWriter sw = new StringWriter();
    encode(result, sw, params);
    return sw.toString();
  }

  /**
   * Encodes the structure of the relation into the outputstream, applying any
   * of the specified parameters.
   * @param relation Relation whose structure is to be encoded.
   * @param out Outputstream to write encoded structure to.
   * @param params Parameters to control the encoding.
   */
  default void encode(Relation      relation,
                      OutputStream  out,
                      Configuration params) {
    encode(relation, new OutputStreamWriter(out, UTF_8), params);
  }

  /**
   * Encodes the structure of the relation into the outputstream, applying any
   * of the specified parameters.
   * @param relation Relation whose structure is to be encoded.
   * @param out Outputstream to write encoded structure to.
   */
  default void encode(Relation     relation,
                      OutputStream out) {
    encode(relation, out, Configuration.EMPTY);
  }

  /**
   * Encodes the structure of the relation into the writer, applying any
   * of the specified parameters.
   * @param relation Relation whose structure is to be encoded.
   * @param out Writer to write encoded structure to.
   * @param params Parameters to control the encoding.
   */
  default void encode(Relation      relation,
                      Writer        out,
                      Configuration params) {
    encode(relation, new WriterOutputStream(out, UTF_8), params);
  }

  /**
   * Encodes the structure of the relation into the writer, applying any
   * of the specified parameters.
   * @param relation Relation whose structure is to be encoded.
   * @param out Writer to write encoded structure to.
   */
  default void encode(Relation relation,
                      Writer   out) {
    encode(relation, out, Configuration.EMPTY);
  }

  /**
   * Utility method that uses a {@link StringWriter} to encode the relation into
   * a String.
   * @param relation Relation to encode.
   * @return The encoded result as a string.
   */
  default String encode(Relation relation) {
    return encode(relation, Configuration.EMPTY);
  }

  /**
   * Utility method that uses a {@link StringWriter} to encode the relation into
   * a String.
   * @param relation Relation to encode.
   * @return The encoded result as a string.
   * @param params Parameters to control the encoding.
   */
  default String encode(Relation relation, Configuration params) {
    StringWriter sw = new StringWriter();
    encode(relation, sw, params);
    return sw.toString();
  }

  /**
   * The number of spaces to indent JSON (and other hierarchical format) text.
   */
  String INDENT = "INDENT";

  /**
   * The target to encode expressions to; default is JAVASCRIPT.
   */
  String TARGET = "TARGET";

  /**
   * Only output structure of result when set to true in config.
   */
  String STRUCTURE_ONLY = "STRUCTURE_ONLY";

  /**
   * Only output result rows (no structure) when set to true in config.
   */
  String ROWS_ONLY = "ROWS_ONLY";
}
