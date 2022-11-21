/*
 * Copyright (c) 2022 Vikash Madhow
 */

package ma.vi.esql.encoder;

import ma.vi.base.config.Configuration;
import ma.vi.base.tuple.T2;
import ma.vi.esql.exec.ColumnMapping;
import ma.vi.esql.exec.Result;
import ma.vi.esql.exec.ResultColumn;
import ma.vi.esql.semantic.type.Column;
import ma.vi.esql.semantic.type.Relation;
import ma.vi.esql.syntax.EsqlPath;
import ma.vi.esql.syntax.define.Attribute;
import ma.vi.esql.syntax.expression.Expression;
import ma.vi.esql.syntax.expression.UncomputedExpression;
import ma.vi.esql.syntax.expression.literal.Literal;
import ma.vi.esql.syntax.query.SingleTableExpr;
import ma.vi.esql.translation.StringForm;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pcollections.HashPMap;
import org.pcollections.IntTreePMap;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static ma.vi.esql.builder.Attributes.TYPE;
import static ma.vi.esql.database.Database.NULL_DB;
import static ma.vi.esql.database.EsqlConnection.NULL_CONNECTION;
import static ma.vi.esql.translation.Translatable.Target;
import static ma.vi.esql.translation.Translatable.Target.ESQL;
import static ma.vi.esql.translation.Translatable.Target.JAVASCRIPT;
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
 *    },
 *
 *    rows: [
 *      [r1_c1, r1_c2, ..., r1_cn],
 *      [r2_c1, r2_c2, ..., r2_cn],
 *      ...
 *      [rm_c1, rm_c2, ..., rm_cn]
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
      int indent = params.get(INDENT, 2);
      boolean rowsOnly = params.get(ROWS_ONLY, false);
      boolean outputStructure = !rowsOnly;
      boolean outputRows = !params.get(STRUCTURE_ONLY, false);

      if (!rowsOnly) out.write("{\n");
      boolean hasPrevious = false;
      List<ColumnMapping> columns = rs.columns();
      if (outputStructure) {
        Map<String, Object> attributes = new HashMap<>(rs.query.resultAttributes() != null
                                                     ? rs.query.resultAttributes()
                                                     : emptyMap());
        if (!attributes.containsKey(TYPE)
         && rs.query.query() != null) {
          SingleTableExpr table = rs.query.query().tables() != null
                                ? rs.query.query().tables().find(SingleTableExpr.class)
                                : null;
          if (table != null) attributes.put(TYPE, table.tableName());
        }
        if (!attributes.isEmpty()) {
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

        columns = columns == null ? emptyList() : columns;
        if (!columns.isEmpty()) {
          /*
           * columns in their loaded order (the same order that the rows are
           * outputted) along with their base metadata. E.g:
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
                if (!e.getKey().equals("_id")) {
                  if (firstIndex) firstIndex = false;
                  else            out.write(",\n");
                  out.write(repeat(' ', indent * 2)
                          + '"' + e.getKey() + "\":" + toJson(e.getValue(), indent));
                }
              }
            }
            out.write('\n' + repeat(' ', indent) + "}");
          }
          out.write("\n}");
          hasPrevious = true;
        }
      }

      if (outputRows) {
        boolean first = true;
        int columnCount = rs.columnsCount();
        while (rs.toNext()) {
          if (first) {
            if (hasPrevious) out.write(",\n");
            if (!rowsOnly)   out.write("\"rows\":");
            out.write("[\n");
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
      }
      if (!rowsOnly) out.write("}");
      out.flush();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  @Override
  public void encode(Relation      relation,
                     Writer        out,
                     Configuration params) {
    try {
      int indent = params.get(INDENT, 2);
      Target target = params.get(TARGET, JAVASCRIPT);

      out.write("{\n");
      List<T2<Relation, Column>> columns = relation.columns();
      Map<String, Attribute> attributes = new HashMap<>(relation.attributes() != null
                                                      ? relation.attributes()
                                                      : emptyMap());
      if (!attributes.containsKey(TYPE)) {
        attributes.put(TYPE, Attribute.from(null, TYPE, relation.name()));
      }
      /*
       * Output relation metadata. E.g.:
       *    $m: {
       *      type: "a.A",
       *      unique: [["_id"], ["a", "b"]]
       *    }
       */
      out.write("\"$m\":{\n");
      boolean first = true;
      for (Map.Entry<String, Attribute> a: attributes.entrySet()) {
        if (first) first = false;
        else       out.write(",\n");
        out.write(repeat(' ', indent));
        out.write('"' + a.getKey() + "\":");
        out.write(toJson(a.getValue().attributeValue(), indent, target));
      }
      out.write("\n}");

      columns = columns == null ? emptyList() : columns;
      if (!columns.isEmpty()) {
        /*
         * columns in their loaded order (the same order that the rows are
         * outputted) along with their base metadata. E.g:
         *    columns: {
         *      _id: {
         *        type: "uuid",
         *        required: false,
         *        readonly: true,
         *        label: "Id"
         *      },
         *  ...
         */
        out.write(",\n");
        out.write("\"columns\":{");
        first = true;
        for (T2<Relation, Column> col: columns) {
          Column c = col.b();
          if (!c.name().contains("/")) {
            if (first) {
              out.write("\n");
              first = false;
            } else {
              out.write(",\n");
            }
            out.write(repeat(' ', indent) + '"' + c.name() + "\":{");

            boolean firstIndex = true;
            if (c.derived()) {
              out.write('\n' + repeat(' ', indent * 2));
              out.write("\"derived_expression\": ");
              out.write(toJson(c.expression(), 0, target));
              firstIndex = false;
            }

            if (c.metadata() != null
            &&  c.metadata().attributes() != null
            && !c.metadata().attributes().isEmpty()) {
              if (firstIndex) out.write('\n');
              for (Attribute a: c.metadata().attributes().values()) {
                if (!a.name().equals("_id")) {
                  if (firstIndex) firstIndex = false;
                  else            out.write(",\n");
                  out.write(repeat(' ', indent * 2)
                          + '"' + a.name() + "\":" + toJson(a.attributeValue(), indent, target));
                }
              }
              out.write('\n' + repeat(' ', indent));
            }
            out.write("}");
          }
        }
        out.write("\n}");
      }
      out.write("}");
      out.flush();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * Encode the (database) value as a JSON value.
   * @param value The value to encode.
   * @param indent The number of spaces to use for indentation of JSON elements
   *               in the encoded output.
   * @return The encoded value.
   */
  public static String toJson(Object value, int indent, Target target) {
    if (value instanceof Literal<?> l) {
      if (value instanceof UncomputedExpression u) {
        try                { value = u.translate(target); }
        catch(Exception x) { value = u.translate(ESQL);   }
      } else {
        try {
          value = l.exec(target,
                         NULL_CONNECTION,
                         new EsqlPath(l),
                         HashPMap.empty(IntTreePMap.empty()),
                         NULL_DB.structure());
        } catch(Exception x) {
          value = l.exec(ESQL,
                         NULL_CONNECTION,
                         new EsqlPath(l),
                         HashPMap.empty(IntTreePMap.empty()),
                         NULL_DB.structure());
        }
      }
    } else if (value instanceof Expression<?,?> e) {
      try                { value = "$(" + e.translate(target) + ')'; }
      catch(Exception x) { value = "$(" + e.translate(ESQL) + ')';   }
    }

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

    } else if (value instanceof Date d) {
      return '"' + TO_JAVASCRIPT_DATE.format(d) + '"';

    } else if (value instanceof LocalDate d) {
      return '"' + DateTimeFormatter.ISO_LOCAL_DATE.format(d) + '"';

    } else if (value instanceof LocalTime d) {
      return '"' + DateTimeFormatter.ISO_LOCAL_TIME.format(d) + '"';

    } else if (value instanceof LocalDateTime d) {
      return '"' + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(d).replace('T', ' ') + '"';

    } else if (value instanceof StringForm sf) {
      StringBuilder st = new StringBuilder();
      sf._toString(st, 0, indent);
      return quote(st.toString());

    } else {
      /*
       * quote unsupported json types and expressions
       */
      return quote(value.toString());
    }
  }

  public static String toJson(Object value, int indent) {
    return toJson(value, indent, JAVASCRIPT);
  }

  /**
   * Encode the (database) value as a JSON value.
   * @param value The value to encode.
   * @return The encoded value.
   */
  public static String toJson(Object value) {
    return toJson(value, 0);
  }

  /**
   * To send data to a Javascript client, ignore time zone as this is not kept
   * in the database.
   */
  public static final SimpleDateFormat TO_JAVASCRIPT_DATE =
      new SimpleDateFormat("yyyy-MM-d H:m:s.S");
}
