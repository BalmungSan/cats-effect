---
id: keyed-mutex
title: KeyedMutex
---

`KeyedMutex` is a concurrency primitive that can be used to give access to a keyed resource
to only **one** fiber at a time.

```scala
trait KeyedMutex[F[_], K] {
  def lock(key: K): Resource[F, Unit]
}
```

You can think of it as a `Key => Mutex`.
In other words, you get a different lock per each key.

**Caution**: This lock is not reentrant, thus this
`mutex.lock(key).surround(mutex.lock(key).use_)`
will deadlock.

## Using `KeyedMutex`

```scala mdoc:silent
import cats.effect.IO
import cats.effect.std.{KeyedMutex, MapRef}

trait State
trait Key

class Service(mutex: Mutex[IO], mapref: MapRef[IO, Key, State]) {
  def withState(key: Key)(f: State => IO[Unit]): IO[Unit] =
    mutex.lock(key).surround {
      for {
        current <- mapref(key).get
        _ <- f(current)
      } yield ()
    }
}
```
