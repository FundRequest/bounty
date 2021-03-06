package io.fundrequest.profile.profile;

import io.fundrequest.profile.linkedin.LinkedInService;
import io.fundrequest.profile.profile.dto.UserProfile;
import io.fundrequest.profile.profile.dto.UserProfileProvider;
import io.fundrequest.profile.profile.provider.Provider;
import io.fundrequest.profile.ref.RefSignupEvent;
import io.fundrequest.profile.ref.ReferralService;
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
import java.net.URLEncoder;
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
    private LinkedInService linkedInService;
    private ReferralService referralService;


    public ProfileController(final ApplicationEventPublisher eventPublisher,
                             final ProfileService profileService,
                             final TwitterBountyService twitterBountyService,
                             final TelegramVerificationService telegramVerificationService,
                             final LinkedInService linkedInService, ReferralService referralService) {
        this.eventPublisher = eventPublisher;
        this.profileService = profileService;
        this.twitterBountyService = twitterBountyService;
        this.telegramVerificationService = telegramVerificationService;
        this.linkedInService = linkedInService;
        this.referralService = referralService;
    }

    @GetMapping("/profile")
    public ModelAndView showProfile(Principal principal, @RequestParam(value = "ref", required = false) String ref, HttpServletRequest request, Model model) throws Exception {
        if (StringUtils.isNotBlank(ref)) {
            eventPublisher.publishEvent(RefSignupEvent.builder().principal(principal).ref(ref).build());
            return redirectToProfile();
        }
        final ModelAndView mav = new ModelAndView("profile");
        final UserProfile userProfile = (UserProfile) model.asMap().get("profile");
        enrichTwitter(mav, userProfile);
        enrichTelegram(mav, principal);

        mav.addObject("refLink", getRefLink(principal, "web"));
        mav.addObject("refLinkTwitter", URLEncoder.encode(getRefLink(principal, "twitter"), "UTF-8"));
        mav.addObject("refLinkLinkedin", URLEncoder.encode(getRefLink(principal, "linkedin"), "UTF-8"));
        mav.addObject("refLinkFacebook", URLEncoder.encode(getRefLink(principal, "facebook"), "UTF-8"));
        mav.addObject("linkedInVerification", linkedInService.getVerification(principal));
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

    @PostMapping("/profile/headline")
    public ModelAndView updateHeadline(Principal principal, @RequestParam("headline") String headline) {
        profileService.updateHeadline(principal, headline);
        return redirectToProfile();
    }

    @PostMapping("/profile/telegramname")
    public ModelAndView updateTelegram(Principal principal, @RequestParam("telegramname") String telegramname) {
        telegramname = telegramname.startsWith("@") ? telegramname.replaceFirst("@", "") : telegramname;
        if (!telegramname.matches("^[a-zA-Z][a-zA-Z0-9_]*[a-zA-Z0-9]$")) {
            throw new IllegalArgumentException("Not a valid telegram handle, you can use a-z, 0-9 and underscores.");
        }
        telegramVerificationService.createTelegramVerification(principal.getName(), telegramname);
        profileService.updateTelegramName(principal, telegramname);
        return redirectToProfile();
    }

    private String getRefLink(final Principal principal, String source) {
        return referralService.generateRefLink(principal.getName(), source);
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
                    Collections.shuffle(posts);
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
