import cats.Apply
import scala.collection.mutable.Queue
import parser.Parser
import parser.Parser.{given, _}
import cats.syntax.apply._

case class Ident(value: String)
case class Param(name: Ident, tpe: Ident)
case class Method(name: Ident, params: Queue[Param], tpe: Ident)

val whitespace = Parser.chars(' ').zeroOrMore.combineAll

val typeSep = whitespace *> Parser.char(':') <* whitespace
val paramSep = whitespace *> Parser.char(',') <* whitespace

val identParser =
  Parser.alphas.oneOrMore.combineAll.map(s => Ident(s))
val paramParser =
  (identParser <* typeSep, identParser)
    .mapN { Param(_, _) }

val paramList =
  (paramParser <* paramSep).zeroOrMore
    .combineWith(Queue.empty)(_ :+ _)
    .product(paramParser.orElse(Parser.empty))
    .map {
      case (a, b: Param) => a :+ b
      case (a, null) => a
    }

val methodName = Parser.string("def ") *> (identParser)
val methodParams = char('(') *> paramList <* char(')') 
val methodType = typeSep *> identParser
val methodParser =
  (methodName, methodParams, methodType)
    .mapN { case (a, b, c) => Method(a, b, c) }

methodName.flatMap:
  case Ident(name) if name.length() < 10 => ???
  case Ident(name) => ???

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
