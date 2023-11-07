package parser

import hedgehog._
import hedgehog.munit.HedgehogSuite

class ParserProducts extends HedgehogSuite {
  // property("string succeeds when 2 parsers are combined and both succeed") {
  //   for {
  //     artist <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
  //     name <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
  //     input = s"$artist$name"
  //     result = Parser.string(artist).product(Parser.string(name)).parse(input)
  //   } yield result match {
  //     case parser.Failure(_, _, _) =>
  //       fail(s"Parser failed on input $input when it should have failed")
  //     case parser.Success(_, _, _) => success
  //   }
  // }

  // property("string succeeds when 3 parsers are combined and all succeed") {
  //   for {
  //     artist <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
  //     name <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
  //     year <- Gen.int(Range.linear(0, 2022)).forAll
  //     input = s"$artist$name$year"
  //     result = Parser.string(artist).product(Parser.string(name)).product(Parser.int(year)).parse(input)
  //   } yield result match {
  //     case parser.Failure(_, _, _) =>
  //       fail(s"Parser failed on input $input when it should have failed")
  //     case parser.Success(_, _, _) => success
  //   }
  // }

  // property("string fails when 2 parsers are combined and the first parser fails") {
  //   for {
  //     artist <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
  //     name <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
  //     year <- Gen.int(Range.linear(0, 2022)).forAll
  //     input = s"$artist - $name ($year)"
  //     result = Parser.string(artist + "!").product(Parser.string(name)).parse(input)
  //   } yield result match {
  //     case parser.Failure(_, _, _) => success
  //     case parser.Success(_, _, _) =>
  //       fail(s"Parser succeeded on input $input when it should have failed")
  //   }
  // }

  // property("string fails when 2 parsers are combined and the second parser fails") {
  //   for {
  //     artist <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
  //     name <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
  //     year <- Gen.int(Range.linear(0, 2022)).forAll
  //     input = s"$artist - $name ($year)"
  //     result = Parser.string(artist).product(Parser.string(name + "!")).parse(input)
  //   } yield result match {
  //     case parser.Failure(_, _, _) => success
  //     case parser.Success(_, _, _) =>
  //       fail(s"Parser succeeded on input $input when it should have failed")
  //   }
  // }
}
