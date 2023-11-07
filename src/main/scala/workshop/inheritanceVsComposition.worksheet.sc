// // Inheritance
// trait Encoder:
//   def encode(): String
// trait Decoder[+A]:
//   def decode(s: String): A
// trait Combiner[-A]:
//   def combine(b: A): String

// abstract class Animal extends Encoder with Decoder[Animal] with Combiner[Animal]
// case class Dog() extends Animal:
//   override def encode(): String = ???
//   override def decode(s: String): Dog = ???
//   override def combine(b: Animal): String = ???
// case class Cat() extends Animal:
//   override def encode(): String = ???
//   override def decode(s: String): Cat = ???
//   override def combine(b: Animal): String = ???
// case class Bird() extends Animal:
//   override def encode(): String = ???
//   override def decode(s: String): Bird = ???
//   override def combine(b: Animal): String = ???

// // Composition
// trait Encoder[-A]:
//   def encode(a: A): String
// trait Decoder[+A]:
//   def decode(s: String): A
// trait Combiner[-A]:
//   def combine(a: A, b: A): String

// abstract class Animal
// case class Dog() extends Animal
// case class Cat() extends Animal
// case class Bird() extends Animal

// val catEncoder: Encoder[Cat] = ???
// val cat = Cat()
// catEncoder.encode(cat)

// Type class
trait Encoder[-A]:
  def encode(a: A): String
trait Decoder[+A]:
  def decode(s: String): A
trait Combiner[-A]:
  def combine(a: A, b: A): String

abstract class Animal
case class Dog() extends Animal
case class Cat() extends Animal
case class Bird() extends Animal

extension [A](a: A)
  def encode(using encoder: Encoder[A]): String = encoder.encode(a)
  def combine(b: A)(using combiner: Combiner[A]): String = combiner.combine(a, b)
extension (s: String) def decode[A](using decoder: Decoder[A]): A = decoder.decode(s)

given Encoder[Cat] with { def encode(a: Cat): String = "A cat" }
given Encoder[Animal] with { def encode(a: Animal): String = "An animal" }

val cat = Cat()
val encodedCat = cat.encode
val dog = Dog()
val encodedDog = dog.encode