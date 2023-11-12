import parser.Parser
import scala.util.Random
val chars = ('a' to 'z') ++ ('A' to 'Z')
val randomString = (1 to 10).map(_ => chars(Random.nextInt(chars.length))).mkString
val ascii_a = 'a'.toInt
val ascii_z = 'z'.toInt
val ascii_A = 'A'.toInt
val ascii_Z = 'Z'.toInt
val strParser = Parser.alphas.oneOrMore.combineAll

def test(f: => Unit) =
  val t0 = System.nanoTime()
  val n = 1000000
  for {
    _ <- 0 until n
  } f
  val t1 = System.nanoTime()
  s"${(t1 - t0) / 1e6} ms for $n iterations"

def parseWith(str: String)(p: Char => Boolean) =
  str.takeWhile(p)

test {
  randomString.forall(chars.contains)
}
test {
  parseWith(randomString): c =>
    (ascii_a <= c && c <= ascii_z)
    || (ascii_A <= c && c <= ascii_Z)
}
test {
  strParser.parse(randomString)
}

