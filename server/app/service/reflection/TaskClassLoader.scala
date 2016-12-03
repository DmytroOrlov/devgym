package service.reflection

import java.net.URLClassLoader

//import buildinfo.BuildInfo

class TaskClassLoader(parent: ClassLoader) extends ClassLoader { // extends URLClassLoader(BuildInfo.server.toList.toArray, parent) {
  import TaskClassLoader._

  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    if (forbiddenClasses.contains(name)) {
      throw new IllegalArgumentException("This functionality is disabled")
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
    """java.io.ObjectInputStream
      |java.io.ObjectOutputStream
      |java.io.ObjectStreamField
      |java.io.ObjectStreamClass
      |java.util.logging.Logger
      |java.sql.DriverManager
      |javax.sql.rowset.serial.SerialJavaObject
    """.stripMargin.split("\n").toSet

  // a bit extreme, but see http://www.security-explorations.com/materials/se-2014-02-report.pdf
  val javaClasses =
    """java.lang.Class
      |java.lang.ClassLoader
      |java.lang.Package
      |java.lang.invoke.MethodHandleProxies
      |java.lang.reflect.Proxy
      |java.lang.reflect.Constructor
      |java.lang.reflect.Method
    """.stripMargin.split("\n").toSet

  val forbiddenClasses: Set[String] = javaClasses ++ miscClasses

}
