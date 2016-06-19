# Flake
[![Clojars Project](https://img.shields.io/clojars/v/flake.svg)](https://clojars.org/flake)
[![Build Status](https://travis-ci.org/maxcountryman/flake.svg?branch=master)](https://travis-ci.org/maxcountryman/flake)
[![Dependencies Status](https://jarkeeper.com/maxcountryman/flake/status.svg)](https://jarkeeper.com/maxcountryman/flake)

Decentralized, k-ordered unique ID generator.

This is a Clojure implementation of Boundary's [Erlang Flake ID service](https://github.com/boundary/flake).

## Usage

New flake IDs can be generated with the `generate!` fn from flake's core
namespace.

Note that in order to prevent generation of duplicate IDs, **`init!` must be
called prior to generating IDs for the first time**.

For example:

```clojure
=> (require '[flake.core :as flake])
=> (flake/init!)
=> (map flake/flake->bigint (take 3 (repeatedly flake/generate!)))
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
=> (->> (repeatedly flake/generate!)
        first
        flake/flake->bigint
        utils/base62-encode)
"8mwFA958SJ2CZVu9nk"
```

A flake's middle-most bits are derived from a hardware address, e.g. MAC. If
this is not desirable or the caller wishes to have more granular control over
which bits are used here, a custom `worker-id` may be provided:

```clojure
(import '[java.security SecureRandom])

(defn rand-bytes
  "Return `n` random bytes in an array."
  [n]
  (let [bs (byte-array n)]
    (.nextBytes (SecureRandom.) bs)
    bs))

(def worker-id (rand-bytes 6))

(first (repeatedly (partial flake/generate! worker-id)))
```

The above specifies a random array of bytes to be used as the worker-id.

# Specification

Flakes are byte sequences composed of 128 bits. These sequences are structured
such that the first 64 bits are a timestamp, i.e. the time since an epoch in
milliseconds, the next 48 bits are a unique, machine-specific bitset, normally
the MAC, and finally the remaining 16 bits are a monotonically increasing
short.

A diagram of the flake byte structure:

    [timestamp:64][MAC:48][sequence:16]

# Exceptions

Exceptions may occur if a single machine generates more than 65,535 flakes
within a millisecond. That puts the upper-bound on flake generation per machine
at ~65 million flakes per second. If this limit is reached a
`IllegalArgumentException` will be raised.

Additionally flake makes an effort to detect drift in system time and will
raise `IllegalStateException` if time appears to be flowing in the wrong
direction. Note that `init!` must be used for this to work properly!
