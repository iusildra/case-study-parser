
/* -------------------------------------------------------------------------- */
/*                               TC introduction                              */
/* -------------------------------------------------------------------------- */
case class Car()
case class Boat()

val car = Car()
val boat = Boat()

/* ------------------------------ TC definition ----------------------------- */
trait Honker[T]:
  def honk: String

/* ----------------------- Methods using TC instances ----------------------- */
def hooonk[T](t: T)(using h: Honker[T]): String = h.honk

/* ------------------------------ TC instances ------------------------------ */
given Honker[Car] with
  def honk = "honk"

given Honker[Boat] with
  def honk = "hooooonk"

hooonk(car)
hooonk(boat)

/* ---------------------------- Extension methods --------------------------- */
extension [T](t: T)(using h: Honker[T])
  def honk: String = h.honk

car.honk
boat.honk


/* -------------------------------------------------------------------------- */
/*                                TC exercices                                */
/* -------------------------------------------------------------------------- */

case class Person(name: String, age: Int, address: String, town: String)
val lulu = Person("Lulu", 22, "43 Avenue d'Assas", "Montpellier")
val zozo = Person("Zozo", 22, "43 Avenue d'Assas", "Montpellier")
val lili = Person("Lili", 22, "43 Avenue d'Assas", "Montpellier")

// 1. Define a TC to serialize a person in JSON
trait JsonEncoder[-T]:
  def encode(t: T): String

// 2. Define the methods using the TC
def preparePerson(person: Person)(using encoder: JsonEncoder[Person]): String =
  encoder.encode(person)

// 3. Define a TC instance for Person
given JsonEncoder[Person] with
  def encode(person: Person): String =
    s"""{"name": "${person.name}", "age": ${person.age}, "address": "${person.address}", "town": "${person.town}"}"""

preparePerson(lulu)

// 4. Define extension methods
extension [T](t: T)(using encoder: JsonEncoder[T])
  def toJson: String = encoder.encode(t)

lulu.toJson

given [A](using JsonEncoder[A]): JsonEncoder[List[A]] with
  def encode(list: List[A]): String =
    list.map(_.toJson).mkString("[", ",", "]")

List(lulu, zozo, lili).toJson


given [T: JsonEncoder]: JsonEncoder[Option[T]] with
  def encode(option: Option[T]): String =
    option.map(_.toJson).getOrElse("null")


val list = List(Some(lulu), None, Some(zozo))
list.toJson