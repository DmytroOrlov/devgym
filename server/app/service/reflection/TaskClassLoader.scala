package service.reflection

class TaskClassLoader(parent: ClassLoader) extends ClassLoader(parent) {

  import TaskClassLoader._

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    if (forbiddenClasses.contains(name)) {
      throw new IllegalArgumentException(s"This class is disabled: $name")
    }
    super.loadClass(name, resolve)
  }

  override def findClass(name: String): Class[_] = {
    super.findClass(name)
  }
}

object TaskClassLoader {

  // There are various classes that have weaker security privileges and only check the immediate parent.
  // The simplest way to deal with them is to not let them be instantiated in the first place.

  val miscClasses =
    """java.io.ObjectStreamClass
      |java.util.logging.Logger
      |java.sql.DriverManager
      |javax.sql.rowset.serial.SerialJavaObject
      |java.io.File
      |java.io.FileInputStream
      |java.io.FileOutputStream"""
      .stripMargin.split("\n").toSet

  // a bit extreme, but see http://www.security-explorations.com/materials/se-2014-02-report.pdf
  val javaClasses =
    """java.lang.invoke.MethodHandleProxies
      |java.lang.reflect.Proxy
      |java.lang.System
      |java.lang.Thread"""
      .stripMargin.split("\n").toSet

  val forbiddenClasses: Set[String] = javaClasses ++ miscClasses

}
