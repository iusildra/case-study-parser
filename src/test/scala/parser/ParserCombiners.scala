package parser

import hedgehog._
import hedgehog.munit.HedgehogSuite
import parser.Parser.{given}
class ParserCombiners extends HedgehogSuite {
  property("string succeeds when 2 parsers are combined by a logical OR and the first parser succeeds") {
    for {
      expected <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
      suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
      input = expected + suffix
      result = Parser.string(expected).orElse(Parser.string("!" + expected)).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) =>
        fail(s"Parser failed on input $input when it should have failed")
      case parser.Success(_, _, _) => success
    }
  }

  property("string succeeds when 2 parsers are combined by a logical OR and the second parser succeeds") {
    for {
      expected <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
      suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
      input = expected + suffix
      result = Parser.string(suffix).orElse(Parser.string(expected)).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) =>
        fail(s"Parser failed on input $input when it should have failed")
      case parser.Success(_, _, _) => success
    }
  }

  property("string fails when 2 parsers are combined by a logical OR and both parsers fail") {
    for {
      prefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
      expected <- Gen.string(Gen.latin1, Range.linear(1, 10)).forAll
      badPrefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
      input = prefix + expected
      result = Parser.string(badPrefix).orElse(Parser.string(badPrefix)).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) => success
      case parser.Success(_, _, _) =>
        fail(s"Parser succeeded on input $input when it should have failed")
    }
  }

  property("string succeeds when 2 parsers are combined by a logical AND and both parsers succeed") {
    for {
      prefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
      suffix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
      input = prefix + suffix
      result = Parser.string(prefix).and(Parser.string(suffix)).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) =>
        fail(s"Parser failed on input $input when it should have succeeded")
      case parser.Success(_, _, _) => success
    }
  }

  property("string fails when 2 parsers are combined by a logical AND and the first parser fails") {
    for {
      prefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
      suffix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
      badPrefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
      input = prefix + suffix
      result = Parser.string(badPrefix).and(Parser.string(suffix)).parse(input)
    } yield result match {
      case parser.Failure(_, _, _) => success
      case parser.Success(_, _, _) =>
        fail(s"Parser succeeded on input $input when it should have failed")
    }
  }

  property("string fails when 2 parsers are combined by a logical AND and the second parser fails") {
    for {
      prefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
      suffix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
      badSuffix = suffix + "a"
      input = prefix + suffix
      result = Parser.string(prefix).and(Parser.string(badSuffix)).parse(input)
    } yield
      if (suffix == badSuffix) success
      else
        result match {
          case parser.Failure(_, _, _) => success
          case parser.Success(_, _, _) =>
            fail(s"Parser succeeded on input $input when it should have failed")
        }
  }

  property("string succeeds when parsers are producted") {
    case class Album(artist: String, name: String, year: Int)
    val stringParser =
      Parser.alphas
        .withChars(' ')
        .oneOrMore
        .combineAll
        .and(Parser.char(','))
    val intParser = Parser.digits.oneOrMore.combineAll

    val albumParser =
      stringParser
        .product(stringParser)
        .product(intParser)
        .map { case ((artist, name), year) =>
          Album(artist.dropRight(1), name.dropRight(1), year)
        }

    for {
      artist <- Gen.string(Gen.alpha, Range.linear(1, 10)).forAll
      name <- Gen.string(Gen.alpha, Range.linear(1, 10)).forAll
      year <- Gen.int(Range.linear(1, 10)).forAll
      input = artist + "," + name + "," + year
      result = albumParser.parse(input)
    } yield result match
      case parser.Success(Album(art, nme, yr), _, _) =>
        if (
          artist == art
          && name == nme
          && year == yr
        ) success
        else fail(s"Parser returned unexpected result")
      case parser.Failure(_, _, _) =>
        fail(s"Parser failed on input The BeatlesAbbey Road1969 when it should have succeeded")
  }
}
