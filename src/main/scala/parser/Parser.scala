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

import cats.Apply
import cats.kernel.Monoid
import cats.kernel.Semigroup

import scala.collection.mutable.Queue

/* -------------------------------------------------------------------------- */
/*                                General type                                */
/* -------------------------------------------------------------------------- */
sealed trait Parser[+A] {
  def map[B](f: A => B): Parser[B]
  def flatMap[B](f: A => Parser[B]): Parser[B]

  def orElse[B >: A](that: => Parser[B]): Parser[B] =
    (this, that) match
      case (ParserFail, _) => that
      case (_, ParserFail) => this
      case _ => ParserOrElse(this, that)

  def and[B >: A: Semigroup](that: Parser[B]): Parser[B] =
    (this, that) match
      case (ParserFail, _) | (_, ParserFail) => ParserFail
      case _ => ParserAnd(this, that)

  def dropRight[B](that: Parser[B]): Parser[A] =
    (this, that) match
      case (ParserFail, _) | (_, ParserFail) => ParserFail
      case _ => Ignored(used = this, ignored = that)

  def product[T >: A, B](that: => Parser[B]): Parser[(T, B)] =
    (this, that) match
      case (ParserFail, _) | (_, ParserFail) => ParserFail
      case _ => ParserProduct(this, that)

  def zeroOrMore[B >: A: Monoid]: KleeneStarParser[B] = KleeneStarParser(this)
  def oneOrMore[B >: A: Monoid]: RepeatedParser[B] = RepeatedParser(this)

  def parse(input: String): Result[A] = parse(input, 0)
  def parse(input: String, index: Int): Result[A]

  def and[B >: A: Monoid](others: Parser[B]*): Parser[B] =
    others.foldLeft[Parser[B]](this)(_ and _)

  def prettyPrint(level: Int): String
  override def toString(): String = prettyPrint(0)
}

object Parser {
  def int(value: Int): Parser[Int] = ParserInt(value)
  def string(value: String): Parser[String] = ParserString(value)
  def char(value: Char): Parser[String] = ParserString(value.toString)
  def fail[T]: Parser[T] = ParserFail

  val digits: Parser[Int] =
    List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
      .map(Parser.int)
      .foldLeft[Parser[Int]](ParserFail)(_ orElse _)

  val alphas = (('a' to 'z') ++ ('A' to 'Z'))
    .map(Parser.char)
    .foldLeft[Parser[String]](ParserFail)(_ orElse _)

  def chars(chars: Char*) = chars
    .map(Parser.char)
    .foldLeft[Parser[String]](ParserFail)(_ orElse _)

  extension (stringParser: Parser[String])
    def withChars(chars: Char*): Parser[String] =
      chars
        .map(Parser.char(_))
        .foldLeft[Parser[String]](stringParser)(_ orElse _)

  given intMonoid: Monoid[Int] with
    def empty: Int = 0
    def combine(x: Int, y: Int): Int = x * 10 + y

  given parserApply: Apply[Parser] with
    def map[A, B](fa: Parser[A])(f: A => B): Parser[B] = fa.map(f)
    def ap[A, B](ff: Parser[A => B])(fa: Parser[A]): Parser[B] =
      ff.product(fa).map((f, a) => f(a))

}

/* -------------------------------------------------------------------------- */
/*                                General kinds                               */
/* -------------------------------------------------------------------------- */
given Monoid[Nothing] with
  def empty: Nothing = ???
  def combine(x: Nothing, y: Nothing): Nothing = ???

object ParserFail extends Parser[Nothing]:
  override def map[B](f: Nothing => B): Parser[B] = this
  override def flatMap[B](f: Nothing => Parser[B]): Parser[B] = this
  override def parse(input: String, index: Int): Result[Nothing] = throw new UnsupportedOperationException(
    "Cannot parse with ParserFail"
  )
  override def prettyPrint(level: Int): String =
    s"ParserFail"

sealed abstract class ValidParser[A] extends Parser[A]:
  override def map[B](f: A => B): Parser[B] =
    ParserMap(this, f)

  override def flatMap[B](f: A => Parser[B]): Parser[B] =
    ParserFlatMap(this, f)

class KleeneStarParser[A](parser: Parser[A]) extends ValidParser[List[A]]:
  protected def loop(input: String, index: Int, result: Queue[A] = Queue.empty): (Int, Queue[A]) =
    val parsing = parser.parse(input, index)
    parsing match
      case fail: Failure => (index, result)
      case Success(res, _, offset) => loop(input, offset, result += res)

  override def parse(input: String, index: Int): Result[List[A]] =
    loop(input, index) match
      case (_, Queue()) => Result.success(List(), input, index)
      case (offset, results) =>
        Success(results.toList, input, offset)

  def combineAll(using monoid: Monoid[A]): Parser[A] =
    this.map(_.foldLeft(monoid.empty)(monoid.combine))

  override def prettyPrint(level: Int): String =
    val indent = "  "
    s"""|KleeneStarParser(
        |${indent * level}${parser.prettyPrint(level + 1)}
        |${indent * (level - 1)})""".stripMargin

