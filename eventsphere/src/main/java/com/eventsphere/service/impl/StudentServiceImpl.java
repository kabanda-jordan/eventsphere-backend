package com.eventsphere.service.impl;

import com.eventsphere.dto.response.ApiResponse;
import com.eventsphere.entity.Student;
import com.eventsphere.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.eventsphere.repository.StudentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class StudentServiceImpl {

    private final StudentRepository studentRepository;

    public StudentServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudents(String search, Pageable pageable) {
        Page<Student> page = (search == null || search.isBlank())
                ? studentRepository.findAll(pageable)
                : studentRepository.search(search, pageable);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", page.getContent().stream().map(this::toStudentData).toList());
        result.put("page", page.getNumber());
        result.put("size", page.getSize());
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("last", page.isLast());
        return result;
    }

    @Transactional(readOnly = true)
    public ApiResponse.StudentData getStudent(Long id) {
        return toStudentData(studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found")));
    }

    @Transactional
    public ApiResponse.StudentData updateStudent(Long id, UpdateStudentCommand command) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        student.setDepartment(command.getDepartment());
        student.setYearOfStudy(command.getYearOfStudy());
        student.setPhone(command.getPhone());
        return toStudentData(studentRepository.save(student));
    }

    private ApiResponse.StudentData toStudentData(Student student) {
        ApiResponse.StudentData data = new ApiResponse.StudentData();
        data.setId(student.getId());
        data.setUserId(student.getUser().getId());
        data.setUsername(student.getUser().getUsername());
        data.setEmail(student.getUser().getEmail());
        data.setFullName(student.getUser().getFullName());
        data.setStudentId(student.getStudentId());
        data.setDepartment(student.getDepartment());
        data.setYearOfStudy(student.getYearOfStudy());
        data.setPhone(student.getPhone());
        data.setCreatedAt(student.getCreatedAt());
        data.setUpdatedAt(student.getUpdatedAt());
        return data;
    }

    public static class UpdateStudentCommand {
        private String department;
        private Integer yearOfStudy;
        private String phone;

        public UpdateStudentCommand() {
        }

        public UpdateStudentCommand(String department, Integer yearOfStudy, String phone) {
            this.department = department;
            this.yearOfStudy = yearOfStudy;
            this.phone = phone;
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
