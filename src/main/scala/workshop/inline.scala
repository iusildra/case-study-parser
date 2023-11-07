inline def foo(inline x: Int) =
  if (x % 2 == 0) "even"
  else "odd"

object Inline extends App:
  val x = foo(1)

  val y = foo(2)