import cats.kernel.Monoid
import parser.Parser
import cats.syntax.semigroup._

extension [A: Monoid](fa: Seq[A]) def combineAll = fa.foldLeft(Monoid[A].empty)(_ combine _)

val digit: Parser[Int] =
  List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
  .map(x => Parser.int(x))
  .combineAll

digit.parse("200") match
  case parser.Success(result, input, offset) =>
    println(s"Success: $result, $input, $offset")
  case parser.Failure(reason, input, start) =>
    println(s"Failure: $reason, $input, $start")

val albumParser = Parser.string("album")
val artistParser = Parser.string("artist")
val yearParser = Parser.int(2023)

albumParser.product(artistParser).product(yearParser)