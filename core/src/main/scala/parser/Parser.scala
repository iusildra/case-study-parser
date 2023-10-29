/*
 * Copyright 2022 Creative Scala
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

package parser

import cats.Functor
import cats.kernel.Monoid

sealed trait Parser[A] {
  import Parser._

  def map[B](f: A => B): Parser[B] =
    ParserMap(this, f)

  def orElse(that: => Parser[A]): Parser[A] =
    (this, that) match
      case (ParserFail(), _) => that
      case (_, ParserFail()) => this
      case _ => ParserOrElse(this, that)

  def parse(input: String): Result[A] = {
    def loop[T](parser: Parser[T], index: Int): Result[T] =
      parser match {
        case ParserMap(source, f) =>
          loop(source, index) match {
            case fail: Failure => fail
            case Success(result, input, offset) =>
              Success(f(result), input, offset)
          }

        case ParserString(value) =>
          if (input.startsWith(value, index))
            Success(value, input, index + value.size)
          else
            Failure(
              s"input did not start with $value at index $index",
              input,
              index
            )
        case ParserOrElse(left, right) =>
          loop(left, index) match {
            case fail: Failure => loop(right, index)
            case success: Success[?] => success
          }
        case ParserFail() => Failure("Bad parser", input, index)
      }

    loop(this, 0)
  }
}
object Parser {
  def string(value: String): Parser[String] = ParserString(value)
  def fail[T]: Parser[T] = ParserFail()

  final case class ParserFail[T]() extends Parser[T]
  final case class ParserString(value: String) extends Parser[String]
  final case class ParserMap[A, B](source: Parser[A], f: A => B) extends Parser[B]
  final case class ParserOrElse[A, B](left: Parser[A], right: Parser[B]) extends Parser[A | B]

  given Functor[Parser] with
    def map[A, B](fa: Parser[A])(f: A => B): Parser[B] =
      fa.map(f)
  given Monoid[Parser[Int]] with
    def empty: Parser[Int] = ParserFail()
    def combine(x: Parser[Int], y: Parser[Int]): Parser[Int] = x.orElse(y)
}
