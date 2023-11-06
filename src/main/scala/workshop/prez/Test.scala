package workshop.prez

object Test:
  trait Encoder[-A] { def encode (a: A): String }

  abstract class Animal
  case class Cat() extends Animal

  extension [A](a: A)(using encoder: Encoder[A])
    def encode: String = encoder.encode(a)

  given catEncoder: Encoder[Cat] with
    def encode(a: Cat) = "A cat"

  val str = Cat().encode // rewritten as encode(Cat(), catEncoder)
  // val str: String = "A cat"