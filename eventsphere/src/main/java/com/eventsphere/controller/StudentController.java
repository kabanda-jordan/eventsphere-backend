package com.eventsphere.controller;

import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.service.impl.StudentServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Students", description = "Student management endpoints — ADMIN only")
@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentServiceImpl studentService;

    public StudentController(StudentServiceImpl studentService) {
        this.studentService = studentService;
    }

    @Operation(summary = "List all students", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Returns a paginated list of students with optional search. Requires ADMIN role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Students fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse.Success<Map<String, Object>>> getStudents(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Search by name, username, or student ID") @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.Success.of("Students fetched successfully",
                studentService.getStudents(search, PageRequest.of(page, size))));
    }

    @Operation(summary = "Get student by ID", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Returns a single student by their internal ID. Requires ADMIN role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student fetched successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse.Success<ApiResponse.StudentData>> getStudent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.Success.of("Student fetched successfully", studentService.getStudent(id)));
    }

    @Operation(summary = "Update student profile", security = @SecurityRequirement(name = "bearerAuth"),
               description = "Updates a student's department, year of study, and phone number. Requires ADMIN role.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — ADMIN role required",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Student not found",
            content = @Content(schema = @Schema(implementation = ApiResponse.Error.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse.Success<ApiResponse.StudentData>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request) {
        StudentServiceImpl.UpdateStudentCommand command =
                new StudentServiceImpl.UpdateStudentCommand(request.getDepartment(), request.getYearOfStudy(), request.getPhone());
        return ResponseEntity.ok(ApiResponse.Success.of("Student updated successfully",
                studentService.updateStudent(id, command)));
    }

    @Schema(description = "Request body for updating a student profile")
    public static class UpdateStudentRequest {
        @Schema(description = "Department name", example = "Computer Science")
        private String department;
        @Schema(description = "Year of study (1-6)", example = "2")
        private Integer yearOfStudy;
        @Schema(description = "Phone number", example = "+1234567890")
        private String phone;

        public UpdateStudentRequest() {
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public Integer getYearOfStudy() {
            return yearOfStudy;
        }

        public void setYearOfStudy(Integer yearOfStudy) {
            this.yearOfStudy = yearOfStudy;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}
