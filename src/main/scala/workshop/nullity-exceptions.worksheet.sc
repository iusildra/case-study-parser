import scala.util.control.NonFatal
import scala.util.Failure

case class Fail(message: String)
def noException = Fail("No exception was thrown")
def exception = Failure(Exception("An exception was thrown"))

val t0 = System.nanoTime()
for(_ <- 0 until 100000) do noException
val t1 = System.nanoTime()
for(_ <- 0 until 100000) do exception
val t2 = System.nanoTime()
for _ <- 0 until 100000 do try{throw Exception("boom")} catch {case NonFatal(_) => 1}

val t3 = System.nanoTime()

val withoutException = (t1 - t0) / 1e6
val withException = (t2 - t1) / 1e6
val thrownException = (t3 - t2) / 1e6


withException/withoutException
thrownException/withException

val adder: (Int, Int) => Int = _ + _