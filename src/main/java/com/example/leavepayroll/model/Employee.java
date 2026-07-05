package com.example.leavepayroll.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String department;
    private String designation;

    @Column(name = "basic_salary", nullable = false)
    private Double baseSalary;

    @Column(name = "allowed_leaves", nullable = false)
    private Integer allowedLeaves = 15; // default allowed leaves per year

    @Column(name = "remaining_leaves", nullable = false)
    private Integer remainingLeaves = 15;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaveRequest> leaveRequests;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payslip> payslips;

    // Constructors
    public Employee() {}

    public Employee(String name, String email, String department, String designation, Double baseSalary, Integer allowedLeaves) {
        this.name = name;
        this.email = email;
        this.department = department;
        this.designation = designation;
        this.baseSalary = baseSalary;
        this.allowedLeaves = allowedLeaves;
        this.remainingLeaves = allowedLeaves;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public Double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(Double baseSalary) {
        this.baseSalary = baseSalary;
    }

    public Integer getAllowedLeaves() {
        return allowedLeaves;
    }

    public void setAllowedLeaves(Integer allowedLeaves) {
        this.allowedLeaves = allowedLeaves;
    }

    public Integer getRemainingLeaves() {
        return remainingLeaves;
    }

    public void setRemainingLeaves(Integer remainingLeaves) {
        this.remainingLeaves = remainingLeaves;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
