package controllers

import org.mockito.Mockito.{mock => mockitoMock}
import reflect.Manifest
import org.mockito.stubbing.Answer
import org.mockito.MockSettings

trait MockitoSugar {

  def mockito[T <: AnyRef](implicit manifest: Manifest[T]): T = {
    mockitoMock(manifest.erasure.asInstanceOf[Class[T]])
  }

  def mockito[T <: AnyRef](defaultAnswer: Answer[_])(implicit manifest: Manifest[T]): T = {
    mockitoMock(manifest.erasure.asInstanceOf[Class[T]], defaultAnswer)
  }

  def mockito[T <: AnyRef](mockSettings: MockSettings)(implicit manifest: Manifest[T]): T = {
    mockitoMock(manifest.erasure.asInstanceOf[Class[T]], mockSettings)
  }

  def mockito[T <: AnyRef](name: String)(implicit manifest: Manifest[T]): T = {
    mockitoMock(manifest.erasure.asInstanceOf[Class[T]], name)
  }
}
