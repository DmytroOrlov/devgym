package service.reflection

import java.security._
import java.util.PropertyPermission

class TaskSecurityPolicy extends Policy {
  private val logger = org.slf4j.LoggerFactory.getLogger(this.getClass)

  // cache a reference to this protection domain (per p123 of Java Security)
  // this give us AllPermission
  private val providerDomain = {
    val p = this
    AccessController.doPrivileged(new PrivilegedAction[ProtectionDomain]() {
      override def run() = p.getClass.getProtectionDomain
    })
  }

  override def getPermissions(domain: ProtectionDomain): PermissionCollection = {
    val result: PermissionCollection = if (isPolicy(domain)) {
      new AllPermission().newPermissionCollection()
    } else if (isSandbox(domain)) {
      sandboxPermissions
    } else {
      appPermissions
    }
    result
  }

  override def implies(domain: ProtectionDomain, permission: Permission): Boolean = {
    val isImplied = if (isPolicy(domain)) {
      true
    } else {
      super.implies(domain, permission)
    }

    if (isSandbox(domain)) {
      logger.debug(s"implies: domainPermissions = [${domain.getPermissions}], permission = [$permission], result = $isImplied")
    }

    isImplied
  }

  private def isPolicy(domain: ProtectionDomain) = providerDomain == domain

  private def appPermissions: Permissions = {
    val permissions = new Permissions()
    permissions.add(new AllPermission())
    permissions
  }

  private def sandboxPermissions: Permissions = {
    val permissions = new Permissions()
    permissions.add(new PropertyPermission("*", "read"))
    permissions
  }

  private def isSandbox(domain: ProtectionDomain): Boolean = {
    domain.getClassLoader match {
      case cl: TaskClassLoader =>
        true
      case other =>
        false
    }
  }
}
