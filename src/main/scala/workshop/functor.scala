package workshop

import cats.Functor
import cats.instances.list._
import cats.instances.option._
import cats.instances.try_._
import scala.util.Try

object SimpleFunctors extends App:
  extension [T, F[_]](fa: F[T])(using functor: Functor[F])
    def fmap[B](f: T => B): F[B] = functor.map(fa)(f)

  List(1, 2, 3).fmap(_ + 1)
  Option(1).fmap(_ + 1)
  Try(1).fmap(_ + 1)
  Either.cond(true, 1, "error").fmap(_ + 1)

object ComplexFunctors extends App:
  val intListOption = List(Some(1), None, Some(2))
  val stringListOption = List(Some("a"), None, Some("b"))
  val intOptionList = Option(List(1, 2, 3))
  val stringOptionList = Option(List("a", "b", "c"))

  given [CC[_]: Functor, F[_]: Functor]: Functor[[A] =>> CC[F[A]]] = Functor[CC].compose[F]
  extension [T, F[_]: Functor, G[T]: Functor](fa: F[G[T]])
    def fmap[B](f: T => B): F[G[B]] = Functor[F].compose[G].map(fa)(f)

  println(intListOption.fmap(_ + 1))
  println(stringListOption.fmap(_ + "a"))
  println(intOptionList.fmap(_ + 1))
  println(stringOptionList.fmap(_ + "a"))
