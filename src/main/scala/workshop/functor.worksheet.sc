import scala.util.Try

trait Functor[F[_]]:
  def map[A, B](fa: F[A])(f: A => B): F[B]

given Functor[List] with
  def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
given Functor[Option] with
  def map[A, B](fa: Option[A])(f: A => B): Option[B] = fa.map(f)
given Functor[Try] with
  def map[A, B](fa: Try[A])(f: A => B): Try[B] = fa.map(f)

// extension [T, F[_]](fa: F[T])(using functor: Functor[F])
//   def map[B](f: T => B): F[B] = functor.map(fa)(f)

def mapThat[A, B, F[_]: Functor](fa: F[A])(f: A => B)(using functor: Functor[F]): F[B] =
  functor.map(fa)(f)

mapThat(List(1, 2, 3))(_ + 1)
mapThat(Option(1))(_ + 1)
mapThat(Try(1))(_ + 1)
