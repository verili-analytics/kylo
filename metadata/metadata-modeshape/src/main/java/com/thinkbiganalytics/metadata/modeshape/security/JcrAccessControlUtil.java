/**
 * 
 */
package com.thinkbiganalytics.metadata.modeshape.security;

import java.security.Principal;
import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

import com.thinkbiganalytics.metadata.modeshape.MetadataRepositoryException;

/**
 *
 * @author Sean Felten
 */
public class JcrAccessControlUtil {
    
    public static boolean addPermissions(Node node, Principal principal, String... privilegeNames) {
        try {
            return addPermissions(node.getSession(), node.getPath(), principal, privilegeNames);
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to add permission(s) to node " + node + ": " + privilegeNames, e);
        }
    }

    public static boolean addPermissions(Node node, Principal principal, Privilege... privileges) {
        try {
            return addPermissions(node.getSession(), node.getPath(), principal, privileges);
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to add permission(s) to node " + node + ": " + privileges, e);
        }

    }
    
    public static boolean addPermissions(Session session, String path, Principal principal, String... privilegeNames) {
        try {
            AccessControlManager acm = session.getAccessControlManager();
            Privilege[] privs = new Privilege[privilegeNames.length];
            
            for (int i = 0; i < privilegeNames.length; i++) {
                privs[i] = acm.privilegeFromName(privilegeNames[i]);
            }
            
            return addPermissions(session, path, principal, privs);
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to add permission(s) to node " + path + ": " + privilegeNames, e);
        }
    }
    
    public static boolean addPermissions(Session session, String path, Principal principal, Privilege... privileges) {
        try {
            AccessControlManager acm = session.getAccessControlManager();
            AccessControlList acl = null;
            AccessControlPolicyIterator it = acm.getApplicablePolicies(path);
            
            if (it.hasNext()) {
                acl = (AccessControlList)it.nextAccessControlPolicy();
            } else {
                acl = (AccessControlList)acm.getPolicies(path)[0];
            }
            
            boolean added = acl.addAccessControlEntry(principal, privileges);
            acm.setPolicy(path, acl);
            
            return added;
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to add permission(s) to node " + path + ": " + privileges, e);
        }
        
    }
    
    public static boolean addHierarchyPermissions(Node node, Principal principal, Node toNode, String... privilegeNames) {
        try {
            Node endNode = toNode;
            Node current = node;
            boolean added = false;
            
            do {
                added |= addPermissions(current, principal, privilegeNames);
                current = current.getParent();
            } while (! current.equals(endNode) && ! current.equals(toNode.getSession().getRootNode()));
            
            return added;
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to add permission(s) to hierarch from node " + node + " up to " + toNode, e);
        }
    }

    public static boolean removePermissions(Session session, String path, Principal principal, String... privilegeNames) {
        try {
            AccessControlManager acm = session.getAccessControlManager();
            Privilege[] privs = new Privilege[privilegeNames.length];
            
            for (int i = 0; i < privilegeNames.length; i++) {
                privs[i] = acm.privilegeFromName(privilegeNames[i]);
            }
            
            return removePermissions(session, path, principal, privs);
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to add permission(s) to node " + path + ": " + privilegeNames, e);
        }
    }
    
    public static boolean removePermissions(Session session, String path, Principal principal, Privilege... removes) {
        try {
            AccessControlManager acm = session.getAccessControlManager();
            AccessControlPolicy[] aclArray = acm.getPolicies(path);
            
            if (aclArray.length > 0) {
                AccessControlList acl = (AccessControlList) aclArray[0];
                boolean removed = false;
                
                for (AccessControlEntry entry : acl.getAccessControlEntries()) {
                    if (entry.getPrincipal().getName().equals(principal.getName())) {
                        Privilege[] newPrivs = Arrays.stream(entry.getPrivileges())
                                        .filter(p -> Arrays.stream(removes).anyMatch(r -> ! r.equals(p)))
                                        .toArray(Privilege[]::new);
                        
                        if (entry.getPrivileges().length != newPrivs.length) {
                            if (newPrivs.length == 0) {
                                acl.removeAccessControlEntry(entry);
                            } else {
                                acl.addAccessControlEntry(entry.getPrincipal(), newPrivs);
                            }
                            removed = true;
                        }
                    }
                }
                
                acm.setPolicy(path, acl);
                
                return removed;
            } else {
                return false;
            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to add permission(s) to node " + path + ": " + removes, e);
        }
        
    }
    
    public static boolean removeHierarchyPermissions(Node node, Principal principal, Node toNode, String... privilegeNames) {
        try {
            Node endNode = toNode;
            Node current = node;
            boolean removed = false;
            
            do {
                removed |= removePermissions(node.getSession(), node.getPath(), principal, privilegeNames);
                current = current.getParent();
            } while (! current.equals(endNode) && ! current.equals(toNode.getSession().getRootNode()));
            
            return removed;
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to add permission(s) to hierarch from node " + node + " up to " + toNode, e);
        }
    }

    public static boolean clearPermissions(Session session, String path) {
        try {
            AccessControlManager acm = session.getAccessControlManager();
            AccessControlPolicy[] acls = acm.getPolicies(path);
            
            if (acls.length > 0) {
                for (AccessControlPolicy policy : acm.getPolicies(path)) {
                    acm.removePolicy(path, policy);
                }
                
                return true;
            } else {
                return false;
            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Failed to remove all permission(s) from node " + path, e);
        }
        
    }

    public static void addRecursivePermissions(Node node, Principal modeShapeAdminPrincipal, String... privilegeNames) {
        // TODO Auto-generated method stub
        
    }
}
