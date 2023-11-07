/* --------------------------- Contract definition -------------------------- */
trait JsonEncoder[-T]:
  def encode(entity: T): String

/* ------------------------------- Data types ------------------------------- */
sealed abstract class Animal
case class Cat (name: String, age: Int) extends Animal
case class Bird(name: String, age: Int) extends Animal

/* -------------------- Extension methods for convenience ------------------- */
extension [T <: Animal](animal: T)(using jsonEncoder: JsonEncoder[T])
  def toJson: String = jsonEncoder.encode(animal)

/* --------------------------- Contract instances --------------------------- */
given JsonEncoder[Cat] with
  def encode(cat: Cat) = s"""{"kind": "kitty", "name": "${cat.name}", "age": ${cat.age}}"""
given JsonEncoder[Animal] with
  def encode(entity: Animal): String = s"""{"kind": "animal", "info": "unknown"}"""

/* ------------------------------- Test cases ------------------------------- */
val cat = Cat("Scala", 12)
val bird = Bird("Haskell", 10)

cat.toJson // : String = {"kind": "kitty", "name": "Scala", "age": 12}
bird.toJson // : String = {"kind": "animal", "info": "unknown"}


given Int = 42
def foo(using i: Int): Int = i

foo
