# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.1] - 2022-07-06
### Added
- `type` attribute added to JSON encoded result metadata.
- Correct encoding of `UncomputedExpression` to run on client-side.

## [0.3.0] - 2022-06-28
### Added
- Encode expressions to calculate values of derived columns in table structures.
- Encode expressions to calculate values of computed attributes (e.g. `required: a % 2=0`).
- Change columns references in expressions to be calculated on client-side to 
  refer to the containing objects and values correctly.
- Attribute with expression values are now included in JSON encoding.
- Support for encoding of `java.time` `LocalDate`, `LocalTime` and `LocalDateTime`.
- Expressions are encoded in a form that can be executed in a Javascript context.
  If this encoding fails, the expression are encoded as an ESQL statement.
- Complies with ESQL 1.0.0.

## [0.2.5] - 2022-05-23
### Changed
- Complies with ESQL 0.9.7.

## [0.2.4] - 2022-05-17
### Changed
- Complies with ESQL 0.9.6.

## [0.2.3] - 2022-04-08
### Added
- Translation of values of type `StringForm` in `toJson`. 

## [0.2.2] - 2022-03-22
### Added
- `toJson` method without the `indent` parameter defaulting the latter to 0. 

## [0.2.1] - 2022-03-17
### Changed
- Complies with ESQL 0.9.1. 

## [0.2.0] - 2022-03-05
### Added
- Encoding of structure of relations.
- Control through parameters of which parts to encode: structure only, rows only,
  structure and rows (default).
- Utility methods to encode structure and rows into strings (using a `StringWriter`).

### Changed
- All methods documented.
- Gradle build file normalised.
- INDENT parameter constant to set number of indentation spaces in configuration.

## [0.1.3] - 2022-02-26
### Added
- Complies with changes to latest ESQL version (0.8.9).
- Testing of JSON valued attributes.

## [0.1.2] - 2022-02-17
### Changed
- Include changes to base Configuration methods.

## [0.1.1] - 2022-02-14
### Added
- Support for encoding maps and collections as Json arrays and objects.

## [0.1.0] - 2022-02-13
### Added
- Json result encoder.