/*
 * Copyright (c) 2022 Vikash Madhow
 */

package ma.vi.esql.encoder;

import ma.vi.base.config.Configuration;
import ma.vi.esql.exec.ColumnMapping;
import ma.vi.esql.exec.Result;
import ma.vi.esql.exec.ResultColumn;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.json.JSONObject.quote;

/**
 * Encodes a result as JSON in the following format and send through the provided
 * output stream (or writer). Result is output in the following format:
 *
 * <pre>
 *  {
 *    // result metadata
 *    $m: {
 *      type: "a.A",
 *      unique: [["_id"], ["a", "b"]]
 *    },
 *
 *    // columns in their loaded order (the same order that the rows are outputted)
 *    // along with their base metadata
 *    columns: {
 *      _id: {
 *        type: "uuid",
 *        required: false,
 *        readonly: true
 *      },
 *      a: {
 *        type: "string",
 *        label: "First",
 *        $e: "$(b + c)"
 *      },
 *      ...
 *    ],
 *
 *    rows: [
 *      [r1_c1, r1_c2, ..., r1_cn],
 *      [r2_c1, r2_c2, ..., r2_cn],
 *      ...
 *      [rm_c1, rm_c2, ..., rm_cn],
 *    ]
 *   }
 * </pre>
 *
 * Column values in rows can be a single value or an object containing the value
 * for that column along with metadata overriding the default metadata defined
 * for that column in the result header. E.g.
 *
 * <pre>
 *   rows: [
 *     [1, "abc", {$v: false, $m: {a: 1, b: 2}}],
 *     [2, {$v: "Xyz", $m: {b: 5}}, true]
 *   ]
 * </pre>
 *
 * @author Vikash Madhow (vikash.madhow@gmail.com)
 */
