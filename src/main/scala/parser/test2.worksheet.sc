import parser.Parser
import cats.kernel.Monoid
given intMonoid: Monoid[Int] with
    def empty: Int = 0
    def combine(x: Int, y: Int): Int = x * 10 + y

Parser.digits.and(Parser.digits).parse("12", 0).get
