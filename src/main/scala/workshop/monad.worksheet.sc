trait Monad[F[_]]:
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]

given Monad[List] with
  def flatMap[A, B](fa: List[A])(f: A => List[B]): List[B] = fa.flatMap(f)
given Monad[Option] with
  def flatMap[A, B](fa: Option[A])(f: A => Option[B]): Option[B] = fa.flatMap(f)

def chain[A, B, F[_]: Monad](fa: F[A])(f: A => F[B])(using monad: Monad[F]): F[B] =
  monad.flatMap(fa)(f)

chain(List(1, 2, 3))(i => List(i, i + 1))
chain(Option(1))(i => Option(i + 1))