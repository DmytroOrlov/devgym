import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

package object controllers {
  implicit val system = ActorSystem() //TODO: move to Guice module?
  implicit val mat = ActorMaterializer()

  // http param names
  val loginName = "user"
  val userName = "userName"
  val avatarUrl = "avatarUrl"
}
