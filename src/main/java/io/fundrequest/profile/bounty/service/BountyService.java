package io.fundrequest.profile.bounty.service;

import io.fundrequest.profile.bounty.dto.BountyDTO;
import io.fundrequest.profile.bounty.dto.PaidBountyDto;
import io.fundrequest.profile.bounty.event.CreateBountyCommand;

import java.security.Principal;
import java.util.List;

public interface BountyService {
    void createBounty(CreateBountyCommand createBountyCommand);

    BountyDTO getBounties(Principal principal);

    List<PaidBountyDto> getPaidBounties(Principal principal);
}
