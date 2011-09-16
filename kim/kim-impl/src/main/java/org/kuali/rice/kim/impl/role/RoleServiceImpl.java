package org.kuali.rice.kim.impl.role;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.kuali.rice.core.api.util.jaxb.MapStringStringAdapter;
import org.kuali.rice.core.api.util.jaxb.SqlDateAdapter;
import org.kuali.rice.kim.api.KimConstants;
import org.kuali.rice.kim.api.common.delegate.DelegateMember;
import org.kuali.rice.kim.api.common.delegate.DelegateType;
import org.kuali.rice.kim.api.group.GroupMember;
import org.kuali.rice.kim.api.role.Role;
import org.kuali.rice.kim.api.role.RoleMember;
import org.kuali.rice.kim.api.role.RoleMembership;
import org.kuali.rice.kim.api.role.RoleResponsibility;
import org.kuali.rice.kim.api.role.RoleResponsibilityAction;
import org.kuali.rice.kim.api.role.RoleService;
import org.kuali.rice.kim.api.services.KimApiServiceLocator;
import org.kuali.rice.kim.api.type.KimType;
import org.kuali.rice.kim.framework.common.delegate.DelegationTypeService;
import org.kuali.rice.kim.framework.role.RoleTypeService;
import org.kuali.rice.kim.framework.services.KimFrameworkServiceLocator;
import org.kuali.rice.kim.framework.type.KimTypeService;
import org.kuali.rice.kim.impl.common.delegate.DelegateBo;
import org.kuali.rice.kim.impl.common.delegate.DelegateMemberAttributeDataBo;
import org.kuali.rice.kim.impl.common.delegate.DelegateMemberBo;
import org.kuali.rice.kim.impl.group.GroupMemberBo;
import org.kuali.rice.krad.service.KRADServiceLocator;
import org.kuali.rice.krad.service.SequenceAccessorService;

