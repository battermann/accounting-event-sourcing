import java.util.UUID

object Application extends App {
  val store = EventStore(
    appendToStream = InMemoryEventStore.appendToStream,
    readFromStream = InMemoryEventStore.readFromStream
  )

  // dependency injection by partial application
  val handle = {
    CommandHandling.handleCommand(store, ProcessManager.process) _
  }

  def query(accountId: UUID) = {
    store
      .readFromStream(accountId.toString)
      .map(events => Domain.replay(Uninitialized, events))
  }

  val accountId1 = UUID.randomUUID()
  val accountId2 = UUID.randomUUID()

  val commands = List(
    (accountId1, CreateOnlineAccount(accountId1)),
    (accountId1, MakeDeposit(accountId1, 1000)),
    (accountId2, CreateOnlineAccount(accountId2)),
    (accountId1, MakeTransaction(accountId1, 500, accountId2))
  )

  commands foreach { case (id, cmd) =>
    println()
    val result = handle(id, cmd)
    println(cmd)
    println(s"${result.fold(err => s"[ERROR] $err", _ => "[SUCCESS]")}")
    println(s"current state: ${query(id).fold(err => "invalid", account => s"$account")}")
  }

  println(s"current state: ${query(accountId2).fold(err => "invalid", account => s"$account")}")
}
