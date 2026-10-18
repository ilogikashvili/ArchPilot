package com.ilia.gikashvili.archmain.invitation.service;
import com.ilia.gikashvili.archmain.invitation.entity.Invitation;
import com.ilia.gikashvili.archmain.invitation.repository.InvitationRepository;
import com.ilia.gikashvili.archmain.access.service.ProjectAccessService;
import com.ilia.gikashvili.archmain.access.policy.*;
import com.ilia.gikashvili.archmain.security.AuthenticatedUser;
import com.ilia.gikashvili.archmain.common.exception.*;
import com.ilia.gikashvili.archmain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional
public class InvitationService {
 private final InvitationRepository repo;private final ProjectAccessService access;private final UserRepository users;
 public InvitationService(InvitationRepository r,ProjectAccessService a,UserRepository u){repo=r;access=a;users=u;}
 public Object mine(AuthenticatedUser u){return repo.findByEmailIgnoreCaseAndStatus(u.email(),Invitation.Status.PENDING);}
 public Object create(UUID pid,AuthenticatedUser u,String email,ProjectRole role){access.require(pid,u.id(),Permission.MANAGE_MEMBERS);email=email.trim().toLowerCase(Locale.ROOT);if(repo.existsByProjectIdAndEmailAndStatus(pid,email,Invitation.Status.PENDING))throw new ConflictException("Pending invitation already exists");var existing=users.findByEmail(email);if(existing.isPresent()&&access.hasPermission(pid,existing.get().getId(),Permission.READ))throw new ConflictException("User is already a member");Invitation i=new Invitation();i.projectId=pid;i.inviterId=u.id();i.email=email;i.role=role;return repo.saveAndFlush(i);}
 public Object list(UUID pid,UUID u){access.require(pid,u,Permission.MANAGE_MEMBERS);return repo.findAllByProjectId(pid);}
 public Object change(UUID id,AuthenticatedUser u,Invitation.Status status){Invitation i=repo.findLockedById(id).orElseThrow(()->new NotFoundException("Invitation",id));if(status==Invitation.Status.CANCELLED){if(!i.inviterId.equals(u.id()))access.require(i.projectId,u.id(),Permission.MANAGE_MEMBERS);}else if(!i.email.equalsIgnoreCase(u.email()))throw new ForbiddenException("Invitation belongs to another email");if(i.status!=Invitation.Status.PENDING)throw new ConflictException("Invitation is no longer pending");if(status==Invitation.Status.ACCEPTED){if(access.hasPermission(i.projectId,u.id(),Permission.READ))throw new ConflictException("User is already a member");access.addMember(i.projectId,u.id(),i.role);}i.status=status;return repo.saveAndFlush(i);}
}
