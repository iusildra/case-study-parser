import cats.Applicative
import cats.syntax.apply._

val list1 = List(1, 2, 3)
val list2 = List(4, 5, 6)

def product[W[_], A, B](wa: W[A], wb: W[B])(using app: Applicative[W]): W[(A, B)] =
  val functionWrapper: W[B => (A, B)] = app.map(wa)(a => (b: B) => (a, b))
  app.ap(functionWrapper)(wb)

product(list1, list2)

val f: (Int, Int) => Int = _ + _
val intList1 = List(5, 10, 15)
val intList2 = List(0, 1)

val adder = intList1.map(i1 => (i2: Int) => f(i1, i2))
// charAdder = Some((c: Char) => f(5, c))
adder.ap(intList2)
