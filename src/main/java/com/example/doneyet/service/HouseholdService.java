package com.example.doneyet.service;

import com.example.doneyet.domain.*;
import com.example.doneyet.dto.HouseholdDto;
import com.example.doneyet.exception.ValidationException;
import com.example.doneyet.repository.HouseholdRepository;
import com.example.doneyet.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class HouseholdService {
    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;

    public HouseholdService(
            HouseholdRepository householdRepository,
            UserRepository userRepository
    ) {
        this.householdRepository = householdRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public HouseholdDto.HouseholdResponse createHousehold(UUID userId, String name) {
        validateHouseholdName(name);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        if (householdRepository.existsByCreatedById(userId)) {
            throw new ValidationException("User already has a household. Use the existing household to create tasks.");
        }

        Household household = new Household(name, user);
        Household savedHousehold = householdRepository.save(household);

        return new HouseholdDto.HouseholdResponse(
                savedHousehold.getId(),
                savedHousehold.getName(),
                savedHousehold.getCreatedBy().getId(),
                savedHousehold.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<HouseholdDto.HouseholdResponse> getUserHouseholds(UUID userId) {
        List<Household> households = householdRepository.findByCreatedById(userId);

        List<HouseholdDto.HouseholdResponse> responses = new ArrayList<>();
        for (Household household : households) {
            responses.add(new HouseholdDto.HouseholdResponse(
                    household.getId(),
                    household.getName(),
                    household.getCreatedBy().getId(),
                    household.getCreatedAt()
            ));
        }

        responses.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return responses;
    }

    @Transactional(readOnly = true)
    public HouseholdDto.HouseholdDetailsResponse getHouseholdDetails(UUID householdId, UUID userId) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ValidationException("Household not found"));

        if (!household.getCreatedBy().getId().equals(userId)) {
            throw new ValidationException("User is not the owner of this household");
        }

        return new HouseholdDto.HouseholdDetailsResponse(
                household.getId(),
                household.getName(),
                household.getCreatedBy().getId(),
                household.getCreatedAt()
        );
    }

    private void validateHouseholdName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Household name is required");
        }
    }
}
