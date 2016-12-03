package service.reflection

import java.security.Policy

//TODO: make this manager to check permission for exitVm, stopThread. They should come not from our custom class loader
class TaskSecurityManager extends SecurityManager {
  Policy.setPolicy(new TaskSecurityPolicy)

}
