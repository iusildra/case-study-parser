import scala.collection.mutable.Queue
import parser.Parser
import parser.Parser.{given, _}
import cats.syntax.apply._

case class Ident(value: String)
case class Param(name: Ident, tpe: Ident)
case class Method(name: Ident, params: Queue[Param], tpe: Ident)

val whitespace =
  Parser
    .chars(' ', '\t', '\n')
    .zeroOrMore
    .combineAll

val typeParamSepParser =
  whitespace *> Parser.char(':') <* whitespace
val paramSepParser =
  whitespace *> Parser.char(',') <* whitespace

val identParser =
  Parser.alphas.oneOrMore.combineAll.map(s => Ident(s))
val paramParser =
  identParser
    .product(typeParamSepParser)
    .product(identParser)
    .map { case ((a, _), b) => Param(a, b) }

val paramListParser =
  (paramParser <* paramSepParser).zeroOrMore
    .combineWith(Queue.empty)(_ :+ _)
    .product(paramParser.orElse(Parser.empty))
    .map {
      case (a, b: Param) => a :+ b
      case (a, null) => a
    }

val methodParser =
  (Parser.string("def ") *> (identParser))
    .product(char('(') *> paramListParser <* char(')'))
    .product(typeParamSepParser *> identParser)
    .map { case ((a, b), c) => Method(a, b, c) }

methodParser.parse("def foo(): Int").get
methodParser.parse("def foo(x: Int): Int").get
methodParser.parse("def foo(x: Int, y: Int): Int").get

val stringParser = Parser.alphas.oneOrMore.combineAll
val intParser = Parser.digits.oneOrMore.combineAll
val stringOrInt: Parser[String | Int] =
  stringParser.orElse(intParser)
stringOrInt.parse("abc", 0).get match
  case s: String => s"String: $s"
  case i: Int => s"Int: $i"

stringOrInt.parse("123", 0).get match
  case s: String => s"String: $s"
  case i: Int => s"Int: $i"

