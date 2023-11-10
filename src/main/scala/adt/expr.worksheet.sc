import adt._
import parser.Parser
import cats.implicits._

val whitespace = Parser.chars(' ').zeroOrMore.combineAll
val nbr = Parser.digits.oneOrMore.combineAll.map(i => Literal(i))
val nme = Parser.alphas.oneOrMore.combineAll.map(s => Variable(s))
val exp = nbr.orElse(nme)
val add = whitespace *> Parser.char('+') <* whitespace

val additionParser = (exp, add, exp).mapN((l, _, r) => l + r)
// val additionParser1: Parser[Add] = (exp, add, additionParser1.orElse(exp)).mapN((l, _, r) => l + r)
def additionParser2: Parser[Add] = (exp, add, additionParser2.orElse(exp)).mapN((l, _, r) => l + r)
// additionParser2.parse("1+2+3")
def additionParser3: Parser[Add] = (exp, add, Parser.lzy(additionParser3).orElse(exp)).mapN((l, _, r) => l + r)
additionParser3.parse("1+2+3")



def addition2: Parser[Expression] = (exp, (whitespace *> Parser.char('+') <* whitespace), Parser.lzy(addition2))
  .mapN((l, _, r) => l + r).orElse(exp)

val add1 = "1+2"
val add2 = "1 +2 + 3"
val add3 = "a + bc"
val add4 = "a +1 + dac + 8"

addition2.parse(add1)
addition2.parse(add2)
addition2.parse(add3)
addition2.parse(add4)

