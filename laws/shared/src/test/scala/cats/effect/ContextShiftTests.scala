/*
 * Copyright (c) 2017-2018 The Typelevel Cats-effect Project Developers
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

package cats.effect

import cats.data.EitherT
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.ExecutionContext

class ContextShiftTests extends AsyncFunSuite with Matchers {
  implicit override def executionContext =
    ExecutionContext.global

  type EitherIO[A] = EitherT[IO, Throwable, A]


  test("ContextShift[IO].shift") {
    for (_ <- ContextShift[IO].shift.unsafeToFuture()) yield {
      assert(1 == 1)
    }
  }

  test("Timer[EitherT].shift") {
    for (r <- ContextShift.deriveIO[EitherIO].shift.value.unsafeToFuture()) yield {
      r shouldBe Right(())
    }
  }

}
