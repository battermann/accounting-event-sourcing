import java.util.UUID

object Application extends App {
  val store = EventStore(
    appendToStream = InMemoryEventStore.appendToStream,
    readFromStream = InMemoryEventStore.readFromStream
  )

  // dependency injection by partial application
  val handle = CommandHandling.handleCommand(store) _

  def query(accountId: UUID) = {
    store
      .readFromStream(accountId.toString)
      .map(events => Domain.replay(Uninitialized, events))
  }

  val accountId = UUID.randomUUID()

  val commands = List(
    CreateOnlineAccount(accountId),
    MakeDeposit(accountId, 1000),
    Withdraw(accountId, 500),
    Withdraw(accountId, 501)
  )

  commands.foreach(cmd => {
    val result = handle(accountId, cmd)
    println(cmd)
    println(s"${result.fold(err => s"[ERROR] $err", _ => "[SUCCESS]")}")
    println(s"current state: ${query(accountId).fold(err => "invalid", account => s"$account")}")
  })
}
