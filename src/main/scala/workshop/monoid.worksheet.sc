trait Monoid[T]:
  def empty: T
  def combine(a: T, b: T): T

given Monoid[Int] with
  def empty: Int = 0
  def combine(a: Int, b: Int): Int = a + b

def combineAll[A](fa: Seq[A])(using monoid: Monoid[A]): A =
  fa.foldLeft(monoid.empty)(monoid.combine)

combineAll(List(1, 2, 3))
combineAll(Seq(3, 2, 3))
