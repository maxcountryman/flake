# Flake

## [Unreleased]

## [0.4.4] - 2016-08-04
### Added
- A utility macro, `with-timestamp`, which can be used to contain flake
    generation to a bounded time window. While unlikely, `generate!` could
    repeatedly lose the race to generate a flake which could be undesirable--
    wrapping `generate!` with `with-timeout` addresses this issue.

### Changed
- Replaced the use of `read-string` with Integer/parseInt in reading timestamps
    from disk. This is generally safer, as `read-string` is essentially
    `eval`.

## [0.4.3] - 2016-07-08
### Fixed
- The ordering of the base 62 alphabet used to generate base 62 numbers was
misasligned such that IDs produced using this function could sort improperly.
This is now fixed.

## [0.4.2] - 2016-06-21
### Changed
- Replaced usage of `System/currentTimeMillis` with a custom epoch derived from
    the difference between `System/currentTimeMillis` and `System/nanonTime`.
    In order to protect against clock skew, a sampling of deltas between the
    two is taken and the average is used as the epoch for yielding new
    timestamps.
- BREAKING: The persistent timer now uses the same epoch used to generate
    timestamps. As a result the arity of `write-timestamp` was changed to
    include the epoch as the final argument.

### Fixed
- A potential race condition existed where, if two threads called `generate!`
    simultaneously, the fact that the timestamp comparison was done outside of
    the swap update function could result in duplicate IDs.

## [0.4.1] - 2016-06-03
### Fixed
- Catch java.io.IOException when reading the timer path--fixes an issue where
    non-existant paths threw uncaught exceptions.

## [0.4.0] - 2016-06-02
### Added
- Better hardware address discovery. Far more robust, filtering out `null`
devices and picking the first seemingly valid hardware address.

### Changed
- Removed random worker ID assignment, when no hardware address was found.
- BREAKING: `generate` is now `generate!` in addition, other functions removed.
- BREAKING: `generate!` now returns a ByteBuffer--use `flake->bigint` to
    maintain backwards compatibility.
- BREAKING: Persistent timer moved to its own namespace, `flake.timer`.

## [0.3.2] - 2016-04-14
### Changed
- Better network interface fallback--this involves filtering out loopback
devices.

## [0.3.1] - 2015-06-29
### Added
- Support for creating the worker ID portion of IDs via `SecureRandom` when
no hardware interfaces can be found.

## [0.3.0] - 2015-06-11
### Fixed
- An equality check that wasn't wrapped in the `clojure.test/is` macro.
- The ordering of `write-timestamp` was reversed, resulting in an exception.

## [0.2.0] - 2014-08-23
### Fixed
- Persistent timer sleep duration.

## [0.1.0] - 2014-08-19
### Added
- Published project.

[Unreleased]: https://github.com/maxcountryman/flake/compare/0.4.4...HEAD
[0.4.4]: https://github.com/maxcountryman/flake/compare/0.4.3...0.4.4
[0.4.3]: https://github.com/maxcountryman/flake/compare/0.4.2...0.4.3
[0.4.2]: https://github.com/maxcountryman/flake/compare/0.4.1...0.4.2
[0.4.1]: https://github.com/maxcountryman/flake/compare/0.4.0...0.4.1
[0.4.0]: https://github.com/maxcountryman/flake/compare/0.3.2...0.4.0
[0.3.2]: https://github.com/maxcountryman/flake/compare/0.3.1...0.3.2
[0.3.1]: https://github.com/maxcountryman/flake/compare/0.3.0...0.3.1
[0.3.0]: https://github.com/maxcountryman/flake/compare/0.2.0...0.3.0
[0.2.0]: https://github.com/maxcountryman/flake/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/maxcountryman/flake/compare/0.1.0...0.1.0
