# Flake

Decentralized, k-ordered unique ID generator.

This is a Clojure implementation of Boundary's [Erlang Flake ID service](https://github.com/boundary/flake).

## Installation
`flake` is available via [Clojars](https://clojars.org/flake):

```clojure
[flake "0.2.0"]
```

## Usage

New flake IDs can be generated with the `generate` fn from flake's core
namespace.

Note that in order to prevent generation of duplicate IDs, `init!` should be
called prior to generating IDs for the first time to prevent potential
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
=> (utils/base62-encode (flake/generate))
"8mwFA958SJ2CZVu9nk"
```

## Exceptional cases

When generating flakes, if system time [flows backwards](http://aphyr.com/posts/299-the-trouble-with-timestamps) then an `IllegalStateException` will be thrown. In the extremely unlikely event that you generate more than 65,535 flakes on one machine in a millisecond (65 million flakes/second) then an `IllegalArgumentException` will be thrown as the flake sequence id (of type short) overflows.