import javax.jws.WebParam;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RoleServiceImpl extends RoleServiceBase implements RoleService {
    private static final Logger LOG = Logger.getLogger(RoleServiceImpl.class);

    private static final Map<String, RoleDaoAction> memberTypeToRoleDaoActionMap = populateMemberTypeToRoleDaoActionMap();

    private static Map<String, RoleDaoAction> populateMemberTypeToRoleDaoActionMap() {
        Map<String, RoleDaoAction> map = new HashMap<String, RoleDaoAction>();
        map.put(Role.GROUP_MEMBER_TYPE, RoleDaoAction.ROLE_GROUPS_FOR_GROUP_IDS_AND_ROLE_IDS);
        map.put(Role.PRINCIPAL_MEMBER_TYPE, RoleDaoAction.ROLE_PRINCIPALS_FOR_PRINCIPAL_ID_AND_ROLE_IDS);
        map.put(Role.ROLE_MEMBER_TYPE, RoleDaoAction.ROLE_MEMBERSHIPS_FOR_ROLE_IDS_AS_MEMBERS);     
        return Collections.unmodifiableMap(map);
    }
    
    @Override
    public Role getRole(@WebParam(name = "roleId") String roleId) {
        RoleBo roleBo = getRoleBo(roleId);
        if (roleBo == null) {
            return null;
        }
        return RoleBo.to(roleBo);
    }

    protected Map<String, RoleBo> getRoleBoMap(Collection<String> roleIds) {
        Map<String, RoleBo> result;
        // check for a non-null result in the cache, return it if found
        if (roleIds.size() == 1) {
            String roleId = roleIds.iterator().next();
            RoleBo bo = getRoleBo(roleId);
            if (bo.isActive()) {
                result = Collections.singletonMap(roleId, bo);
            } else {
                result = Collections.emptyMap();
            }
        } else {
            result = new HashMap<String, RoleBo>(roleIds.size());
            for (String roleId : roleIds) {
                RoleBo bo = getRoleBo(roleId);
                if (bo.isActive()) {
                    result.put(roleId, bo);
                }
            }
        }
        return result;
    }

    @Override
    public List<Role> getRoles(@WebParam(name = "roleIds") List<String> roleIds) {
        Collection<RoleBo> roleBos = getRoleBoMap(roleIds).values();
        List<Role> roles = new ArrayList<Role>(roleBos.size());
        for (RoleBo bo : roleBos) {
            roles.add(RoleBo.to(bo));
        }
        return roles;
    }

    @Override
    public Role getRoleByName(@WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName) {
        RoleBo roleBo = getRoleBoByName(namespaceCode, roleName);
        if (roleBo != null) {
            return RoleBo.to(roleBo);
        }
        return null;
    }

    @Override
    public String getRoleIdByName(@WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName) {
        Role role = getRoleByName(namespaceCode, roleName);
        if (role != null) {
            return role.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean isRoleActive(@WebParam(name = "roleId") String roleId) {
        RoleBo roleBo = getRoleBo(roleId);
        return roleBo != null && roleBo.isActive();
    }

    @Override
    public List<Map<String, String>> getRoleQualifiersForPrincipal(@WebParam(name = "principalId") String principalId,
                                                            @WebParam(name = "roleIds") List<String> roleIds,
                                                            @WebParam(name = "qualification") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualification) {

        List<Map<String, String>> results = new ArrayList<Map<String, String>>();

        List<RoleMemberBo> roleMemberBoList = getStoredRoleMembersUsingExactMatchOnQualification(principalId, null, roleIds, qualification);

        Map<String, List<RoleMembership>> roleIdToMembershipMap = new HashMap<String, List<RoleMembership>>();
        for (RoleMemberBo roleMemberBo : roleMemberBoList) {
            // gather up the qualifier sets and the service they go with
            if (roleMemberBo.getMemberTypeCode().equals(Role.PRINCIPAL_MEMBER_TYPE)) {
                RoleTypeService roleTypeService = getRoleTypeService(roleMemberBo.getRoleId());
                if (roleTypeService != null) {
                    List<RoleMembership> las = roleIdToMembershipMap.get(roleMemberBo.getRoleId());
                    if (las == null) {
                        las = new ArrayList<RoleMembership>();
                        roleIdToMembershipMap.put(roleMemberBo.getRoleId(), las);
                    }
                    RoleMembership mi = RoleMembership.Builder.create(
                            roleMemberBo.getRoleId(),
                            roleMemberBo.getRoleMemberId(),
                            roleMemberBo.getMemberId(),
                            roleMemberBo.getMemberTypeCode(),
                            roleMemberBo.getAttributes()).build();

                    las.add(mi);
                } else {
                    results.add(roleMemberBo.getAttributes());
                }
            }
        }
        for (String roleId : roleIdToMembershipMap.keySet()) {
            RoleTypeService roleTypeService = getRoleTypeService(roleId);
            //it is possible that the the roleTypeService is coming from a remote application
            // and therefore it can't be guaranteed that it is up and working, so using a try/catch to catch this possibility.
            try {
                List<RoleMembership> matchingMembers = roleTypeService.getMatchingRoleMemberships(qualification,
                        roleIdToMembershipMap.get(roleId));
                for (RoleMembership rmi : matchingMembers) {
                    results.add(rmi.getQualifier());
                }
            } catch (Exception ex) {
                LOG.warn("Not able to retrieve RoleTypeService from remote system for role Id: " + roleId, ex);
            }
        }
        return results;
    }

    @Override
    public List<Map<String, String>> getRoleQualifiersForPrincipal(@WebParam(name = "principalId") String principalId, @WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName, @WebParam(name = "qualification") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualification) {
        String roleId = getRoleIdByName(namespaceCode, roleName);
        if (roleId == null) {
            return new ArrayList<Map<String, String>>(0);
        }
        return getNestedRoleQualifiersForPrincipal(principalId, Collections.singletonList(roleId), qualification);
    }

    @Override
    public List<Map<String, String>> getNestedRoleQualifiersForPrincipal(@WebParam(name = "principalId") String principalId, @WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName, @WebParam(name = "qualification") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualification) {
        String roleId = getRoleIdByName(namespaceCode, roleName);
        if (roleId == null) {
            return new ArrayList<Map<String, String>>(0);
        }
        return getNestedRoleQualifiersForPrincipal(principalId, Collections.singletonList(roleId), qualification);
    }

    @Override
    public List<Map<String, String>> getNestedRoleQualifiersForPrincipal(@WebParam(name = "principalId") String principalId, @WebParam(name = "roleIds") List<String> roleIds, @WebParam(name = "qualification") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualification) {
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();

        Map<String, RoleBo> roleBosById = getRoleBoMap(roleIds);

        // get the person's groups
        List<String> groupIds = getGroupService().getGroupIdsForPrincipal(principalId);
        List<RoleMemberBo> roleMemberBos = getStoredRoleMembersUsingExactMatchOnQualification(principalId, groupIds, roleIds, qualification);

        Map<String, List<RoleMembership>> roleIdToMembershipMap = new HashMap<String, List<RoleMembership>>();
        for (RoleMemberBo roleMemberBo : roleMemberBos) {
            RoleTypeService roleTypeService = getRoleTypeService(roleMemberBo.getRoleId());
            // gather up the qualifier sets and the service they go with
            if (roleMemberBo.getMemberTypeCode().equals(Role.PRINCIPAL_MEMBER_TYPE)
                    || roleMemberBo.getMemberTypeCode().equals(Role.GROUP_MEMBER_TYPE)) {
                if (roleTypeService != null) {
                    List<RoleMembership> las = roleIdToMembershipMap.get(roleMemberBo.getRoleId());
                    if (las == null) {
                        las = new ArrayList<RoleMembership>();
                        roleIdToMembershipMap.put(roleMemberBo.getRoleId(), las);
                    }
                    RoleMembership mi = RoleMembership.Builder.create(
                            roleMemberBo.getRoleId(),
                            roleMemberBo.getRoleMemberId(),
                            roleMemberBo.getMemberId(),
                            roleMemberBo.getMemberTypeCode(),
                            roleMemberBo.getAttributes()).build();

                    las.add(mi);
                } else {
                    results.add(roleMemberBo.getAttributes());
                }
            } else if (roleMemberBo.getMemberTypeCode().equals(Role.ROLE_MEMBER_TYPE)) {
                // find out if the user has the role
                // need to convert qualification using this role's service
                Map<String, String> nestedQualification = qualification;
                if (roleTypeService != null) {
                    RoleBo roleBo = roleBosById.get(roleMemberBo.getRoleId());
                    // pulling from here as the nested roleBo is not necessarily (and likely is not)
                    // in the roleBosById Map created earlier
                    RoleBo nestedRole = getRoleBo(roleMemberBo.getMemberId());
                    //it is possible that the the roleTypeService is coming from a remote application
                    // and therefore it can't be guaranteed that it is up and working, so using a try/catch to catch this possibility.
                    try {
                        nestedQualification = roleTypeService.convertQualificationForMemberRoles(roleBo.getNamespaceCode(), roleBo.getName(), nestedRole.getNamespaceCode(), nestedRole.getName(), qualification);
                    } catch (Exception ex) {
                        LOG.warn("Not able to retrieve RoleTypeService from remote system for roleBo Id: " + roleBo.getId(), ex);
                    }
                }
                List<String> nestedRoleId = new ArrayList<String>(1);
                nestedRoleId.add(roleMemberBo.getMemberId());
                // if the user has the given role, add the qualifier the *nested role* has with the
                // originally queries role
                if (principalHasRole(principalId, nestedRoleId, nestedQualification, false)) {
                    results.add(roleMemberBo.getAttributes());
                }
            }
        }
        for (String roleId : roleIdToMembershipMap.keySet()) {
            RoleTypeService roleTypeService = getRoleTypeService(roleId);
            //it is possible that the the roleTypeService is coming from a remote application
            // and therefore it can't be guaranteed that it is up and working, so using a try/catch to catch this possibility.
            try {
                List<RoleMembership> matchingMembers = roleTypeService.getMatchingRoleMemberships(qualification,
                        roleIdToMembershipMap.get(roleId));
                for (RoleMembership roleMembership : matchingMembers) {
                    results.add(roleMembership.getQualifier());
                }
            } catch (Exception ex) {
                LOG.warn("Not able to retrieve RoleTypeService from remote system for role Id: " + roleId, ex);
            }
        }
        return results;
    }

    @Override
    public List<RoleMembership> getRoleMembers(@WebParam(name = "roleIds") List<String> roleIds, @WebParam(name = "qualification") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualification) {
        Set<String> foundRoleTypeMembers = new HashSet<String>();
        return getRoleMembers(roleIds, qualification, true, foundRoleTypeMembers);
    }

    @Override
    public Collection<String> getRoleMemberPrincipalIds(@WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName, @WebParam(name = "qualification") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualification) {
        Set<String> principalIds = new HashSet<String>();
        Set<String> foundRoleTypeMembers = new HashSet<String>();
        List<String> roleIds = Collections.singletonList(getRoleIdByName(namespaceCode, roleName));
        for (RoleMembership roleMembership : getRoleMembers(roleIds, qualification, false, foundRoleTypeMembers)) {
            if (Role.GROUP_MEMBER_TYPE.equals(roleMembership.getMemberTypeCode())) {
                principalIds.addAll(getGroupService().getMemberPrincipalIds(roleMembership.getMemberId()));
            } else {
                principalIds.add(roleMembership.getMemberId());
            }
        }
        return principalIds;
    }

    @Override
    public boolean principalHasRole(@WebParam(name = "principalId") String principalId, @WebParam(name = "roleIds") List<String> roleIds, @WebParam(name = "qualification") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualification) {
        return principalHasRole(principalId, roleIds, qualification, true);
    }

    @Override
    public List<String> getPrincipalIdSubListWithRole(@WebParam(name = "principalIds") List<String> principalIds, @WebParam(name = "roleNamespaceCode") String roleNamespaceCode, @WebParam(name = "roleName") String roleName, @WebParam(name = "qualification") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualification) {
        List<String> subList = new ArrayList<String>();
        RoleBo role = getRoleBoByName(roleNamespaceCode, roleName);
        for (String principalId : principalIds) {
            if (principalHasRole(principalId, Collections.singletonList(role.getId()), qualification)) {
                subList.add(principalId);
            }
        }
        return subList;
    }

    @Override
    public List<Role> getRolesSearchResults(@XmlJavaTypeAdapter(value = MapStringStringAdapter.class) @WebParam(name = "fieldValues") Map<String, String> fieldValues) {
        List<RoleBo> roleBoList = getRoleDao().getRoles(fieldValues);
        List<Role> roles = new ArrayList<Role>();
        for (RoleBo roleBo : roleBoList) {
            roles.add(RoleBo.to(roleBo));
        }
        return roles;
    }

    @Override
    public void principalInactivated(@WebParam(name = "principalId") String principalId) {
        long oneDayInMillis = TimeUnit.DAYS.toMillis(1);
        Timestamp yesterday = new Timestamp(System.currentTimeMillis() - oneDayInMillis);

        inactivatePrincipalRoleMemberships(principalId, yesterday);
        inactivatePrincipalGroupMemberships(principalId, yesterday);
        inactivatePrincipalDelegations(principalId, yesterday);
        inactivateApplicationRoleMemberships(principalId, yesterday);
    }

    @Override
    public void roleInactivated(@WebParam(name = "roleId") String roleId) {
        long oneDayInMillis = TimeUnit.DAYS.toMillis(1);
        Timestamp yesterday = new Timestamp(System.currentTimeMillis() - oneDayInMillis);

        List<String> roleIds = new ArrayList<String>();
        roleIds.add(roleId);
        inactivateRoleMemberships(roleIds, yesterday);
        inactivateRoleDelegations(roleIds, yesterday);
        inactivateMembershipsForRoleAsMember(roleIds, yesterday);
    }

    @Override
    public void groupInactivated(@WebParam(name = "groupId") String groupId) {
        long oneDayInMillis = TimeUnit.DAYS.toMillis(1);
        Timestamp yesterday = new Timestamp(System.currentTimeMillis() - oneDayInMillis);

        List<String> groupIds = new ArrayList<String>();
        groupIds.add(groupId);
        inactivatePrincipalGroupMemberships(groupIds, yesterday);
        inactivateGroupRoleMemberships(groupIds, yesterday);
    }

    @Override
    public List<RoleMembership> getFirstLevelRoleMembers(@WebParam(name = "roleIds") List<String> roleIds) {
        List<RoleMemberBo> roleMemberBoList = getStoredRoleMembersForRoleIds(roleIds, null, null);
        List<RoleMembership> roleMemberships = new ArrayList<RoleMembership>();
        for (RoleMemberBo roleMemberBo : roleMemberBoList) {
            RoleMembership roleMembeship = RoleMembership.Builder.create(
                    roleMemberBo.getRoleId(),
                    roleMemberBo.getRoleMemberId(),
                    roleMemberBo.getMemberId(),
                    roleMemberBo.getMemberTypeCode(),
                    roleMemberBo.getAttributes()).build();
            roleMemberships.add(roleMembeship);
        }
        return roleMemberships;
    }

    @Override
    public List<RoleMembership> findRoleMemberships(@XmlJavaTypeAdapter(value = MapStringStringAdapter.class) @WebParam(name = "fieldValues") Map<String, String> fieldValues) {
        return getRoleDao().getRoleMembers(fieldValues);
    }

    @Override
    public List<String> getMemberParentRoleIds(String memberType, String memberId) {
        return super.getMemberParentRoleIds(memberType, memberId);
    }

    @Override
    public List<RoleMember> findRoleMembers(@XmlJavaTypeAdapter(value = MapStringStringAdapter.class) @WebParam(name = "fieldValues") Map<String, String> fieldValues) {
        return super.findRoleMembers(fieldValues);
    }

    @Override
    public List<DelegateMember> findDelegateMembers(@XmlJavaTypeAdapter(value = MapStringStringAdapter.class) @WebParam(name = "fieldValues") Map<String, String> fieldValues) {
        return super.findDelegateMembers(fieldValues);
    }

    @Override
    public List<DelegateMember> getDelegationMembersByDelegationId(@WebParam(name = "delegationId") String delegationId) {
        DelegateBo delegateBo = getKimDelegationImpl(delegationId);
        if (delegateBo == null) {return null;}

        return getDelegateMembersForDelegation(delegateBo);
    }

    @Override
    public DelegateMember getDelegationMemberByDelegationAndMemberId(@WebParam(name = "delegationId") String delegationId, @WebParam(name = "memberId") String memberId) {
        DelegateBo delegateBo = getKimDelegationImpl(delegationId);
        DelegateMemberBo delegationMember = getKimDelegationMemberImplByDelegationAndId(delegationId, memberId);

        return getDelegateCompleteInfo(delegateBo, delegationMember);
    }

    @Override
    public DelegateMember getDelegationMemberById(@WebParam(name = "assignedToId") String delegationMemberId) {

        DelegateMemberBo delegateMemberBo = getDelegateMemberBo(delegationMemberId);
        if (delegateMemberBo == null) {
            return null;
        }

        DelegateBo delegateBo = getKimDelegationImpl(delegateMemberBo.getDelegationId());

        return getDelegateCompleteInfo(delegateBo, delegateMemberBo);
    }

    @Override
    public List<RoleResponsibility> getRoleResponsibilities(@WebParam(name = "roleId") String roleId) {
        Map<String, String> criteria = new HashMap<String, String>(1);
        criteria.put(KimConstants.PrimaryKeyConstants.SUB_ROLE_ID, roleId);
        List<RoleResponsibilityBo> roleResponsibilityBos = (List<RoleResponsibilityBo>)
                getBusinessObjectService().findMatching(RoleResponsibilityBo.class, criteria);
        List<RoleResponsibility> roleResponsibilities = new ArrayList<RoleResponsibility>();

        for (RoleResponsibilityBo roleResponsibilityImpl : roleResponsibilityBos) {
            roleResponsibilities.add(RoleResponsibilityBo.to(roleResponsibilityImpl));
        }
        return roleResponsibilities;
    }

    @Override
    public List<RoleResponsibilityAction> getRoleMemberResponsibilityActions(@WebParam(name = "roleMemberId") String roleMemberId) {
        return super.getRoleMemberResponsibilityActions(roleMemberId);
    }

    @Override
    public DelegateType getDelegateTypeInfo(@WebParam(name = "roleId") String roleId, @WebParam(name = "delegationTypeCode") String delegationTypeCode) {
        DelegateBo delegateBo = getDelegationOfType(roleId, delegationTypeCode);
        return DelegateBo.to(delegateBo);
    }

    @Override
    public DelegateType getDelegateTypeInfoById(@WebParam(name = "delegationId") String delegationId) {
        if (delegationId == null) {
            return null;
        }
        DelegateBo delegateBo = getKimDelegationImpl(delegationId);
        return DelegateBo.to(delegateBo);
    }

    @Override
    public void applicationRoleMembershipChanged(@WebParam(name = "roleId") String roleId) {
        getResponsibilityInternalService().updateActionRequestsForRoleChange(roleId);
    }

    @Override
    public List<Role> lookupRoles(@WebParam(name = "searchCriteria") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> searchCriteria) {
        Collection<RoleBo> roleBoCollection = getBusinessObjectService().findMatching(RoleBo.class, searchCriteria);
        ArrayList<Role> roleList = new ArrayList<Role>();
        for (RoleBo roleBo : roleBoCollection) {
            roleList.add(RoleBo.to(roleBo));
        }
        return roleList;
    }

    @SuppressWarnings("unchecked")
    protected void inactivateApplicationRoleMemberships(String principalId, Timestamp yesterday) {

    }


    protected void inactivatePrincipalRoleMemberships(String principalId, Timestamp yesterday) {
        // go through all roles and post-date them
        List<RoleMemberBo> roleMembers = getStoredRolePrincipalsForPrincipalIdAndRoleIds(null, principalId, null);
        Set<String> roleIds = new HashSet<String>(roleMembers.size());
        for (RoleMemberBo roleMemberBo : roleMembers) {
            roleMemberBo.setActiveToDateValue(yesterday);
            roleIds.add(roleMemberBo.getRoleId()); // add to the set of IDs
        }
        getBusinessObjectService().save(roleMembers);
    }

    protected void inactivateGroupRoleMemberships(List<String> groupIds, Timestamp yesterday) {
        List<RoleMemberBo> roleMemberBosOfGroupType = getStoredRoleGroupsForGroupIdsAndRoleIds(null, groupIds, null);
        for (RoleMemberBo roleMemberbo : roleMemberBosOfGroupType) {
            roleMemberbo.setActiveToDateValue(yesterday);
        }
        getBusinessObjectService().save(roleMemberBosOfGroupType);
    }

    protected void inactivatePrincipalGroupMemberships(String principalId, Timestamp yesterday) {
        List<GroupMember> groupMembers = getRoleDao().getGroupPrincipalsForPrincipalIdAndGroupIds(null, principalId);
        List<GroupMemberBo> groupMemberBoList = new ArrayList<GroupMemberBo>(groupMembers.size());
        for (GroupMember gm : groupMembers) {
            GroupMember.Builder builder = GroupMember.Builder.create(gm);
            builder.setActiveToDate(new DateTime(yesterday.getTime()));
            groupMemberBoList.add(GroupMemberBo.from(builder.build()));
        }
        getBusinessObjectService().save(groupMemberBoList);
    }

    protected void inactivatePrincipalGroupMemberships(List<String> groupIds, Timestamp yesterday) {
        List<GroupMember> groupMembers = getRoleDao().getGroupMembers(groupIds);
        List<GroupMemberBo> groupMemberBoList = new ArrayList<GroupMemberBo>(groupMembers.size());
        for (GroupMember groupMember : groupMembers) {
            GroupMember.Builder builder = GroupMember.Builder.create(groupMember);
            builder.setActiveToDate(new DateTime(yesterday.getTime()));
            groupMemberBoList.add(GroupMemberBo.from(builder.build()));
        }
        getBusinessObjectService().save(groupMemberBoList);
    }

    protected void inactivatePrincipalDelegations(String principalId, Timestamp yesterday) {
        List<DelegateMemberBo> delegationMembers = getStoredDelegationPrincipalsForPrincipalIdAndDelegationIds(null, principalId);
        for (DelegateMemberBo delegateMemberBo : delegationMembers) {
            delegateMemberBo.setActiveToDateValue(yesterday);
        }
        getBusinessObjectService().save(delegationMembers);
    }


    protected List<RoleMembership> getRoleMembers(List<String> roleIds, Map<String, String> qualification, boolean followDelegations, Set<String> foundRoleTypeMembers) {
        List<RoleMembership> results = new ArrayList<RoleMembership>();
        Set<String> allRoleIds = new HashSet<String>();
        for (String roleId : roleIds) {
            if (isRoleActive(roleId)) {
                allRoleIds.add(roleId);
            }
        }
        // short-circuit if no roles match
        if (allRoleIds.isEmpty()) {
            return results;
        }
        Set<String> matchingRoleIds = new HashSet<String>(allRoleIds.size());
        // for efficiency, retrieve all roles and store in a map
        Map<String, RoleBo> roles = getRoleBoMap(allRoleIds);

        List<String> copyRoleIds = new ArrayList<String>(allRoleIds);
        List<RoleMemberBo> rms = new ArrayList<RoleMemberBo>();

        for (String roleId : allRoleIds) {
            RoleTypeService roleTypeService = getRoleTypeService(roleId);
            if (roleTypeService != null) {
                List<String> attributesForExactMatch = roleTypeService.getQualifiersForExactMatch();
                if (CollectionUtils.isNotEmpty(attributesForExactMatch)) {
                    copyRoleIds.remove(roleId);
                    rms.addAll(getStoredRoleMembersForRoleIds(Collections.singletonList(roleId), null, populateQualifiersForExactMatch(qualification, attributesForExactMatch)));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(copyRoleIds)) {
            rms.addAll(getStoredRoleMembersForRoleIds(copyRoleIds, null, null));
        }

        // build a map of role ID to membership information
        // this will be used for later qualification checks
        Map<String, List<RoleMembership>> roleIdToMembershipMap = new HashMap<String, List<RoleMembership>>();
        for (RoleMemberBo roleMemberBo : rms) {
            RoleMembership mi = RoleMembership.Builder.create(
                    roleMemberBo.getRoleId(),
                    roleMemberBo.getRoleMemberId(),
                    roleMemberBo.getMemberId(),
                    roleMemberBo.getMemberTypeCode(),
                    roleMemberBo.getAttributes()).build();

            // if the qualification check does not need to be made, just add the result
            if ((qualification == null || qualification.isEmpty()) || getRoleTypeService(roleMemberBo.getRoleId()) == null) {
                if (roleMemberBo.getMemberTypeCode().equals(Role.ROLE_MEMBER_TYPE)) {
                    // if a role member type, do a non-recursive role member check
                    // to obtain the group and principal members of that role
                    // given the qualification
                    Map<String, String> nestedRoleQualification = qualification;
                    if (getRoleTypeService(roleMemberBo.getRoleId()) != null) {
                        // get the member role object
                        RoleBo memberRole = getRoleBo(mi.getMemberId());
                        nestedRoleQualification = getRoleTypeService(roleMemberBo.getRoleId())
                                .convertQualificationForMemberRoles(
                                        roles.get(roleMemberBo.getRoleId()).getNamespaceCode(),
                                        roles.get(roleMemberBo.getRoleId()).getName(),
                                        memberRole.getNamespaceCode(),
                                        memberRole.getName(),
                                        qualification);
                    }
                    if (isRoleActive(roleMemberBo.getRoleId())) {
                        Collection<RoleMembership> nestedRoleMembers = getNestedRoleMembers(nestedRoleQualification, mi, foundRoleTypeMembers);
                        if (!nestedRoleMembers.isEmpty()) {
                            results.addAll(nestedRoleMembers);
                            matchingRoleIds.add(roleMemberBo.getRoleId());
                        }
                    }
                } else { // not a role member type
                    results.add(mi);
                    matchingRoleIds.add(roleMemberBo.getRoleId());
                }
                matchingRoleIds.add(roleMemberBo.getRoleId());
            } else {
                List<RoleMembership> lrmi = roleIdToMembershipMap.get(mi.getRoleId());
                if (lrmi == null) {
                    lrmi = new ArrayList<RoleMembership>();
                    roleIdToMembershipMap.put(mi.getRoleId(), lrmi);
                }
                lrmi.add(mi);
            }
        }
        // if there is anything in the role to membership map, we need to check the role type services
        // for those entries
        if (!roleIdToMembershipMap.isEmpty()) {
            // for each role, send in all the qualifiers for that role to the type service
            // for evaluation, the service will return those which match
            for (String roleId : roleIdToMembershipMap.keySet()) {
                //it is possible that the the roleTypeService is coming from a remote application
                // and therefore it can't be guaranteed that it is up and working, so using a try/catch to catch this possibility.
                try {
                    RoleTypeService roleTypeService = getRoleTypeService(roleId);
                    List<RoleMembership> matchingMembers = roleTypeService.getMatchingRoleMemberships(qualification,
                            roleIdToMembershipMap.get(roleId));
                    // loop over the matching entries, adding them to the results
                    for (RoleMembership roleMemberships : matchingMembers) {
                        if (roleMemberships.getMemberTypeCode().equals(Role.ROLE_MEMBER_TYPE)) {
                            // if a role member type, do a non-recursive role member check
                            // to obtain the group and principal members of that role
                            // given the qualification
                            // get the member role object
                            RoleBo memberRole = getRoleBo(roleMemberships.getMemberId());
                            if (memberRole.isActive()) {
                                Map<String, String> nestedRoleQualification = roleTypeService.convertQualificationForMemberRoles(
                                        roles.get(roleMemberships.getRoleId()).getNamespaceCode(),
                                        roles.get(roleMemberships.getRoleId()).getName(),
                                        memberRole.getNamespaceCode(),
                                        memberRole.getName(),
                                        qualification);
                                Collection<RoleMembership> nestedRoleMembers = getNestedRoleMembers(nestedRoleQualification, roleMemberships, foundRoleTypeMembers);
                                if (!nestedRoleMembers.isEmpty()) {
                                    results.addAll(nestedRoleMembers);
                                    matchingRoleIds.add(roleMemberships.getRoleId());
                                }
                            }
                        } else { // not a role member
                            results.add(roleMemberships);
                            matchingRoleIds.add(roleMemberships.getRoleId());
                        }
                    }
                } catch (Exception ex) {
                    LOG.warn("Not able to retrieve RoleTypeService from remote system for role Id: " + roleId, ex);
                }
            }
        }
        return results;
    }


    protected boolean principalHasRole(String principalId, List<String> roleIds, Map<String, String> qualification, boolean checkDelegations) {
        if (StringUtils.isBlank(principalId)) {
            return false;
        }
        Set<String> allRoleIds = new HashSet<String>();
        // remove inactive roles
        for (String roleId : roleIds) {
            if (isRoleActive(roleId)) {
                allRoleIds.add(roleId);
            }
        }
        // short-circuit if no roles match
        if (allRoleIds.isEmpty()) {
            return false;
        }
        // for efficiency, retrieve all roles and store in a map
        Map<String, RoleBo> roles = getRoleBoMap(allRoleIds);
        // get all roles to which the principal is assigned
        List<String> copyRoleIds = new ArrayList<String>(allRoleIds);
        List<RoleMemberBo> rps = new ArrayList<RoleMemberBo>();

        for (String roleId : allRoleIds) {
            RoleTypeService roleTypeService = getRoleTypeService(roleId);
            if (roleTypeService != null) {
                List<String> attributesForExactMatch = roleTypeService.getQualifiersForExactMatch();
                if (CollectionUtils.isNotEmpty(attributesForExactMatch)) {
                    copyRoleIds.remove(roleId);
                    rps.addAll(getStoredRolePrincipalsForPrincipalIdAndRoleIds(Collections.singletonList(roleId), principalId, populateQualifiersForExactMatch(qualification, attributesForExactMatch)));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(copyRoleIds)) {
            rps.addAll(getStoredRolePrincipalsForPrincipalIdAndRoleIds(copyRoleIds, principalId, null));
        }

        // if the qualification is null and the role list is not, then any role in the list will match
        // so since the role ID list is not blank, we can return true at this point
        if ((qualification == null || qualification.isEmpty()) && !rps.isEmpty()) {
            return true;
        }

        // check each membership to see if the principal matches

        // build a map of role ID to membership information
        // this will be used for later qualification checks
        Map<String, List<RoleMembership>> roleIdToMembershipMap = new HashMap<String, List<RoleMembership>>();
        if (getRoleIdToMembershipMap(roleIdToMembershipMap, rps)) {
            return true;
        }

        // perform the checks against the role type services
        for (String roleId : roleIdToMembershipMap.keySet()) {
            try {
                RoleTypeService roleTypeService = getRoleTypeService(roleId);
                if (!roleTypeService.getMatchingRoleMemberships(qualification, roleIdToMembershipMap.get(roleId)).isEmpty()) {
                    return true;
                }
            } catch (Exception ex) {
                LOG.warn("Unable to find role type service with id: " + roleId);
            }
        }

        // find the groups that the principal belongs to
        List<String> principalGroupIds = getGroupService().getGroupIdsForPrincipal(principalId);
        // find the role/group associations
        if (!principalGroupIds.isEmpty()) {
            List<RoleMemberBo> rgs = getStoredRoleGroupsUsingExactMatchOnQualification(principalGroupIds, allRoleIds, qualification);
            roleIdToMembershipMap.clear(); // clear the role/member map for further use
            if (getRoleIdToMembershipMap(roleIdToMembershipMap, rgs)) {
                return true;
            }

            // perform the checks against the role type services
            for (String roleId : roleIdToMembershipMap.keySet()) {
                try {
                    RoleTypeService roleTypeService = getRoleTypeService(roleId);
                    if (!roleTypeService.getMatchingRoleMemberships(qualification, roleIdToMembershipMap.get(roleId)).isEmpty()) {
                        return true;
                    }
                } catch (Exception ex) {
                    LOG.warn("Unable to find role type service with id: " + roleId);
                }
            }
        }

        // check member roles
        // first, check that the qualifiers on the role membership match
        // then, perform a principalHasRole on the embedded role
        List<RoleMemberBo> roleMemberBos = getStoredRoleMembersForRoleIds(roleIds, Role.ROLE_MEMBER_TYPE, null);
        for (RoleMemberBo roleMemberBo : roleMemberBos) {
            RoleTypeService roleTypeService = getRoleTypeService(roleMemberBo.getRoleId());
            if (roleTypeService != null) {
                //it is possible that the the roleTypeService is coming from a remote application
                // and therefore it can't be guaranteed that it is up and working, so using a try/catch to catch this possibility.
                try {
                    if (roleTypeService.doesRoleQualifierMatchQualification(qualification, roleMemberBo.getAttributes())) {
                        RoleBo memberRole = getRoleBo(roleMemberBo.getMemberId());
                        Map<String, String> nestedRoleQualification = roleTypeService.convertQualificationForMemberRoles(
                                roles.get(roleMemberBo.getRoleId()).getNamespaceCode(),
                                roles.get(roleMemberBo.getRoleId()).getName(),
                                memberRole.getNamespaceCode(),
                                memberRole.getName(),
                                qualification);
                        ArrayList<String> roleIdTempList = new ArrayList<String>(1);
                        roleIdTempList.add(roleMemberBo.getMemberId());
                        if (principalHasRole(principalId, roleIdTempList, nestedRoleQualification, true)) {
                            return true;
                        }
                    }
                } catch (Exception ex) {
                    LOG.warn("Not able to retrieve RoleTypeService from remote system for role Id: " + roleMemberBo.getRoleId(), ex);
                    //return false;
                }
            } else {
                // no qualifiers - role is always used - check membership
                ArrayList<String> roleIdTempList = new ArrayList<String>(1);
                roleIdTempList.add(roleMemberBo.getMemberId());
                // no role type service, so can't convert qualification - just pass as is
                if (principalHasRole(principalId, roleIdTempList, qualification, true)) {
                    return true;
                }
            }

        }


        // check for application roles and extract principals and groups from that - then check them against the
        // role type service passing in the qualification and principal - the qualifier comes from the
        // external system (application)

        // loop over the allRoleIds list
        for (String roleId : allRoleIds) {
            RoleBo role = roles.get(roleId);
            RoleTypeService roleTypeService = getRoleTypeService(roleId);
            // check if an application role
            //it is possible that the the roleTypeService is coming from a remote application
            // and therefore it can't be guaranteed that it is up and working, so using a try/catch to catch this possibility.
            try {
                if (isApplicationRoleType(role.getKimTypeId(), roleTypeService)) {
                    if (roleTypeService.hasApplicationRole(principalId, principalGroupIds, role.getNamespaceCode(), role.getName(), qualification)) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                LOG.warn("Not able to retrieve RoleTypeService from remote system for role Id: " + roleId, ex);
                //return false;
            }
        }

        // delegations
        if (checkDelegations) {
            if (matchesOnDelegation(allRoleIds, principalId, principalGroupIds, qualification)) {
                return true;
            }
        }

        // NOTE: this logic is a little different from the getRoleMembers method
        // If there is no primary (matching non-delegate), this method will still return true
        return false;
    }


    protected boolean isApplicationRoleType(String roleTypeId, RoleTypeService service) {
       final  Boolean result;
            if (service != null) {
                result = service.isApplicationRoleType();
            } else {
                result = Boolean.FALSE;
            }
        return result;
    }

    /**
     * Support method for principalHasRole.  Checks delegations on the passed in roles for the given principal and groups.  (It's assumed that the principal
     * belongs to the given groups.)
     * <p/>
     * Delegation checks are mostly the same as role checks except that the delegateBo itself is qualified against the original role (like a RolePrincipal
     * or RoleGroup.)  And then, the members of that delegateBo may have additional qualifiers which are not part of the original role qualifiers.
     * <p/>
     * For example:
     * <p/>
     * A role could be qualified by organization.  So, there is a person in the organization with primary authority for that org.  But, then they delegate authority
     * for that organization (not their authority - the delegateBo is attached to the org.)  So, in this case the delegateBo has a qualifier of the organization
     * when it is attached to the role.
     * <p/>
     * The principals then attached to that delegateBo (which is specific to the organization), may have additional qualifiers.
     * For Example: dollar amount range, effective dates, document types.
     * As a subsequent step, those qualifiers are checked against the qualification passed in from the client.
     */
    protected boolean matchesOnDelegation(Set<String> allRoleIds, String principalId, List<String> principalGroupIds, Map<String, String> qualification) {
        // get the list of delegations for the roles
        Map<String, DelegateBo> delegations = getStoredDelegationImplMapFromRoleIds(allRoleIds);
        // loop over the delegations - determine those which need to be inspected more directly
        for (DelegateBo delegation : delegations.values()) {
            // check if each one matches via the original role type service
            if (!delegation.isActive()) {
                continue;
            }
            RoleTypeService roleTypeService = getRoleTypeService(delegation.getRoleId());
            for (DelegateMemberBo delegateMemberBo : delegation.getMembers()) {
                if (!delegateMemberBo.isActive(new Timestamp(new Date().getTime()))) {
                    continue;
                }
                // check if this delegateBo record applies to the given person
                if (delegateMemberBo.getTypeCode().equals(Role.PRINCIPAL_MEMBER_TYPE)
                        && !delegateMemberBo.getMemberId().equals(principalId)) {
                    continue; // no match on principal
                }
                // or if a group
                if (delegateMemberBo.getTypeCode().equals(Role.GROUP_MEMBER_TYPE)
                        && !principalGroupIds.contains(delegateMemberBo.getMemberId())) {
                    continue; // no match on group
                }
                // or if a role
                if (delegateMemberBo.getTypeCode().equals(Role.ROLE_MEMBER_TYPE)
                        && !principalHasRole(principalId, Collections.singletonList(delegateMemberBo.getMemberId()), qualification, false)) {
                    continue; // no match on role
                }
                // OK, the member matches the current user, now check the qualifications

                // NOTE: this compare is slightly different than the member enumeration
                // since the requested qualifier is always being used rather than
                // the role qualifier for the member (which is not available)

                //it is possible that the the roleTypeService is coming from a remote application
                // and therefore it can't be guaranteed that it is up and working, so using a try/catch to catch this possibility.
                try {
                    //TODO: remove reference to Attributes here and use Attributes instead.
                    if (roleTypeService != null && !roleTypeService.doesRoleQualifierMatchQualification(qualification, delegateMemberBo.getQualifier())) {
                        continue; // no match - skip to next record
                    }
                } catch (Exception ex) {
                    LOG.warn("Unable to call doesRoleQualifierMatchQualification on role type service for role Id: " + delegation.getRoleId() + " / " + qualification + " / " + delegateMemberBo.getQualifier(), ex);
                    continue;
                }

                // role service matches this qualifier
                // now try the delegateBo service
                DelegationTypeService delegationTypeService = getDelegationTypeService(delegateMemberBo.getDelegationId());
                // QUESTION: does the qualifier map need to be merged with the main delegateBo qualification?
                if (delegationTypeService != null && !delegationTypeService.doesDelegationQualifierMatchQualification(qualification, delegateMemberBo.getQualifier())) {
                    continue; // no match - skip to next record
                }
                // check if a role member ID is present on the delegateBo record
                // if so, check that the original role member would match the given qualifiers
                if (StringUtils.isNotBlank(delegateMemberBo.getRoleMemberId())) {
                    RoleMemberBo rm = getRoleMemberBo(delegateMemberBo.getRoleMemberId());
                    if (rm != null) {
                        // check that the original role member's is active and that their
                        // qualifier would have matched this request's
                        // qualifications (that the original person would have the permission/responsibility
                        // for an action)
                        // this prevents a role-membership based delegateBo from surviving the inactivation/
                        // changing of the main person's role membership
                        if (!rm.isActive(new Timestamp(new Date().getTime()))) {
                            continue;
                        }
                        Map<String, String> roleQualifier = rm.getAttributes();
                        //it is possible that the the roleTypeService is coming from a remote application
                        // and therefore it can't be guaranteed that it is up and working, so using a try/catch to catch this possibility.
                        try {
                            if (roleTypeService != null && !roleTypeService.doesRoleQualifierMatchQualification(qualification, roleQualifier)) {
                                continue;
                            }
                        } catch (Exception ex) {
                            LOG.warn("Unable to call doesRoleQualifierMatchQualification on role type service for role Id: " + delegation.getRoleId() + " / " + qualification + " / " + roleQualifier, ex);
                            continue;
                        }
                    } else {
                        LOG.warn("Unknown role member ID cited in the delegateBo member table:");
                        LOG.warn("       assignedToId: " + delegateMemberBo.getDelegationMemberId() + " / roleMemberId: " + delegateMemberBo.getRoleMemberId());
                    }
                }
                // all tests passed, return true
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method used by principalHasRole to build the role ID -> list of members map.
     *
     * @return <b>true</b> if no further checks are needed because no role service is defined
     */
    protected boolean getRoleIdToMembershipMap(Map<String, List<RoleMembership>> roleIdToMembershipMap, List<RoleMemberBo> roleMembers) {
        for (RoleMemberBo roleMemberBo : roleMembers) {
            RoleMembership roleMembership = RoleMembership.Builder.create(
                    roleMemberBo.getRoleId(),
                    roleMemberBo.getRoleMemberId(),
                    roleMemberBo.getMemberId(),
                    roleMemberBo.getMemberTypeCode(),
                    roleMemberBo.getAttributes()).build();

            // if the role type service is null, assume that all qualifiers match
            if (getRoleTypeService(roleMemberBo.getRoleId()) == null) {
                return true;
            }
            List<RoleMembership> lrmi = roleIdToMembershipMap.get(roleMembership.getRoleId());
            if (lrmi == null) {
                lrmi = new ArrayList<RoleMembership>();
                roleIdToMembershipMap.put(roleMembership.getRoleId(), lrmi);
            }
            lrmi.add(roleMembership);
        }
        return false;
    }

    /**
     * Retrieves a KimDelegationImpl object by its ID. If the delegateBo already exists in the cache, this method will return the cached
     * version; otherwise, it will retrieve the uncached version from the database and then cache it before returning it.
     */
    protected DelegateBo getKimDelegationImpl(String delegationId) {
        if (StringUtils.isBlank(delegationId)) {
            return null;
        }

        return getBusinessObjectService().findByPrimaryKey(DelegateBo.class,
                Collections.singletonMap(KimConstants.PrimaryKeyConstants.DELEGATION_ID, delegationId));
    }

    protected DelegationTypeService getDelegationTypeService(String delegationId) {
        DelegationTypeService service = null;
        DelegateBo delegateBo = getKimDelegationImpl(delegationId);
        KimType delegationType = KimApiServiceLocator.getKimTypeInfoService().getKimType(delegateBo.getKimTypeId());
        if (delegationType != null) {
            KimTypeService tempService = KimFrameworkServiceLocator.getKimTypeService(delegationType);
            if (tempService != null && tempService instanceof DelegationTypeService) {
                service = (DelegationTypeService) tempService;
            } else {
                LOG.error("Service returned for type " + delegationType + "(" + delegationType.getName() + ") was not a DelegationTypeService.  Was a " + tempService.getClass());
            }
        } else { // delegateBo has no type - default to role type if possible
            RoleTypeService roleTypeService = getRoleTypeService(delegateBo.getRoleId());
            if (roleTypeService != null && roleTypeService instanceof DelegationTypeService) {
                service = (DelegationTypeService) roleTypeService;
            }
        }
        return service;
    }

    protected Collection<RoleMembership> getNestedRoleMembers(Map<String, String> qualification, RoleMembership rm, Set<String> foundRoleTypeMembers) {
        // If this role has already been traversed, skip it
        if (foundRoleTypeMembers.contains(rm.getMemberId())) {
            return new ArrayList<RoleMembership>();  // return an empty list
        }
        foundRoleTypeMembers.add(rm.getMemberId());

        ArrayList<String> roleIdList = new ArrayList<String>(1);
        roleIdList.add(rm.getMemberId());

        // get the list of members from the nested role - ignore delegations on those sub-roles
        Collection<RoleMembership> currentNestedRoleMembers = getRoleMembers(roleIdList, qualification, false, foundRoleTypeMembers);

        // add the roles whose members matched to the list for delegateBo checks later
        Collection<RoleMembership> returnRoleMembers = new ArrayList<RoleMembership>();
        for (RoleMembership roleMembership : currentNestedRoleMembers) {
            RoleMembership.Builder rmBuilder = RoleMembership.Builder.create(roleMembership);

            // use the member ID of the parent role (needed for responsibility joining)
            rmBuilder.setRoleMemberId(rm.getRoleMemberId());
            // store the role ID, so we know where this member actually came from
            rmBuilder.setRoleId(rm.getRoleId());
            rmBuilder.setEmbeddedRoleId(rm.getMemberId());
            returnRoleMembers.add(rmBuilder.build());
        }
        return returnRoleMembers;
    }

    /**
     * Retrieves a KimDelegationMemberImpl object by its ID and the ID of the delegation it belongs to. If the delegation member exists in the cache,
     * this method will return the cached one; otherwise, it will retrieve the uncached version from the database and then cache it before returning it.
     */
    protected DelegateMemberBo getKimDelegationMemberImplByDelegationAndId(String delegationId, String delegationMemberId) {
        if (StringUtils.isBlank(delegationId) || StringUtils.isBlank(delegationMemberId)) {
            return null;
        }

        Map<String, String> searchCriteria = new HashMap<String, String>();
        searchCriteria.put(KimConstants.PrimaryKeyConstants.DELEGATION_ID, delegationId);
        searchCriteria.put(KimConstants.PrimaryKeyConstants.DELEGATION_MEMBER_ID, delegationMemberId);
        List<DelegateMemberBo> memberList =
                (List<DelegateMemberBo>) getBusinessObjectService().findMatching(DelegateMemberBo.class, searchCriteria);
        if (memberList != null && !memberList.isEmpty()) {
            return memberList.get(0);
        }
        return null;
    }

    private List<RoleMemberBo> getStoredRoleMembersUsingExactMatchOnQualification(String principalId, List<String> groupIds, List<String> roleIds, Map<String, String> qualification) {
        List<String> copyRoleIds = new ArrayList<String>(roleIds);
        List<RoleMemberBo> roleMemberBoList = new ArrayList<RoleMemberBo>();

        for (String roleId : roleIds) {
            RoleTypeService roleTypeService = getRoleTypeService(roleId);
            if (roleTypeService != null) {
                List<String> attributesForExactMatch = roleTypeService.getQualifiersForExactMatch();
                if (CollectionUtils.isNotEmpty(attributesForExactMatch)) {
                    copyRoleIds.remove(roleId);
                    roleMemberBoList.addAll(getStoredRoleMembersForRoleIdsWithFilters(Collections.singletonList(roleId), principalId, groupIds, populateQualifiersForExactMatch(qualification, attributesForExactMatch)));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(copyRoleIds)) {
            roleMemberBoList.addAll(getStoredRoleMembersForRoleIdsWithFilters(copyRoleIds, principalId, groupIds, null));
        }
        return roleMemberBoList;
    }

    private List<RoleMemberBo> getStoredRoleGroupsUsingExactMatchOnQualification(List<String> groupIds, Set<String> roleIds, Map<String, String> qualification) {
        List<String> copyRoleIds = new ArrayList<String>(roleIds);
        List<RoleMemberBo> roleMemberBos = new ArrayList<RoleMemberBo>();

        for (String roleId : roleIds) {
            RoleTypeService roleTypeService = getRoleTypeService(roleId);
            if (roleTypeService != null) {
                List<String> attributesForExactMatch = roleTypeService.getQualifiersForExactMatch();
                if (CollectionUtils.isNotEmpty(attributesForExactMatch)) {
                    copyRoleIds.remove(roleId);
                    roleMemberBos.addAll(getStoredRoleGroupsForGroupIdsAndRoleIds(Collections.singletonList(roleId), groupIds, populateQualifiersForExactMatch(qualification, attributesForExactMatch)));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(copyRoleIds)) {
            roleMemberBos.addAll(getStoredRoleGroupsForGroupIdsAndRoleIds(copyRoleIds, groupIds, null));
        }
        return roleMemberBos;
    }

    private void inactivateRoleMemberships(List<String> roleIds, Timestamp yesterday) {
        List<RoleMemberBo> roleMemberBoList = getStoredRoleMembersForRoleIds(roleIds, null, null);
        for (RoleMemberBo roleMemberBo : roleMemberBoList) {
            roleMemberBo.setActiveToDateValue(yesterday);
        }
        getBusinessObjectService().save(roleMemberBoList);
    }

    private void inactivateRoleDelegations(List<String> roleIds, Timestamp yesterday) {
        List<DelegateBo> delegations = getStoredDelegationImplsForRoleIds(roleIds);
        for (DelegateBo delegation : delegations) {
            delegation.setActive(false);
            for (DelegateMemberBo delegationMember : delegation.getMembers()) {
                delegationMember.setActiveToDateValue(yesterday);
            }
        }
        getBusinessObjectService().save(delegations);
    }

    private void inactivateMembershipsForRoleAsMember(List<String> roleIds, Timestamp yesterday) {
        List<RoleMemberBo> roleMemberBoList = getStoredRoleMembershipsForRoleIdsAsMembers(roleIds, null);
        for (RoleMemberBo roleMemberBo : roleMemberBoList) {
            roleMemberBo.setActiveToDateValue(yesterday);
        }
        getBusinessObjectService().save(roleMemberBoList);
    }

    private List<DelegateMember> getDelegateMembersForDelegation(DelegateBo delegateBo) {
        if (delegateBo == null || delegateBo.getMembers() == null) {return null;}
        List<DelegateMember> delegateMembersReturnList = new ArrayList<DelegateMember>();
        for (DelegateMemberBo delegateMemberBo : delegateBo.getMembers()) {
            DelegateMember delegateMember = getDelegateCompleteInfo(delegateBo, delegateMemberBo);

            delegateMembersReturnList.add(DelegateMemberBo.to(delegateMemberBo));
        }
        return delegateMembersReturnList;
    }

    private DelegateMember getDelegateCompleteInfo(DelegateBo delegateBo, DelegateMemberBo delegateMemberBo) {
        if (delegateBo == null || delegateMemberBo == null) {return null;}

        DelegateMember.Builder delegateMemberBuilder = DelegateMember.Builder.create(delegateMemberBo);
        delegateMemberBuilder.setTypeCode(delegateBo.getDelegationTypeCode());
        return delegateMemberBuilder.build();
    }

    @Override
    public void assignPrincipalToRole(@WebParam(name = "principalId") String principalId, @WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName, @WebParam(name = "qualifier") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualifier) throws UnsupportedOperationException {
        // look up the role
        RoleBo role = getRoleBoByName(namespaceCode, roleName);
        role.refreshReferenceObject("members");

        // check that identical member does not already exist
    	if ( doAnyMemberRecordsMatchByExactQualifier(role, principalId, memberTypeToRoleDaoActionMap.get(Role.PRINCIPAL_MEMBER_TYPE), qualifier) || 
    			doAnyMemberRecordsMatch( role.getMembers(), principalId, Role.PRINCIPAL_MEMBER_TYPE, qualifier ) ) {
    		return;
    	}
        // create the new role member object
        RoleMemberBo newRoleMember = new RoleMemberBo();
        // get a new ID from the sequence
        SequenceAccessorService sas = getSequenceAccessorService();
        Long nextSeq = sas.getNextAvailableSequenceNumber(
                KimConstants.SequenceNames.KRIM_ROLE_MBR_ID_S, RoleMemberBo.class);
        newRoleMember.setRoleMemberId(nextSeq.toString());

        newRoleMember.setRoleId(role.getId());
        newRoleMember.setMemberId(principalId);
        newRoleMember.setMemberTypeCode(Role.PRINCIPAL_MEMBER_TYPE);

        // build role member attribute objects from the given Map<String, String>
        addMemberAttributeData(newRoleMember, qualifier, role.getKimTypeId());

        // add row to member table
        // When members are added to roles, clients must be notified.
        getResponsibilityInternalService().saveRoleMember(newRoleMember);
    }

    @Override
    public void assignGroupToRole(@WebParam(name = "groupId") String groupId, @WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName, @WebParam(name = "qualifier") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualifier) throws UnsupportedOperationException {
        // look up the role
        RoleBo role = getRoleBoByName(namespaceCode, roleName);
        // check that identical member does not already exist
    	if ( doAnyMemberRecordsMatchByExactQualifier(role, groupId, memberTypeToRoleDaoActionMap.get(Role.GROUP_MEMBER_TYPE), qualifier) || 
    			doAnyMemberRecordsMatch( role.getMembers(), groupId, Role.GROUP_MEMBER_TYPE, qualifier ) ) { 
    		return;
    	}
        // create the new role member object
        RoleMemberBo newRoleMember = new RoleMemberBo();
        // get a new ID from the sequence
        SequenceAccessorService sas = getSequenceAccessorService();
        Long nextSeq = sas.getNextAvailableSequenceNumber(
                KimConstants.SequenceNames.KRIM_ROLE_MBR_ID_S, RoleMemberBo.class);
        newRoleMember.setRoleMemberId(nextSeq.toString());

        newRoleMember.setRoleId(role.getId());
        newRoleMember.setMemberId(groupId);
        newRoleMember.setMemberTypeCode(Role.GROUP_MEMBER_TYPE);

        // build role member attribute objects from the given Map<String, String>
        addMemberAttributeData(newRoleMember, qualifier, role.getKimTypeId());

        // When members are added to roles, clients must be notified.
        getResponsibilityInternalService().saveRoleMember(newRoleMember);
    }

    @Override
    public void assignRoleToRole(@WebParam(name = "roleId") String roleId, @WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName, @WebParam(name = "qualifier") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualifier) throws UnsupportedOperationException {
        // look up the roleBo
        RoleBo roleBo = getRoleBoByName(namespaceCode, roleName);
        // check that identical member does not already exist
    	if ( doAnyMemberRecordsMatchByExactQualifier(roleBo, roleId, memberTypeToRoleDaoActionMap.get(Role.ROLE_MEMBER_TYPE), qualifier) || 
    			doAnyMemberRecordsMatch( roleBo.getMembers(), roleId, Role.ROLE_MEMBER_TYPE, qualifier ) ) {
    		return;
    	}
        // Check to make sure this doesn't create a circular membership
        if (!checkForCircularRoleMembership(roleId, roleBo)) {
            throw new IllegalArgumentException("Circular roleBo reference.");
        }
        // create the new roleBo member object
        RoleMemberBo newRoleMember = new RoleMemberBo();
        // get a new ID from the sequence
        SequenceAccessorService sas = getSequenceAccessorService();
        Long nextSeq = sas.getNextAvailableSequenceNumber(
                KimConstants.SequenceNames.KRIM_ROLE_MBR_ID_S, RoleMemberBo.class);
        newRoleMember.setRoleMemberId(nextSeq.toString());

        newRoleMember.setRoleId(roleBo.getId());
        newRoleMember.setMemberId(roleId);
        newRoleMember.setMemberTypeCode(Role.ROLE_MEMBER_TYPE);
        // build roleBo member attribute objects from the given Map<String, String>
        addMemberAttributeData(newRoleMember, qualifier, roleBo.getKimTypeId());

        // When members are added to roles, clients must be notified.
        getResponsibilityInternalService().saveRoleMember(newRoleMember);
    }

    @Override
    public RoleMember saveRoleMemberForRole(@WebParam(name = "roleMemberId") String roleMemberId, @WebParam(name = "memberId") String memberId, @WebParam(name = "memberTypeCode") String memberTypeCode, @WebParam(name = "roleId") String roleId, @WebParam(name = "qualifications") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualifications, @XmlJavaTypeAdapter(value = SqlDateAdapter.class) @WebParam(name = "activeFromDate") java.sql.Date activeFromDate, @XmlJavaTypeAdapter(value = SqlDateAdapter.class) @WebParam(name = "activeToDate") java.sql.Date activeToDate) throws UnsupportedOperationException {
        if(StringUtils.isEmpty(roleMemberId) && StringUtils.isEmpty(memberId) && StringUtils.isEmpty(roleId)){
    		throw new IllegalArgumentException("Either Role member ID or a combination of member ID and roleBo ID must be passed in.");
    	}
    	RoleMemberBo origRoleMemberBo;
    	RoleBo roleBo;
    	// create the new roleBo member object
    	RoleMemberBo newRoleMember = new RoleMemberBo();
    	if(StringUtils.isEmpty(roleMemberId)){
	    	// look up the roleBo
	    	roleBo = getRoleBo(roleId);
	    	// check that identical member does not already exist
	    	origRoleMemberBo = matchingMemberRecord( roleBo.getMembers(), memberId, memberTypeCode, qualifications );
    	} else{
    		origRoleMemberBo = getRoleMemberBo(roleMemberId);
    		roleBo = getRoleBo(origRoleMemberBo.getRoleId());
    	}

    	if(origRoleMemberBo !=null){
    		newRoleMember.setRoleMemberId(origRoleMemberBo.getRoleMemberId());
    		newRoleMember.setVersionNumber(origRoleMemberBo.getVersionNumber());
    	} else {
	    	// get a new ID from the sequence
	    	SequenceAccessorService sas = getSequenceAccessorService();
	    	Long nextSeq = sas.getNextAvailableSequenceNumber(
	    			KimConstants.SequenceNames.KRIM_ROLE_MBR_ID_S, RoleMemberBo.class);
	    	newRoleMember.setRoleMemberId( nextSeq.toString() );
    	}
    	newRoleMember.setRoleId(roleBo.getId());
    	newRoleMember.setMemberId( memberId );
    	newRoleMember.setMemberTypeCode( memberTypeCode );
		if (activeFromDate != null) {
			newRoleMember.setActiveFromDateValue(new java.sql.Timestamp(activeFromDate.getTime()));
		}
		if (activeToDate != null) {
			newRoleMember.setActiveToDateValue(new java.sql.Timestamp(activeToDate.getTime()));
		}
    	// build roleBo member attribute objects from the given Map<String, String>
    	addMemberAttributeData( newRoleMember, qualifications, roleBo.getKimTypeId() );

    	// When members are added to roles, clients must be notified.
    	getResponsibilityInternalService().saveRoleMember(newRoleMember);
    	deleteNullMemberAttributeData(newRoleMember.getAttributeDetails());
    	return findRoleMember(newRoleMember.getRoleMemberId());
    }

    @Override
    public void saveRoleRspActions(@WebParam(name = "roleResponsibilityActionId") String roleResponsibilityActionId, @WebParam(name = "roleId") String roleId, @WebParam(name = "roleResponsibilityId") String roleResponsibilityId, @WebParam(name = "roleMemberId") String roleMemberId, @WebParam(name = "actionTypeCode") String actionTypeCode, @WebParam(name = "actionPolicyCode") String actionPolicyCode, @WebParam(name = "priorityNumber") Integer priorityNumber, @WebParam(name = "forceAction") Boolean forceAction) {
        RoleResponsibilityActionBo newRoleRspAction = new RoleResponsibilityActionBo();
		newRoleRspAction.setActionPolicyCode(actionPolicyCode);
		newRoleRspAction.setActionTypeCode(actionTypeCode);
		newRoleRspAction.setPriorityNumber(priorityNumber);
		newRoleRspAction.setForceAction(forceAction);
		newRoleRspAction.setRoleMemberId(roleMemberId);
		newRoleRspAction.setRoleResponsibilityId(roleResponsibilityId);
		if(StringUtils.isEmpty(roleResponsibilityActionId)){
			//If there is an existing one
			Map<String, String> criteria = new HashMap<String, String>(1);
			criteria.put(KimConstants.PrimaryKeyConstants.ROLE_RESPONSIBILITY_ID, roleResponsibilityId);
			criteria.put(KimConstants.PrimaryKeyConstants.ROLE_MEMBER_ID, roleMemberId);
			List<RoleResponsibilityActionBo> roleResponsibilityActionImpls = (List<RoleResponsibilityActionBo>)
				getBusinessObjectService().findMatching(RoleResponsibilityActionBo.class, criteria);
			if(roleResponsibilityActionImpls!=null && roleResponsibilityActionImpls.size()>0){
				newRoleRspAction.setId(roleResponsibilityActionImpls.get(0).getId());
				newRoleRspAction.setVersionNumber(roleResponsibilityActionImpls.get(0).getVersionNumber());
			} else{
	//			 get a new ID from the sequence
		    	SequenceAccessorService sas = getSequenceAccessorService();
		    	Long nextSeq = sas.getNextAvailableSequenceNumber(
		    			KimConstants.SequenceNames.KRIM_ROLE_RSP_ACTN_ID_S, RoleResponsibilityActionBo.class);
		    	newRoleRspAction.setId(nextSeq.toString());
			}
		} else{
			Map<String, String> criteria = new HashMap<String, String>(1);
			criteria.put(KimConstants.PrimaryKeyConstants.ROLE_RESPONSIBILITY_ACTION_ID, roleResponsibilityActionId);
			List<RoleResponsibilityActionBo> roleResponsibilityActionImpls = (List<RoleResponsibilityActionBo>)
				getBusinessObjectService().findMatching(RoleResponsibilityActionBo.class, criteria);
			if(CollectionUtils.isNotEmpty(roleResponsibilityActionImpls) && roleResponsibilityActionImpls.size()>0){
				newRoleRspAction.setId(roleResponsibilityActionImpls.get(0).getId());
				newRoleRspAction.setVersionNumber(roleResponsibilityActionImpls.get(0).getVersionNumber());
			}
		}
		getBusinessObjectService().save(newRoleRspAction);
    }

    @Override
    public void saveDelegationMemberForRole(@WebParam(name = "assignedToId") String delegationMemberId, @WebParam(name = "roleMemberId") String roleMemberId, @WebParam(name = "memberId") String memberId, @WebParam(name = "memberTypeCode") String memberTypeCode, @WebParam(name = "delegationTypeCode") String delegationTypeCode, @WebParam(name = "roleId") String roleId, @WebParam(name = "qualifications") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualifications, @XmlJavaTypeAdapter(value = SqlDateAdapter.class) @WebParam(name = "activeFromDate") java.sql.Date activeFromDate, @XmlJavaTypeAdapter(value = SqlDateAdapter.class) @WebParam(name = "activeToDate") java.sql.Date activeToDate) throws UnsupportedOperationException {
        if(StringUtils.isEmpty(delegationMemberId) && StringUtils.isEmpty(memberId) && StringUtils.isEmpty(roleId)){
    		throw new IllegalArgumentException("Either Delegation member ID or a combination of member ID and role ID must be passed in.");
    	}
    	// look up the role
    	RoleBo role = getRoleBo(roleId);
    	DelegateBo delegation = getDelegationOfType(role.getId(), delegationTypeCode);
    	// create the new role member object
    	DelegateMemberBo newDelegationMember = new DelegateMemberBo();

    	DelegateMemberBo origDelegationMember;
    	if(StringUtils.isNotEmpty(delegationMemberId)){
    		origDelegationMember = getDelegateMemberBo(delegationMemberId);
    	} else{
    		List<DelegateMemberBo> origDelegationMembers =
                    this.getDelegationMemberBoListByMemberAndDelegationId(memberId, delegation.getDelegationId());
	    	origDelegationMember =
	    		(origDelegationMembers!=null && origDelegationMembers.size()>0) ? origDelegationMembers.get(0) : null;
    	}
    	if(origDelegationMember!=null){
    		newDelegationMember.setDelegationMemberId(origDelegationMember.getDelegationMemberId());
    		newDelegationMember.setVersionNumber(origDelegationMember.getVersionNumber());
    	} else{
    		newDelegationMember.setDelegationMemberId(getNewDelegationMemberId());
    	}
    	newDelegationMember.setMemberId(memberId);
    	newDelegationMember.setDelegationId(delegation.getDelegationId());
    	newDelegationMember.setRoleMemberId(roleMemberId);
    	newDelegationMember.setTypeCode(memberTypeCode);
		if (activeFromDate != null) {
			newDelegationMember.setActiveFromDateValue(new java.sql.Timestamp(activeFromDate.getTime()));
		}
		if (activeToDate != null) {
			newDelegationMember.setActiveToDateValue(new java.sql.Timestamp(activeToDate.getTime()));
		}

    	// build role member attribute objects from the given Map<String, String>
    	addDelegationMemberAttributeData( newDelegationMember, qualifications, role.getKimTypeId() );

    	List<DelegateMemberBo> delegationMembers = new ArrayList<DelegateMemberBo>();
    	delegationMembers.add(newDelegationMember);
    	delegation.setMembers(delegationMembers);

    	getBusinessObjectService().save(delegation);
    	for(DelegateMemberBo delegationMember: delegation.getMembers()){
    		deleteNullDelegationMemberAttributeData(delegationMember.getAttributes());
    	}
    }

    private void removeRoleMembers(List<RoleMemberBo> members) {
        if(CollectionUtils.isNotEmpty(members)) {
            for ( RoleMemberBo rm : members ) {
                getResponsibilityInternalService().removeRoleMember(rm);
            }
        }
    }
    
    private List<RoleMemberBo> getRoleMembersByDefaultStrategy(RoleBo role, String memberId, String memberTypeCode, Map<String, String> qualifier) {
        List<RoleMemberBo> rms = new ArrayList<RoleMemberBo>();
        role.refreshReferenceObject("members");
        for ( RoleMemberBo rm : role.getMembers() ) {
            if ( doesMemberMatch( rm, memberId, memberTypeCode, qualifier ) ) {
                // if found, remove
                rms.add(rm);
            }
        }
        return rms;
    }
    
    @Override
    public void removePrincipalFromRole(@WebParam(name = "principalId") String principalId, @WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName, @WebParam(name = "qualifier") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualifier) throws UnsupportedOperationException {
        // look up the role
    	RoleBo role = getRoleBoByName(namespaceCode, roleName);
    	// pull all the principal members
    	// look for an exact qualifier match
        List<RoleMemberBo> rms = getRoleMembersByExactQualifierMatch(role, principalId, memberTypeToRoleDaoActionMap.get(Role.PRINCIPAL_MEMBER_TYPE), qualifier);
        if(CollectionUtils.isEmpty(rms)) {
            rms = getRoleMembersByDefaultStrategy(role, principalId, Role.PRINCIPAL_MEMBER_TYPE, qualifier);
        } 
        removeRoleMembers(rms);
    }
    
    @Override
    public void removeGroupFromRole(@WebParam(name = "groupId") String groupId, @WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName, @WebParam(name = "qualifier") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualifier) throws UnsupportedOperationException {
        // look up the roleBo
    	RoleBo roleBo = getRoleBoByName(namespaceCode, roleName);
    	// pull all the group roleBo members
    	// look for an exact qualifier match
        List<RoleMemberBo> rms = getRoleMembersByExactQualifierMatch(roleBo, groupId, memberTypeToRoleDaoActionMap.get(Role.GROUP_MEMBER_TYPE), qualifier);
        if(CollectionUtils.isEmpty(rms)) {
            rms = getRoleMembersByDefaultStrategy(roleBo, groupId, Role.GROUP_MEMBER_TYPE, qualifier);
        } 
        removeRoleMembers(rms);
    }   
    
    @Override
    public void removeRoleFromRole(@WebParam(name = "roleId") String roleId, @WebParam(name = "namespaceCode") String namespaceCode, @WebParam(name = "roleName") String roleName, @WebParam(name = "qualifier") @XmlJavaTypeAdapter(value = MapStringStringAdapter.class) Map<String, String> qualifier) throws UnsupportedOperationException {
        // look up the role
    	RoleBo role = getRoleBoByName(namespaceCode, roleName);
    	// pull all the group role members
    	// look for an exact qualifier match
        List<RoleMemberBo> rms = getRoleMembersByExactQualifierMatch(role, roleId, memberTypeToRoleDaoActionMap.get(Role.ROLE_MEMBER_TYPE), qualifier);
        if(CollectionUtils.isEmpty(rms)) {
            rms = getRoleMembersByDefaultStrategy(role, roleId, Role.ROLE_MEMBER_TYPE, qualifier);
        } 
        removeRoleMembers(rms);
    }    

    @Override
    public void saveRole(@WebParam(name = "roleId") String roleId, @WebParam(name = "roleName") String roleName, @WebParam(name = "roleDescription") String roleDescription, @WebParam(name = "active") boolean active, @WebParam(name = "kimTypeId") String kimTypeId, @WebParam(name = "namespaceCode") String namespaceCode) throws UnsupportedOperationException {
        // look for existing role
        RoleBo role = getBusinessObjectService().findBySinglePrimaryKey(RoleBo.class, roleId);
        if (role == null) {
            role = new RoleBo();
            role.setId(roleId);
        }

        role.setName(roleName);
        role.setDescription(roleDescription);
        role.setActive(active);
        role.setKimTypeId(kimTypeId);
        role.setNamespaceCode(namespaceCode);

        getBusinessObjectService().save(role);
    }

    @Override
    public String getNextAvailableRoleId() throws UnsupportedOperationException {
        Long nextSeq = KRADServiceLocator.getSequenceAccessorService().getNextAvailableSequenceNumber(KimConstants.SequenceNames.KRIM_ROLE_MBR_ID_S, RoleBo.class);

        if (nextSeq == null) {
            LOG.error("Unable to get new role id from sequence " + KimConstants.SequenceNames.KRIM_ROLE_MBR_ID_S);
            throw new RuntimeException("Unable to get new role id from sequence " + KimConstants.SequenceNames.KRIM_ROLE_MBR_ID_S);
        }

        return nextSeq.toString();
    }

    @Override
    public void assignPermissionToRole(String permissionId, String roleId) throws UnsupportedOperationException {
        RolePermissionBo newRolePermission = new RolePermissionBo();

        Long nextSeq = KRADServiceLocator.getSequenceAccessorService().getNextAvailableSequenceNumber(KimConstants.SequenceNames.KRIM_ROLE_PERM_ID_S, RolePermissionBo.class);

        if (nextSeq == null) {
            LOG.error("Unable to get new role permission id from sequence " + KimConstants.SequenceNames.KRIM_ROLE_PERM_ID_S);
            throw new RuntimeException("Unable to get new role permission id from sequence " + KimConstants.SequenceNames.KRIM_ROLE_PERM_ID_S);
        }

        newRolePermission.setId(nextSeq.toString());
        newRolePermission.setRoleId(roleId);
        newRolePermission.setPermissionId(permissionId);
        newRolePermission.setActive(true);

        getBusinessObjectService().save(newRolePermission);
    }

    protected void addMemberAttributeData(RoleMemberBo roleMember, Map<String, String> qualifier, String kimTypeId) {
        List<RoleMemberAttributeDataBo> attributes = new ArrayList<RoleMemberAttributeDataBo>();
        for (String attributeName : qualifier.keySet()) {
            RoleMemberAttributeDataBo roleMemberAttrBo = new RoleMemberAttributeDataBo();
            roleMemberAttrBo.setAttributeValue(qualifier.get(attributeName));
            roleMemberAttrBo.setKimTypeId(kimTypeId);
            roleMemberAttrBo.setAssignedToId(roleMember.getRoleMemberId());
            // look up the attribute ID
            roleMemberAttrBo.setKimAttributeId(getKimAttributeId(attributeName));

            Map<String, String> criteria = new HashMap<String, String>();
            criteria.put(KimConstants.PrimaryKeyConstants.KIM_ATTRIBUTE_ID, roleMemberAttrBo.getKimAttributeId());
            criteria.put(KimConstants.PrimaryKeyConstants.ROLE_MEMBER_ID, roleMember.getRoleMemberId());
            List<RoleMemberAttributeDataBo> origRoleMemberAttributes =
                    (List<RoleMemberAttributeDataBo>) getBusinessObjectService().findMatching(RoleMemberAttributeDataBo.class, criteria);
            RoleMemberAttributeDataBo origRoleMemberAttribute =
                    (origRoleMemberAttributes != null && origRoleMemberAttributes.size() > 0) ? origRoleMemberAttributes.get(0) : null;
            if (origRoleMemberAttribute != null) {
                roleMemberAttrBo.setId(origRoleMemberAttribute.getId());
                roleMemberAttrBo.setVersionNumber(origRoleMemberAttribute.getVersionNumber());
            } else {
                // pull the next sequence number for the data ID
                roleMemberAttrBo.setId(getNewAttributeDataId());
            }
            attributes.add(roleMemberAttrBo);
        }
        roleMember.setAttributeDetails(attributes);
    }

    protected void addDelegationMemberAttributeData( DelegateMemberBo delegationMember, Map<String, String> qualifier, String kimTypeId ) {
		List<DelegateMemberAttributeDataBo> attributes = new ArrayList<DelegateMemberAttributeDataBo>();
		for ( String attributeName : qualifier.keySet() ) {
			DelegateMemberAttributeDataBo delegateMemberAttrBo = new DelegateMemberAttributeDataBo();
			delegateMemberAttrBo.setAttributeValue(qualifier.get(attributeName));
			delegateMemberAttrBo.setKimTypeId(kimTypeId);
			delegateMemberAttrBo.setAssignedToId(delegationMember.getDelegationMemberId());
			// look up the attribute ID
			delegateMemberAttrBo.setKimAttributeId(getKimAttributeId(attributeName));
	    	Map<String, String> criteria = new HashMap<String, String>();
	    	criteria.put(KimConstants.PrimaryKeyConstants.KIM_ATTRIBUTE_ID, delegateMemberAttrBo.getKimAttributeId());
	    	criteria.put(KimConstants.PrimaryKeyConstants.DELEGATION_MEMBER_ID, delegationMember.getDelegationMemberId());
			List<DelegateMemberAttributeDataBo> origDelegationMemberAttributes =
	    		(List<DelegateMemberAttributeDataBo>)getBusinessObjectService().findMatching(DelegateMemberAttributeDataBo.class, criteria);
			DelegateMemberAttributeDataBo origDelegationMemberAttribute =
	    		(origDelegationMemberAttributes!=null && origDelegationMemberAttributes.size()>0) ? origDelegationMemberAttributes.get(0) : null;
	    	if(origDelegationMemberAttribute!=null){
	    		delegateMemberAttrBo.setId(origDelegationMemberAttribute.getId());
	    		delegateMemberAttrBo.setVersionNumber(origDelegationMemberAttribute.getVersionNumber());
	    	} else{
				// pull the next sequence number for the data ID
				delegateMemberAttrBo.setId(getNewAttributeDataId());
	    	}
			attributes.add( delegateMemberAttrBo );
		}
		delegationMember.setAttributes( attributes );
	}



     // --------------------
    // Persistence Methods
    // --------------------

	private void deleteNullMemberAttributeData(List<RoleMemberAttributeDataBo> attributes) {
		List<RoleMemberAttributeDataBo> attributesToDelete = new ArrayList<RoleMemberAttributeDataBo>();
		for(RoleMemberAttributeDataBo attribute: attributes){
			if(attribute.getAttributeValue()==null){
				attributesToDelete.add(attribute);
			}
		}
		getBusinessObjectService().delete(attributesToDelete);
	}

    private void deleteNullDelegationMemberAttributeData(List<DelegateMemberAttributeDataBo> attributes) {
        List<DelegateMemberAttributeDataBo> attributesToDelete = new ArrayList<DelegateMemberAttributeDataBo>();

		for(DelegateMemberAttributeDataBo attribute: attributes){
			if(attribute.getAttributeValue()==null){
				attributesToDelete.add(attribute);
			}
		}
		getBusinessObjectService().delete(attributesToDelete);
	}

}
