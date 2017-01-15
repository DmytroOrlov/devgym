import com.google.inject.AbstractModule
import com.google.inject.name.Names.named

import scala.util.Random.nextInt

class Module extends AbstractModule {
  override def configure() =
    bind(classOf[String]).annotatedWith(named("Secret")) toInstance "devgym_" + nextInt(9999999)
}
