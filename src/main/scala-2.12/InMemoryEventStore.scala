import ErrorHandling._

object InMemoryEventStore {
  type Data = Event
  private var streams = Map[String, List[(Data, Int)]]()

  private def getVersion(stream: List[(Data, Int)]): Int = stream.map(_._2).max

  def appendToStream(streamId: String, expectedVersion: Int, events: List[Event]): Either[Error, Unit] = {
    val streamOrError = streams.get(streamId) match {
      case Some(stream) if getVersion(stream) == expectedVersion =>
        Right(stream)
      case None if expectedVersion == -1 =>
        val stream = List[(Data, Int)]()
        streams = streams + (streamId -> stream)
        Right(stream)
      case _ =>
        Left("concurrency conflict")
    }

    val eventsWithVersion = events
      .zipWithIndex
      .map { case (e, i) => (e, expectedVersion + i + 1) }

    streamOrError
      .map(s => {
        streams = streams + (streamId -> (s ++ eventsWithVersion))
      })
  }

  def readFromStream(streamId: String): Either[Error, List[Event]] = {
    val events = streams.get(streamId) match {
      case Some(stream) =>
        stream
          .sortBy(_._2)
          .map(e => e._1)
      case None => List()
    }
    Right(events)
  }
}
