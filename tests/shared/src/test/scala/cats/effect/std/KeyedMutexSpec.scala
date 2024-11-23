/*
 * Copyright 2020-2024 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats
package effect
package std

import cats.arrow.FunctionK
import cats.syntax.all._

import org.specs2.specification.core.Fragments

import scala.concurrent.duration._

final class KeyedMutexSpec extends BaseSpec with DetectPlatform {
  final override def executionTimeout = 2.minutes

  "ConcurrentKeyedMutex" should {
    tests(KeyedMutex.apply[IO, Int])
  }

  "KeyedMutex with dual constructors" should {
    tests(KeyedMutex.in[IO, IO, Int])
  }

  "MapK'd KeyedMutex" should {
    tests(KeyedMutex[IO, Int].map(_.mapK[IO](FunctionK.id)))
  }

  def tests(keyedMutex: IO[KeyedMutex[IO, Int]]): Fragments = {
    "execute action if free in the given key" in real {
      keyedMutex.flatMap { m => m.lock(key = 0).surround(IO.unit).mustEqual(()) }
    }

    "be reusable in the same key" in real {
      keyedMutex.flatMap { m =>
        val p = m.lock(key = 0).surround(IO.unit)

        (p, p).tupled.mustEqual(((), ()))
      }
    }

    "free key on error" in real {
      keyedMutex.flatMap { m =>
        val p =
          m.lock(key = 0).surround(IO.raiseError(new Exception)).attempt >>
            m.lock(key = 0).surround(IO.unit)

        p.mustEqual(())
      }
    }

    "free key on cancellation" in ticked { implicit ticker =>
      val p = for {
        m <- keyedMutex
        f <- m.lock(key = 0).surround(IO.never).start
        _ <- IO.sleep(1.second)
        _ <- f.cancel
        _ <- m.lock(key = 0).surround(IO.unit)
      } yield ()

      p must completeAs(())
    }

    "block action if not free in the given key" in ticked { implicit ticker =>
      keyedMutex.flatMap { m =>
        m.lock(key = 0).surround(IO.never) >>
          m.lock(key = 0).surround(IO.unit)
      } must nonTerminate
    }

    "not block action if using a different key" in ticked { implicit ticker =>
      keyedMutex.flatMap { m =>
        IO.race(
          m.lock(key = 0).surround(IO.never),
          IO.sleep(1.second) >> m.lock(key = 1).surround(IO.unit)
        ).void
      } must completeAs(())
    }

    "used concurrently in the same key" in ticked { implicit ticker =>
      keyedMutex.flatMap { m =>
        val p =
          IO.sleep(1.second) >>
            m.lock(key = 0).surround(IO.unit)

        (p, p).parTupled
      } must completeAs(((), ()))
    }

    "used concurrently with multiple keys" in ticked { implicit ticker =>
      keyedMutex.flatMap { m =>
        def p(key: Int): IO[Unit] =
          IO.sleep(1.second) >> m.lock(key).surround(IO.unit)

        List.range(start = 0, end = 10).parTraverse_(p)
      } must completeAs(())
    }
  }
}
