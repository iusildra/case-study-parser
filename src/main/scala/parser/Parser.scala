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

import cats.kernel.Monoid

/* -------------------------------------------------------------------------- */
/*                                General type                                */
/* -------------------------------------------------------------------------- */
sealed trait Parser[+A] {
  def map[B](f: A => B): Parser[B] = ParserMap(this, f)

  def parse(input: String): Result[A] = parse(input, 0)
  def parse(input: String, index: Int): Result[A]
}

object Parser {
  def int(value: Int): Parser[Int] = ParserInt(value)
  def string(value: String): Parser[String] = ParserString(value)
  def char(value: Char): Parser[String] = ParserString(value.toString)
  val empty: NullParser = NullParser()
  // val digits: Parser[Int] =
  //   (0 to 9)
  //     .map(Parser.int)
  //     .foldLeft[Parser[Int]](ParserFail)(_ orElse _)

  // val alphas = (('a' to 'z') ++ ('A' to 'Z'))
  //   .map(Parser.char)
  //   .foldLeft[Parser[String]](ParserFail)(_ orElse _)

  // def chars(chars: Char*) = chars
  //   .map(Parser.char)
  //   .foldLeft[Parser[String]](ParserFail)(_ orElse _)
  given intMonoid: Monoid[Int] with
    def empty: Int = 0
    def combine(x: Int, y: Int): Int = x * 10 + y
}

/* -------------------------------------------------------------------------- */
/*                                General kinds                               */
/* -------------------------------------------------------------------------- */
object ParserFail extends Parser[Nothing]:
  override def map[B](f: Nothing => B): Parser[B] = this
  // override def flatMap[B](f: Nothing => Parser[B]): Parser[B] = this
  override def parse(input: String, index: Int): Result[Nothing] = throw new UnsupportedOperationException(
    "Cannot parse with ParserFail"
  )

/* ------------------------------- Basic types ------------------------------ */
final case class NullParser() extends Parser[Null]:
  override def parse(input: String, index: Int): Result[Null] = Success(null, input, index)

final case class ParserInt(value: Int)(using Monoid[Int]) extends Parser[Int]:
  override def parse(input: String, index: Int): Result[Int] =
    if (input.startsWith(value.toString, index))
      Success(value, input, index + value.toString.size)
    else
      Failure(
        s"input did not start with $value at index $index",
        input,
        index
      )

final case class ParserString(value: String) extends Parser[String]:
  override def parse(input: String, index: Int): Result[String] =
    if (input.startsWith(value, index))
      Success(value, input, index + value.size)
    else
      Failure(
        s"input did not start with $value at index $index",
        input,
        index
      )

/* ------------------------------ Mapping types ----------------------------- */
final case class ParserMap[A, B](source: Parser[A], f: A => B) extends Parser[B]:
  override def parse(input: String, index: Int): Result[B] =
    source.parse(input, index).map(f)
