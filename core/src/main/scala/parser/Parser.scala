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
import cats.Applicative

sealed trait Parser[A] {
  def map[B](f: A => B): Parser[B] =
    ParserMap(this, f)

  def orElse(that: => Parser[A]): Parser[A] =
    (this, that) match
      case (ParserFail(), _) => that
      case (_, ParserFail()) => this
      case _ => ParserOrElse(this, that)

  def product[B](that: => Parser[B]): Parser[(A, B)] =
    (this, that) match
      case (ParserFail(), _) => ParserFail()
      case (_, ParserFail()) => ParserFail()
      case _ => ParserProduct(this, that)

  def parse(input: String): Result[A] = parse(input, 0)
  def parse(input: String, index: Int): Result[A]
}

object Parser {
  def int(value: Int): Parser[Int] = ParserInt(value)
  def string(value: String): Parser[String] = ParserString(value)
  def fail[T]: Parser[T] = ParserFail()

  given Monoid[Parser[Int]] with
    def empty: Parser[Int] = ParserFail()
    def combine(x: Parser[Int], y: Parser[Int]): Parser[Int] = x.orElse(y)

  given Functor[Parser] with
    def map[A, B](fa: Parser[A])(f: A => B): Parser[B] =
      fa.map(f)

  given Applicative[Parser] with
    def pure[A](x: A): Parser[A] =
      x match
        case x: Int => ParserInt(x).asInstanceOf[Parser[A]]
        case x: String => ParserString(x).asInstanceOf[Parser[A]]
        case _ => ParserFail()
    def ap[A, B](ff: Parser[A => B])(fa: Parser[A]): Parser[B] =
      ff.product(fa).map { case (f, a) => f(a) }
}

final case class ParserFail[T]() extends Parser[T]:
  override def parse(input: String, index: Int): Result[T] =
    Failure("Bad parser", input, index)

final case class ParserInt(value: Int) extends Parser[Int]:
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

final case class ParserMap[A, B](source: Parser[A], f: A => B) extends Parser[B]:
  override def parse(input: String, index: Int): Result[B] =
    source.parse(input, index) match {
      case fail: Failure => fail
      case Success(result, input, offset) =>
        Success(f(result), input, offset)
    }

final case class ParserOrElse[A, B](left: Parser[A], right: Parser[B]) extends Parser[A | B]:
  override def parse(input: String, index: Int): Result[A | B] =
    left.parse(input, index) match {
      case fail: Failure => right.parse(input, index)
      case success: Success[?] => success
    }

final case class ParserProduct[A, B](left: Parser[A], right: Parser[B]) extends Parser[(A, B)]:
  override def parse(input: String, index: Int): Result[(A, B)] =
    left.parse(input, index) match
      case fail: Failure => fail
      case Success(leftResult, _, offset) =>
        right.parse(input, offset).map((leftResult, _))
