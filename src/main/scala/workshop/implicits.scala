package workshop

object implicits:
  def foo(x: Int)(using y: Int): Int = x + y

object test:
  implicits.foo(1)