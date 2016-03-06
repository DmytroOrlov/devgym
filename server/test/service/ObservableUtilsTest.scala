package service

import monifu.reactive.OverflowStrategy.DropOld
import monifu.reactive.channels.PublishChannel
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}
import shared.Line

class ObservableUtilsTest extends FlatSpec with Matchers with MockFactory {

//  ignore should "complete channel and report on observable success" in {
//    //given
//    val channel = mock[PublishChannel[String]]
//    //when
//    (channel.pushNext(_: String)) expects Line.reportComplete
//    //(channel.pushComplete) expects
//    ObservableRunner(block = s => "success")
//    //
//    Thread.sleep(1000)
//  }
}
