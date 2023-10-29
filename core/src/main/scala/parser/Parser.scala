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
import cats.syntax.semigroup._

/* -------------------------------------------------------------------------- */
/*                                General type                                */
/* -------------------------------------------------------------------------- */
sealed trait Parser[A: Monoid] {
  def map[B: Monoid](f: A => B): Parser[B]
  def flatMap[B: Monoid](f: A => Parser[B]): Parser[B]

  def orElse(that: => Parser[A]): Parser[A] =
    (this, that) match
      case (ParserFail(), _) => that
      case (_, ParserFail()) => this
      case _ => ParserOrElse(this, that)

  def and(that: Parser[A]): Parser[A] =
    (this, that) match
      case (ParserFail(), _) => ParserFail()
      case (_, ParserFail()) => ParserFail()
      case _ => ParserOrElse(this, that)

  def product[B: Monoid](that: => Parser[B]): Parser[(A, B)] =
    (this, that) match
      case (ParserFail(), _) => ParserFail()
      case (_, ParserFail()) => ParserFail()
      case _ => ParserProduct(this, that)

  def repeat: Parser[A] = RepeatedParser(this)

  def parse(input: String): Result[A] = parse(input, 0)
  def parse(input: String, index: Int): Result[A]

  def and(others: Parser[A]*): Parser[A] =
    others.foldLeft(this)(_ orElse _)
}

object Parser {
  def int(value: Int): Parser[Int] = ParserInt(value)
  def string(value: String): Parser[String] = ParserString(value)
  def fail[T: Monoid]: Parser[T] = ParserFail()

  given Monoid[Parser[Int]] with
    def empty: Parser[Int] = ParserFail()
    def combine(x: Parser[Int], y: Parser[Int]): Parser[Int] = x.orElse(y)
}

/* -------------------------------------------------------------------------- */
/*                                General kinds                               */
/* -------------------------------------------------------------------------- */
sealed abstract class InvalidParser[T: Monoid] extends Parser[T]:
  override def map[B: Monoid](f: T => B): Parser[B] = ParserFail()
  override def flatMap[B: Monoid](f: T => Parser[B]): Parser[B] = ParserFail()

sealed abstract class ValidParser[A: Monoid] extends Parser[A]:
  override def map[B: Monoid](f: A => B): Parser[B] =
    ParserMap(this, f)

  override def flatMap[B: Monoid](f: A => Parser[B]): Parser[B] =
    ParserFlatMap(this, f)

private class RepeatedParser[A: Monoid](parser: Parser[A]) extends Parser[A]:
  override def map[B: Monoid](f: A => B): Parser[B] =
    ParserMap(this, f)

  override def flatMap[B: Monoid](f: A => Parser[B]): Parser[B] =
    ParserFlatMap(this, f)

  override def parse(input: String, index: Int): Result[A] =
    def loop(index: Int, result: List[A]): (Int, List[A]) =
      parser.parse(input, index) match
        case fail: Failure => (index, result)
        case Success(res, _, offset) => loop(offset, res :: result)

    loop(index, Nil) match
      case (_, Nil) => Result.success(???, input, index)
      case (offset, results) =>
        val output = results
          .foldLeft(Monoid[A].empty) { (acc, next) =>
            acc.combine(next)
          }
        Success(output, input, offset)

/* -------------------------------------------------------------------------- */
/*                                    Types                                   */
/* -------------------------------------------------------------------------- */

/* ------------------------------- Basic types ------------------------------ */
final case class ParserFail[T: Monoid]() extends InvalidParser[T]:
  override def parse(input: String, index: Int): Result[T] =
    Failure("Bad parser", input, index)

final case class ParserInt(value: Int) extends ValidParser[Int]:
  override def parse(input: String, index: Int): Result[Int] =
    if (input.startsWith(value.toString, index))
      Success(value, input, index + value.toString.size)
    else
      Failure(
        s"input did not start with $value at index $index",
        input,
        index
      )
final case class ParserString(value: String) extends ValidParser[String]:
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
final case class ParserMap[A, B: Monoid](source: Parser[A], f: A => B) extends ValidParser[B]:
  override def parse(input: String, index: Int): Result[B] =
    source.parse(input, index) match
      case fail: Failure => fail
      case Success(result, input, offset) =>
        Success(f(result), input, offset)

final case class ParserFlatMap[A, B: Monoid](source: Parser[A], f: A => Parser[B]) extends ValidParser[B]:
  override def parse(input: String, index: Int): Result[B] =
    source.parse(input, index) match
      case fail: Failure => fail
      case Success(result, input, offset) =>
        f(result).parse(input, offset)

/* ------------------------------- Combinators ------------------------------ */
final case class ParserOrElse[A: Monoid](left: Parser[A], right: Parser[A]) extends ValidParser[A]:
  override def parse(input: String, index: Int): Result[A] =
    left.parse(input, index) match
      case fail: Failure => right.parse(input, index)
      case success: Success[?] => success

final case class ParserAnd[A: Monoid](left: Parser[A], right: Parser[A]) extends ValidParser[A]:
  override def parse(input: String, index: Int): Result[A] =
    left.parse(input, index) match
      case Success(res, _, offset) => right.parse(input, offset).map(_ |+| res)
      case fail: Failure => fail

final case class ParserProduct[A: Monoid, B: Monoid](left: Parser[A], right: Parser[B]) extends ValidParser[(A, B)]:
  override def parse(input: String, index: Int): Result[(A, B)] =
    left.parse(input, index) match
      case fail: Failure => fail
      case Success(leftResult, _, offset) => right.parse(input, offset).map((leftResult, _))
