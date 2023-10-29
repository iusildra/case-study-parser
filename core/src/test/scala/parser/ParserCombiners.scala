// package parser

// import hedgehog._
// import hedgehog.munit.HedgehogSuite
// class ParserCombiners extends HedgehogSuite {
//   property("string succeeds when 2 parsers are combined by a logical AND with expected values") {
//     for {
//       expected1 <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
//       expected2 <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
//       suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
//       input = expected1 ++ expected2 ++ suffix
//       result = Parser.string(expected1).combine(Parser.string(expected2)).parse(input)
//     } yield result match {
//       case parser.Failure(_, _, _) =>
//         fail(s"Parser failed on input $input when it should have failed")
//       case parser.Success(_, _, _) => success
//     }
//   }

//   property("string fails when 2 parsers are combined by a logical AND and the first parser fails") {
//     for {
//       prefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
//       expected <- Gen.string(Gen.latin1, Range.linear(1, 10)).forAll
//       input = prefix ++ expected
//       result = Parser.string(expected).combine(Parser.string(expected)).parse(input)
//     } yield result match {
//       case parser.Failure(_, _, _) => success
//       case parser.Success(_, _, _) =>
//         fail(s"Parser succeeded on input $input when it should have failed")
//     }
//   }

//   property("string fails when 2 parsers are combined by a logical AND and the second parser fails") {
//     for {
//       prefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
//       expected <- Gen.string(Gen.latin1, Range.linear(1, 10)).forAll
//       input = prefix ++ expected
//       result = Parser.string(prefix).combine(Parser.string(prefix)).parse(input)
//     } yield result match {
//       case parser.Failure(_, _, _) => success
//       case parser.Success(_, _, _) =>
//         fail(s"Parser succeeded on input $input when it should have failed")
//     }
//   }

//   property("string succeeds when 2 parsers are combined by a logical OR and the first parser succeeds") {
//     for {
//       expected <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
//       suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
//       input = expected ++ suffix
//       result = Parser.string(expected).orElse(Parser.string("!" + expected)).parse(input)
//     } yield result match {
//       case parser.Failure(_, _, _) =>
//         fail(s"Parser failed on input $input when it should have failed")
//       case parser.Success(_, _, _) => success
//     }
//   }

//   property("string succeeds when 2 parsers are combined by a logical OR and the second parser succeeds") {
//     for {
//       expected <- Gen.string(Gen.latin1, Range.linear(0, 10)).forAll
//       suffix <- Gen.string(Gen.latin1, Range.linear(0, 35)).forAll
//       input = expected ++ suffix
//       result = Parser.string(suffix).orElse(Parser.string(expected)).parse(input)
//     } yield result match {
//       case parser.Failure(_, _, _) =>
//         fail(s"Parser failed on input $input when it should have failed")
//       case parser.Success(_, _, _) => success
//     }
//   }

//   property("string fails when 2 parsers are combined by a logical OR and both parsers fail") {
//     for {
//       prefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
//       expected <- Gen.string(Gen.latin1, Range.linear(1, 10)).forAll
//       badPrefix <- Gen.string(Gen.latin1, Range.linear(1, 35)).forAll
//       input = prefix ++ expected
//       result = Parser.string(badPrefix).orElse(Parser.string(badPrefix)).parse(input)
//     } yield result match {
//       case parser.Failure(_, _, _) => success
//       case parser.Success(_, _, _) =>
//         fail(s"Parser succeeded on input $input when it should have failed")
//     }
//   }
// }
