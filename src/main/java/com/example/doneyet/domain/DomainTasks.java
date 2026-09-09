package com.example.doneyet.domain;

import java.util.Arrays;
import java.util.List;

public class DomainTasks {
    public static final List<DomainTask> DOMAIN_TASKS = Arrays.asList(
            // Cleaning (Weekly / Bi-Weekly)
            new DomainTask("vacuum-living-room", "Vacuum living room", TaskCategory.CLEANING, 7),
            new DomainTask("clean-bathrooms", "Clean bathrooms", TaskCategory.CLEANING, 14),
            new DomainTask("mop-kitchen-floor", "Mop kitchen floor", TaskCategory.CLEANING, 14),
            new DomainTask("change-bed-sheets", "Change bed sheets", TaskCategory.CLEANING, 14),
            new DomainTask("dust-surfaces", "Dust surfaces", TaskCategory.CLEANING, 7),
            new DomainTask("laundry", "Laundry", TaskCategory.LAUNDRY, 2),
            new DomainTask("wash-windows", "Wash windows", TaskCategory.CLEANING, 90),
            new DomainTask("deep-clean-kitchen", "Deep clean kitchen", TaskCategory.CLEANING, 30),
            new DomainTask("deep-clean-bathroom", "Deep clean bathroom", TaskCategory.CLEANING, 30),

            // Maintenance (Monthly / Seasonal / Yearly)
            new DomainTask("check-air-filter", "Check air filter", TaskCategory.MAINTENANCE, 30),
            new DomainTask("change-hvac-filter", "Change HVAC filter", TaskCategory.MAINTENANCE, 90),
            new DomainTask("lawn-mowing", "Lawn mowing", TaskCategory.MAINTENANCE, 14, Season.SPRING),
            new DomainTask("garden-watering", "Garden watering", TaskCategory.MAINTENANCE, 3, Season.SUMMER),
            new DomainTask("gutter-cleaning", "Gutter cleaning", TaskCategory.MAINTENANCE, 365, Season.FALL),
            new DomainTask("ac-maintenance", "AC maintenance", TaskCategory.MAINTENANCE, 365, Season.SPRING),
            new DomainTask("furnace-inspection", "Furnace inspection", TaskCategory.MAINTENANCE, 365, Season.FALL),
            new DomainTask("chimney-sweep", "Chimney sweep", TaskCategory.MAINTENANCE, 365, Season.FALL),
            new DomainTask("pest-control", "Pest control", TaskCategory.MAINTENANCE, 90),
            new DomainTask("water-softener-recharge", "Water softener recharge", TaskCategory.MAINTENANCE, 42),

            // Shopping & Errands
            new DomainTask("grocery-shopping", "Grocery shopping", TaskCategory.SHOPPING, 7),
            new DomainTask("pharmacy-run", "Pharmacy run", TaskCategory.ERRANDS, 30),
            new DomainTask("gas-up-car", "Gas up car", TaskCategory.ERRANDS, 14),
            new DomainTask("car-wash", "Car wash", TaskCategory.ERRANDS, 14),
            new DomainTask("post-office", "Post office", TaskCategory.ERRANDS, 30),

            // Seasonal & Special
            new DomainTask("spring-cleaning", "Spring cleaning", TaskCategory.SEASONAL, 365, Season.SPRING),
            new DomainTask("fall-cleanup", "Fall cleanup", TaskCategory.SEASONAL, 365, Season.FALL),
            new DomainTask("holiday-prep", "Holiday prep", TaskCategory.SEASONAL, 365, Season.WINTER),
            new DomainTask("yard-winterization", "Yard winterization", TaskCategory.SEASONAL, 365, Season.FALL),
            new DomainTask("yard-spring-prep", "Yard spring prep", TaskCategory.SEASONAL, 365, Season.SPRING)
    );

    private DomainTasks() {
    }

    public static DomainTask findById(String id) {
        return DOMAIN_TASKS.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
