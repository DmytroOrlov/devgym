import monifu.reactive.Observable
import shared.Line

import scala.concurrent.Future

package object service {

  implicit class RichObservableFuture(val of: (Observable[Line], Future[String])) extends AnyVal {
    def observable = of._1

    def future = of._2
  }

}
