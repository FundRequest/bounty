package io.fundrequest.profile.profile;

import io.fundrequest.profile.profile.dto.UserProfile;
import io.fundrequest.profile.profile.dto.UserProfileProvider;
import io.fundrequest.profile.profile.provider.Provider;
import io.fundrequest.profile.ref.RefSignupEvent;
import io.fundrequest.profile.telegram.domain.TelegramVerification;
import io.fundrequest.profile.telegram.service.TelegramVerificationService;
import io.fundrequest.profile.twitter.model.TwitterBounty;
import io.fundrequest.profile.twitter.model.TwitterPost;
import io.fundrequest.profile.twitter.service.TwitterBountyService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class ProfileController {

    private ApplicationEventPublisher eventPublisher;
    private ProfileService profileService;
    private TwitterBountyService twitterBountyService;
    private TelegramVerificationService telegramVerificationService;


    public ProfileController(final ApplicationEventPublisher eventPublisher,
                             final ProfileService profileService,
                             final TwitterBountyService twitterBountyService,
                             final TelegramVerificationService telegramVerificationService) {
        this.eventPublisher = eventPublisher;
        this.profileService = profileService;
        this.twitterBountyService = twitterBountyService;
        this.telegramVerificationService = telegramVerificationService;
    }

    @GetMapping("/profile")
    public ModelAndView showProfile(Principal principal, @RequestParam(value = "ref", required = false) String ref, HttpServletRequest request, Model model) {
        if (StringUtils.isNotBlank(ref)) {
            eventPublisher.publishEvent(RefSignupEvent.builder().principal(principal).ref(ref).build());
            return redirectToProfile();
        }
        final ModelAndView mav = new ModelAndView("profile");
        final UserProfile userProfile = (UserProfile) model.asMap().get("profile");
        enrichTwitter(mav, userProfile);
        enrichTelegram(mav, principal);

        mav.addObject("refLink", getRefLink(request, principal));

        return mav;
    }

    private void enrichTelegram(final ModelAndView mav, final Principal principal) {
        final Optional<TelegramVerification> telegramVerification = telegramVerificationService.getByUserId(principal);
        if (telegramVerification.isPresent() && telegramVerification.get().isVerified()) {
            mav.addObject("telegramVerified", true);
            mav.addObject("telegramVerification", telegramVerification.get());
        } else {
            mav.addObject("telegramVerified", false);
            mav.addObject("telegramVerificationCommand", getTelegramVerificationCommand(principal));
        }
    }

    private String getTelegramVerificationCommand(final Principal principal) {
        return "/verify " + telegramVerificationService.createSecret(principal.getName());
    }

    @PostMapping("/profile/etheraddress")
    public ModelAndView updateAddress(Principal principal, @RequestParam("etheraddress") String etherAddress) {
        profileService.updateEtherAddress(principal, etherAddress);
        return redirectToProfile();
    }

    @PostMapping("/profile/telegramname")
    public ModelAndView updateTelegram(Principal principal, @RequestParam("telegramname") String telegramname) {
        if (telegramname.contains("@")) {
            throw new IllegalArgumentException("Only A-Z is allowed");
        }
        telegramVerificationService.createTelegramVerification(principal.getName(), telegramname);
        profileService.updateTelegramName(principal, telegramname);
        return redirectToProfile();
    }

    private static String getRefLink(HttpServletRequest req, Principal principal) {
        return "https://fundrequest.io?ref=" + principal.getName();
    }

    private void enrichTwitter(ModelAndView mav, UserProfile userProfile) {
        if (userProfile.getTwitter() != null) {
            final Optional<TwitterBounty> activeBounty = twitterBountyService.getActiveBounty();
            if (activeBounty.isPresent()) {
                mav.addObject("activeBounty", activeBounty);
                if (claimedTwitterBounty(userProfile.getTwitter(), activeBounty.get())) {
                    mav.addObject("claimedTwitterBounty", true);
                } else {
                    mav.addObject("claimedTwitterBounty", false);
                    final List<TwitterPost> posts = new ArrayList<>(twitterBountyService.getTwitterPosts());
                    Collections.shuffle(twitterBountyService.getTwitterPosts());
                    mav.addObject("twitterPost", posts.get(0));
                }
            }
        }
    }

    private boolean claimedTwitterBounty(final UserProfileProvider twitter, final TwitterBounty twitterBounty) {
        return twitterBountyService.claimedBountyAlready(twitter.getUserId(), twitterBounty);
    }

    @GetMapping("/profile/link/{provider}/redirect")
    public ModelAndView redirectToHereAfterProfileLink(Principal principal, @PathVariable("provider") String provider) {
        profileService.userProviderIdentityLinked(principal, Provider.fromString(provider));
        return redirectToProfile();
    }

    private ModelAndView redirectToProfile() {
        return new ModelAndView(new RedirectView("/profile"));
    }

    @GetMapping(path = "/logout")
    public String logout(HttpServletRequest request) throws ServletException {
        request.logout();
        return "redirect:https://fundrequest.io";
    }
}
