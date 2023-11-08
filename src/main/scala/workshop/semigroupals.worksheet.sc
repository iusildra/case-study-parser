import cats.data.Validated
import scala.concurrent.Future
import cats.Semigroupal
import cats.instances.future._
import scala.concurrent.ExecutionContext.Implicits.global
// given instances for List, Option, ... come with this import

trait MySemigroupal[F[_]]:
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]

def tupleAll[A, B, F[_]](fa: F[A], fb: F[B])(using semigroupal: Semigroupal[F]) =
  semigroupal.product(fa, fb)

tupleAll(List(1, 2, 3), List(4, 5, 6))
tupleAll(Option(1), Option(2))

/* ----------------------------- Monadic product ---------------------------- */
Semigroupal[List].product(
  List(1, 2),
  List(4, 5)
)
Semigroupal[Future].product(
  Future(new Exception("Boom")),
  Future(new Exception("Bang"))
)

/* --------------------------- Applicative product -------------------------- */
type ErrorsOr[A] = Validated[List[String], A]
Semigroupal[ErrorsOr].product(
  Validated.invalid(List("Error 1")),
  Validated.invalid(List("Error 2"))
)
Semigroupal[ErrorsOr].product(
  Validated.valid(List(1, 2)),
  Validated.valid(List(4, 2))
)