public class JsonResultEncoder implements ResultEncoder {
  @Override
  public void encode(Result        rs,
                     Writer        out,
                     Configuration params) {
    try {
      int indent = params.get("indent", 2);
      out.write("{\n");

      boolean hasPrevious = false;
      Map<String, Object> attributes = rs.query.resultAttributes();
      if (attributes != null && !attributes.isEmpty()) {
        /*
         * Output result metadata. E.g.:
         *    $m: {
         *      type: "a.A",
         *      unique: [["_id"], ["a", "b"]]
         *    }
         */
        out.write("\"$m\":{\n");
        boolean first = true;
        for (Map.Entry<String, Object> a: attributes.entrySet()) {
          if (first) first = false;
          else       out.write(",\n");
          out.write(repeat(' ', indent)
                  + '"' + a.getKey() + "\":"
                  + toJson(a.getValue(), indent));
        }
        out.write("\n}");
        hasPrevious = true;
      }

      List<ColumnMapping> columns = rs.columns();
      columns = columns == null ? emptyList() : columns;
      if (!columns.isEmpty()) {
        /*
         * columns in their loaded order (the same order that the rows are outputted)
         * along with their base metadata. E.g:
         *    columns: {
         *      _id: {
         *        type: "uuid",
         *        required: false,
         *        readonly: true,
         *        label: "Id"
         *      },
         *  ...
         */
        if (hasPrevious) out.write(",\n");
        out.write("\"columns\":{");
        boolean first = true;
        for (ColumnMapping c: columns) {
          if (first) {
            out.write("\n");
            first = false;
          } else {
            out.write(",\n");
          }
          out.write(repeat(' ', indent)
                  + '"' + c.column().name() + "\":{\n");

          if (c.attributes() != null
          && !c.attributes().isEmpty()) {
            boolean firstIndex = true;
            for (Map.Entry<String, Object> e: c.attributes().entrySet()) {
              if (firstIndex) firstIndex = false;
              else            out.write(",\n");
              out.write(repeat(' ', indent * 2)
                      + '"' + e.getKey() + "\":" + toJson(e.getValue(), indent));
            }
          }
          out.write('\n' + repeat(' ', indent) + "}");
        }
        out.write("\n}");
        hasPrevious = true;
      }

      boolean first = true;
      int columnCount = rs.columnsCount();
      while (rs.toNext()) {
        if (first) {
          if (hasPrevious) out.write(",\n");
          out.write("\"rows\":[\n");
          first = false;
        } else {
          out.write(",\n");
        }
        out.write(repeat(' ', indent) + '[');
        for (int c = 1; c <= columnCount; c++) {
          if (c > 1) out.write(", ");
          ResultColumn<?> col = rs.get(c);
          ColumnMapping colMap = columns.get(c-1);
          if (colMap.attributeIndices().isEmpty()) {
            /*
             * No computed metadata: output row value only.
             */
            out.write(toJson(col.value(), indent));
          } else {
            /*
             * Only output metadata not already included in column header.
             */
            Set<String> keys = col.metadata().keySet().stream()
                                  .filter(k -> !colMap.attributes().containsKey(k))
                                  .collect(Collectors.toSet());
            if (keys.isEmpty()) {
              out.write(toJson(col.value(), indent));

            } else {
              out.write("{\"$v\":" + toJson(col.value(), indent)
                      + ", \"$m\":{"
                      + keys.stream()
                            .map(k -> '"' + k + "\":"
                                    + toJson(col.metadata().get(k), indent))
                            .collect(Collectors.joining(", "))
                      + "}}");
            }
          }
        }
        out.write(']');
      }
      if (!first) out.write("\n]\n");
      out.write("}");
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Encode the (database) value as a JSON value.
   */
  public static String toJson(Object value, int indent) {
    if (value == null) {
      return "null";

    } else if (value instanceof String str) {
      return quote(str);

    } else if (value instanceof Number
            || value instanceof Boolean) {
      return value.toString();

    } else if (value instanceof Character c) {
      return "\"" + (c == '"' ? '\\' + c : c) + '"';

    } else if (value instanceof JSONArray json) {
      return json.toString(0);

    } else if (value instanceof JSONObject json) {
      return json.toString(indent);

    } else if (value instanceof Map<?, ?> map) {
      /*
       * Output map as JSON object.
       */
      StringBuilder st = new StringBuilder("{");
      boolean first = true;
      for (Map.Entry<?, ?> e: map.entrySet()) {
        if (first) {
          st.append(indent > 0 ? "\n" : "");
          first = false;
        } else {
          st.append(",\n");
        }
        st.append(repeat(' ', indent))
          .append(quote(e.getKey().toString())).append(':')
          .append(toJson(e.getValue(), indent + 1));
      }
      st.append('}');
      return st.toString();

    } else if (value instanceof Collection<?> col) {
      /*
       * Output collections as JSON array.
       */
      StringBuilder st = new StringBuilder("[");
      boolean first = true;
      for (Object e: col) {
        if (first) first = false;
        else       st.append(",\n");
        st.append(toJson(e, indent + 1));
      }
      st.append(']');
      return st.toString();

    } else if (value.getClass().isArray()) {
      /*
       * Output array as JSON array.
       */
      StringBuilder st = new StringBuilder("[");
      int len = Array.getLength(value);
      for (int i = 0; i < len; i++) {
        st.append(i > 0 ? ", " : "")
          .append(toJson(Array.get(value, i), indent));
      }
      st.append(']');
      return st.toString();

    } else if (value instanceof Date) {
      return '"' + TO_JAVASCRIPT_DATE.format((Date)value) + '"';

    } else {
      /*
       * quote unsupported json types and expressions
       */
      return quote(value.toString());
    }
  }

  /**
   * To send data to a Javascript client, ignore time zone as this is not kept in
   * the database.
   */
  public static final SimpleDateFormat TO_JAVASCRIPT_DATE =
      new SimpleDateFormat("yyyy-MM-d H:m:s.S");
}
