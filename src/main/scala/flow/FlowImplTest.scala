package flow

object FlowImplTest {
  def main(string: Array[String]): Unit = {
    //    val iterator1 = List(1, 2, 3).map(Option(_)).iterator
    //        new FlowImpl[Int](() => {
    //          val iterator = iterator1
    //          if (iterator.hasNext) {
    //            val i = iterator.next()
    //            i
    //          } else Option.empty
    //        }).map(_ * 2).filter(_ > 2).map(_ + 1).forEach(println(_))
    Flow.of(List(1, 2, 3)) // return flow with a list producer
      .flatmap(i => { // return a flow with a producer that uses another flow as a source
        Flow.of(List(i * 1, i * 2, i * 3)) // returns a flow that have a list producer to the map function that inputs into flatten
      }).filter(_ > 2).map(_ + 1).forEach(println(_)) // flatten returns with a FlattenedFlow implementation that have a producer that produces Flow each time
    // calls its getNext which is the same as FlowFlattenedImpl getNext that calls the getNext of Flow
    // returned by the map function that produces another flow but it calls the getNext of the flow of lows
    // under the condition that the previous flow returned by the mentioned getNext has been terminated
    // expected output : 4 , 5 , 7 , 4 , 7 , 10
  }
}
