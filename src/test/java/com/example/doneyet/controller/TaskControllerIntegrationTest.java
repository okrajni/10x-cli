package com.example.doneyet.controller;

import com.example.doneyet.TestConfig;
import com.example.doneyet.domain.*;
import com.example.doneyet.repository.*;
import com.example.doneyet.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
class TaskControllerIntegrationTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private HouseholdMemberRepository householdMemberRepository;

    @Autowired
    private TaskRepository taskRepository;

    private MockMvc mockMvc;
    private String authToken;
    private UUID householdId;
    private UUID user1Id;
    private UUID member1Id;
    private UUID member2Id;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        taskRepository.deleteAll();
        householdMemberRepository.deleteAll();
        householdRepository.deleteAll();
        userRepository.deleteAll();

        User user1 = new User("user1@test.com", "hashedPassword");
        User user2 = new User("user2@test.com", "hashedPassword");
        userRepository.save(user1);
        userRepository.save(user2);
        user1Id = user1.getId();

        Household household = new Household("Test Household", user1);
        householdRepository.save(household);
        householdId = household.getId();

        HouseholdMember member1 = new HouseholdMember(household, user1, HouseholdMemberRole.CREATOR);
        HouseholdMember member2 = new HouseholdMember(household, user2, HouseholdMemberRole.PARTNER);
        householdMemberRepository.save(member1);
        householdMemberRepository.save(member2);
        member1Id = member1.getId();
        member2Id = member2.getId();

        authToken = jwtTokenProvider.generateToken(user1Id, "user1@test.com");
    }

    @Test
    void shouldCreateTaskWithExplicitAssigneeId() throws Exception {
        String requestBody = "{" +
                "\"title\":\"Task with Assignment\"," +
                "\"description\":\"Assigned to partner\"," +
                "\"category\":\"CLEANING\"," +
                "\"dueDate\":\"" + LocalDate.now().plusDays(1) + "\"," +
                "\"assigneeId\":\"" + member2Id + "\"" +
                "}";

        mockMvc.perform(post("/api/task")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Task with Assignment"))
                .andExpect(jsonPath("$.assignee").isNotEmpty())
                .andExpect(jsonPath("$.assignee.id").value(member2Id.toString()))
                .andExpect(jsonPath("$.assignee.email").value("user2@test.com"));
    }

    @Test
    void shouldCreateTaskWithoutAssigneeIdDefaultsToCreator() throws Exception {
        String requestBody = "{" +
                "\"title\":\"Default Assignment Task\"," +
                "\"category\":\"SHOPPING\"," +
                "\"dueDate\":\"" + LocalDate.now().plusDays(2) + "\"" +
                "}";

        mockMvc.perform(post("/api/task")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Default Assignment Task"))
                .andExpect(jsonPath("$.assignee").isNotEmpty())
                .andExpect(jsonPath("$.assignee.id").value(member1Id.toString()))
                .andExpect(jsonPath("$.assignee.email").value("user1@test.com"));
    }

    @Test
    void shouldReturnValidationErrorForInvalidAssigneeId() throws Exception {
        String requestBody = "{" +
                "\"title\":\"Invalid Assignee Task\"," +
                "\"category\":\"LAUNDRY\"," +
                "\"dueDate\":\"" + LocalDate.now().plusDays(3) + "\"," +
                "\"assigneeId\":\"" + UUID.randomUUID() + "\"" +
                "}";

        mockMvc.perform(post("/api/task")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReassignTask() throws Exception {
        Task task = new Task("Task to Reassign", householdRepository.findById(householdId).orElseThrow(),
                householdMemberRepository.findById(member1Id).orElseThrow(),
                userRepository.findById(user1Id).orElseThrow());
        taskRepository.save(task);
        UUID taskId = task.getId();

        String requestBody = "{" +
                "\"assigneeId\":\"" + member2Id + "\"" +
                "}";

        mockMvc.perform(put("/api/task/" + taskId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").isNotEmpty())
                .andExpect(jsonPath("$.assignee.id").value(member2Id.toString()))
                .andExpect(jsonPath("$.assignee.email").value("user2@test.com"));
    }

    @Test
    void shouldListTasksWithAssigneeData() throws Exception {
        Task task1 = new Task("Task 1", householdRepository.findById(householdId).orElseThrow(),
                householdMemberRepository.findById(member1Id).orElseThrow(),
                userRepository.findById(user1Id).orElseThrow());
        Task task2 = new Task("Task 2", householdRepository.findById(householdId).orElseThrow(),
                householdMemberRepository.findById(member2Id).orElseThrow(),
                userRepository.findById(user1Id).orElseThrow());
        taskRepository.save(task1);
        taskRepository.save(task2);

        mockMvc.perform(get("/api/task")
                .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].assignee").isNotEmpty())
                .andExpect(jsonPath("$[1].assignee").isNotEmpty())
                .andExpect(jsonPath("$[0].assignee.email").value("user1@test.com"))
                .andExpect(jsonPath("$[1].assignee.email").value("user2@test.com"));
    }
}
