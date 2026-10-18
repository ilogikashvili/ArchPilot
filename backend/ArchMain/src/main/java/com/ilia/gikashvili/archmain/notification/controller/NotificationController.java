package com.ilia.gikashvili.archmain.notification.controller;
import java.util.*; import org.springframework.web.bind.annotation.*; import org.springframework.data.domain.*; import com.ilia.gikashvili.archmain.security.*; import com.ilia.gikashvili.archmain.notification.repository.*; import com.ilia.gikashvili.archmain.notification.service.*; import com.ilia.gikashvili.archmain.notification.entity.*;
@RestController @RequestMapping(com.ilia.gikashvili.archmain.common.api.ApiVersion.V1+"/notifications") public class NotificationController {private final NotificationRepository repo; private final NotificationService service;public NotificationController(NotificationRepository r,NotificationService s){repo=r;service=s;}
@GetMapping public Page<Notification> list(@CurrentUser AuthenticatedUser u,@RequestParam(defaultValue="false") boolean unreadOnly,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){var p=PageRequest.of(page,Math.min(size,100),Sort.by("createdAt").descending());return unreadOnly?repo.findByUserIdAndReadFalse(u.id(),p):repo.findByUserId(u.id(),p);}
@GetMapping("/unread-count") public Map<String,Long> count(@CurrentUser AuthenticatedUser u){return Map.of("count",repo.countByUserIdAndReadFalse(u.id()));}
public record ReadRequest(@jakarta.validation.constraints.NotNull Boolean read){}
@PatchMapping("/{id}") public Notification mark(@CurrentUser AuthenticatedUser u,@PathVariable UUID id,@jakarta.validation.Valid @RequestBody ReadRequest b){return service.mark(u.id(),id,b.read());}
@PostMapping("/mark-all-read") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void all(@CurrentUser AuthenticatedUser u){service.all(u.id());}
@DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void delete(@CurrentUser AuthenticatedUser u,@PathVariable UUID id){repo.delete(service.owned(u.id(),id));}
}
