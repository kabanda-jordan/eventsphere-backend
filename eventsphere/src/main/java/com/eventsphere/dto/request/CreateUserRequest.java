package com.eventsphere.dto.request;

import com.eventsphere.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "username: must not be blank")
    @Size(min = 3, max = 50, message = "username: must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "email: must not be blank")
    @Email(message = "email: must be a valid email address")
    private String email;

    @NotBlank(message = "fullName: must not be blank")
    private String fullName;

    @NotBlank(message = "password: must not be blank")
    @Size(min = 8, message = "password: must be at least 8 characters")
    private String password;

    @NotNull(message = "role: must not be null (ADMIN or STUDENT)")
    private Role role;

    // Optional — only relevant when role = STUDENT
    private String studentId;
    private String department;
    private Integer yearOfStudy;
    private String phone;

    public CreateUserRequest() {}

    public String getUsername()              { return username; }
    public void setUsername(String v)        { this.username = v; }
    public String getEmail()                 { return email; }
    public void setEmail(String v)           { this.email = v; }
    public String getFullName()              { return fullName; }
    public void setFullName(String v)        { this.fullName = v; }
    public String getPassword()              { return password; }
    public void setPassword(String v)        { this.password = v; }
    public Role getRole()                    { return role; }
    public void setRole(Role v)              { this.role = v; }
    public String getStudentId()             { return studentId; }
    public void setStudentId(String v)       { this.studentId = v; }
    public String getDepartment()            { return department; }
    public void setDepartment(String v)      { this.department = v; }
    public Integer getYearOfStudy()          { return yearOfStudy; }
    public void setYearOfStudy(Integer v)    { this.yearOfStudy = v; }
    public String getPhone()                 { return phone; }
    public void setPhone(String v)           { this.phone = v; }
}
