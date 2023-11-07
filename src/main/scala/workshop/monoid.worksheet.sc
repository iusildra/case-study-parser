import cats.Monoid
import cats.instances.int._

val aMonoid = Monoid[Int]
aMonoid.empty
aMonoid.combine(1, 2)

extension [A](fa: Seq[A])(using monoid: Monoid[A])
  def combineAll = fa.foldLeft(monoid.empty)(monoid.combine)

List(1, 2, 3).combineAll
Vector(1, 2, 3).combineAll