case class RepeatedParser[A](parser: Parser[A]) extends KleeneStarParser[A](parser):
  override def parse(input: String, index: Int): Result[List[A]] =
    super.parse(input, index) match
      case Success(List(), _, _) =>
        Failure(
          "Repeated parser failed to parse at least one element",
          input,
          index
        )
      case res => res

  override def prettyPrint(level: Int): String =
    val indent = "  "
    s"""|RepeatedParser(
        |${indent * level}${parser.prettyPrint(level + 1)}
        |${indent * (level - 1)})""".stripMargin

/* -------------------------------------------------------------------------- */
/*                                    Types                                   */
/* -------------------------------------------------------------------------- */

/* ------------------------------- Basic types ------------------------------ */
final case class ParserInt(value: Int)(using Monoid[Int]) extends ValidParser[Int]:
  override def parse(input: String, index: Int): Result[Int] =
    if (input.startsWith(value.toString, index))
      Success(value, input, index + value.toString.size)
    else
      Failure(
        s"input did not start with $value at index $index",
        input,
        index
      )

  override def prettyPrint(level: Int): String =
    s"ParserInt($value)"

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

  override def prettyPrint(level: Int): String =
    s"ParserString($value)"

/* ------------------------------ Mapping types ----------------------------- */
final case class ParserMap[A, B](source: Parser[A], f: A => B) extends ValidParser[B]:
  override def parse(input: String, index: Int): Result[B] =
    source.parse(input, index).map(f)

  override def prettyPrint(level: Int): String =
    val indent = "  "
    s"""|ParserMap(
        |${indent * level}source = ${source.prettyPrint(level + 1)}
        |${indent * level}f = $f
        |${indent * (level - 1)})""".stripMargin

final case class ParserFlatMap[A, B](source: Parser[A], f: A => Parser[B]) extends ValidParser[B]:
  override def parse(input: String, index: Int): Result[B] =
    source.parse(input, index) match
      case fail: Failure => fail
      case Success(result, input, offset) =>
        f(result).parse(input, offset)

  override def prettyPrint(level: Int): String =
    val indent = "  "
    s"""|ParserFlatMap(
        |${indent * level}source = ${source.prettyPrint(level + 1)}
        |${indent * level}f = $f
        |${indent * (level - 1)})""".stripMargin

/* ------------------------------- Combinators ------------------------------ */
final case class ParserOrElse[A](left: Parser[A], right: Parser[A]) extends ValidParser[A]:
  override def parse(input: String, index: Int): Result[A] =
    left.parse(input, index) match
      case fail: Failure => right.parse(input, index)
      case success: Success[?] => success

  override def prettyPrint(level: Int): String =
    val indent = "  "
    s"""|ParserOrElse(
        |${indent * level}initial = ${left.prettyPrint(level + 1)}
        |${indent * level}recover = ${right.prettyPrint(level + 1)}
        |${indent * (level - 1)})""".stripMargin

final case class ParserAnd[A](left: Parser[A], right: Parser[A])(using semigroup: Semigroup[A]) extends ValidParser[A]:
  override def parse(input: String, index: Int): Result[A] =
    left.parse(input, index) match
      case Success(res, _, offset) =>
        right.parse(input, offset).map(semigroup.combine(res, _))
      case fail: Failure => fail

  override def prettyPrint(level: Int): String =
    val indent = "  "
    s"""|ParserAnd(
        |${indent * level}left = ${left.prettyPrint(level + 1)}
        |${indent * level}right = ${right.prettyPrint(level + 1)}
        |${indent * (level - 1)})""".stripMargin

final case class ParserProduct[A, B](left: Parser[A], right: Parser[B]) extends ValidParser[(A, B)]:
  override def parse(input: String, index: Int): Result[(A, B)] =
    left.parse(input, index) match
      case fail: Failure => fail
      case Success(leftResult, _, offset) =>
        right.parse(input, offset).map((leftResult, _))

  override def prettyPrint(level: Int): String =
    val indent = "  "
    s"""|ParserProduct(
        |${indent * level}left = ${left.prettyPrint(level + 1)}
        |${indent * level}right = ${right.prettyPrint(level + 1)}
        |${indent * (level - 1)})""".stripMargin

final case class Ignored[A, B](used: Parser[A], ignored: Parser[B]) extends ValidParser[A]:
  override def parse(input: String, index: Int): Result[A] =
    used.parse(input, index) match
      case fail: Failure => fail
      case Success(res, _, offset) => ignored.parse(input, offset).map(_ => res)

  override def prettyPrint(level: Int): String =
    val indent = "  "
    s"""|Ignored(
        |${indent * level}used = ${used.prettyPrint(level + 1)}
        |${indent * level}ignored = ${ignored.prettyPrint(level + 1)}
        |${indent * (level - 1)})""".stripMargin
