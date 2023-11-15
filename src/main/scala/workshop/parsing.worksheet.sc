import parser.Parser
import parser.Parser.{given, _}
import cats.syntax.apply._

sealed trait Expr:
  def +(that: Expr): Add = Add(this, that)
  def *(that: Expr): Multiply = Multiply(this, that)

case class Num(value: Int) extends Expr
case class Var(name: String) extends Expr
case class Add(left: Expr, right: Expr) extends Expr
case class Multiply(left: Expr, right: Expr) extends Expr

val num = digits.oneOrMore.combineAll.map(s => Num(s))
val variable = alphas.oneOrMore.combineAll.map(s => Var(s))
val whitespace = chars(' ').zeroOrMore.combineAll
val plus = whitespace *> char('+') <* whitespace
val times = whitespace *> char('*') <* whitespace
val expr = variable.orElse(num)

val additionParser: Parser[Add] =
  (expr, plus, expr).mapN((l, _, r) => l + r)
def bigAdditionParser: Parser[Add] =
  (expr, plus, lzy(bigAdditionParser).orElse(expr)).mapN((l, _, r) => l + r)

val parser3: Parser[Add] =
  (expr, plus, lzy(parser3).orElse(expr)).mapN((l, _, r) => l + r)
parser3.parse("x + 1 + a").get
