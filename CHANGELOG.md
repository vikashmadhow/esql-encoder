# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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