package io.fundrequest.profile.ref;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RefSignupListener {

    private ReferralService referralService;

    public RefSignupListener(ReferralService referralService) {
        this.referralService = referralService;
    }

    @Async
    @EventListener
    public void onSignup(RefSignupEvent refSignupEvent) {
        referralService.createNewRef(
                CreateRefCommand.builder().principal(refSignupEvent.getPrincipal()).ref(refSignupEvent.getRef()).build()
        );
    }


}
