import adt._
import parser.Parser
import cats.implicits._

val whitespace = Parser.chars(' ', '\t', '\n').zeroOrMore.combineAll
val literal = Parser.digits.oneOrMore.combineAll.map(Literal.apply)
val variable = Parser.alphas.oneOrMore.combineAll.map(Variable.apply)

val factor: Parser[Expression] = literal.orElse(variable)

val addition = (factor, (whitespace *> Parser.char('+') <* whitespace), factor)
  .mapN((l, _, r) => l + r)

def addition2: Parser[Expression] = (factor, (whitespace *> Parser.char('+') <* whitespace), Parser.lzy(addition2))
  .mapN((l, _, r) => l + r).orElse(factor)

val add1 = "1+2"
val add2 = "1 +2 + 3"
val add3 = "a + bc"
val add4 = "a +1 + dac + 8"

addition2.parse(add1)
addition2.parse(add2)
addition2.parse(add3)
addition2.parse(add4)

