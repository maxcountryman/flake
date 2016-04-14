# Flake
[![Clojars Project](https://img.shields.io/clojars/v/flake.svg)](https://clojars.org/flake)
[![Build Status](https://travis-ci.org/maxcountryman/flake.svg?branch=master)](https://travis-ci.org/maxcountryman/flake)
[![Dependencies Status](https://jarkeeper.com/maxcountryman/flake/status.svg)](https://jarkeeper.com/maxcountryman/flake)

Decentralized, k-ordered unique ID generator.

This is a Clojure implementation of Boundary's [Erlang Flake ID service](https://github.com/boundary/flake).

## Usage

New flake IDs can be generated with the `generate` fn from flake's core
namespace.

Note that in order to prevent generation of duplicate IDs, *`init!` must be
called prior to generating IDs for the first time* to prevent potential
duplicate IDs.

For example:

```clojure
=> (require '[flake.core :as flake])
=> (flake/init!)
=> (take 3 (repeatedly flake/generate))
(25978563106299135585558915252224N
 25978563106299135585558915252225N
 25978563106299135585558915252226N)
```

Here we have generated three BigIntegers which are flake IDs. Note how they are
ordered.

It may be desirable to encode these IDs in a shorter representation, such as
Base62. The utils namespace provides an encoder:

```clojure
=> (require '[flake.utils :as utils])
=> (utils/base62-encode (repeatedly flake/generate))
"8mwFA958SJ2CZVu9nk"
```

# Specification

Flakes are byte sequences composed of 128 bits. These sequences are structured
such that the first 64 bits are a timestamp, i.e. the UNIX time in
milliseconds, the next 48 bits are a unique, machine-specific bitset, normally
the MAC, and finally the remaining 16 bits are a monotonically increasing
short.

A diagram of the flake byte structure:

    [timestamp:64][MAC:48][sequence:16]

Note that if a MAC is unavailable, a random byte array will be generated.
Because this byte array will vary between program invocations, this may break
lexicographic ordering.

# Exceptions

Exceptions may occur if a single machine generates more than 65,535 flakes
within a millisecond. That puts the upper-bound on flake generation per machine
at ~65 million flakes per second. If this limit is reached a
`IllegalArgumentException` will be raised.

Additionally flake makes an effort to detect drift in system time and will
raise `IllegalStateException` if time appears to be flowing in the wrong
direction.
